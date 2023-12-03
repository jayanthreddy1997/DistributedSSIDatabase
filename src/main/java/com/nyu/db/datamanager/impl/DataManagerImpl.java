package com.nyu.db.datamanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.*;
import com.nyu.db.utils.TimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataManagerImpl implements DataManager {

    private static final Logger logger = LoggerFactory.getLogger(DataManagerImpl.class);
    private int siteId;
    private boolean siteUp; // TODO: remove if not required

    // Multiple versions of committed values for every variable
    private Map<Integer, List<VariableSnapshot>> committedSnapshots;

    private Map<Long, Map<Integer, Integer>> transactionDataStore; // Uncommitted data for each transaction
    private List<Long> bootTimes; // Time instant at which the site came back up from a down state
    private List<Long> downTimes; // Time instant at which the site went down

    public DataManagerImpl(int siteId){
        this.siteId = siteId;
        this.siteUp = true;
        this.committedSnapshots = new HashMap<>();
        this.bootTimes = new ArrayList<>();
        this.downTimes = new ArrayList<>();
        this.transactionDataStore = new HashMap<>();
    }

    @Override
    public void registerVariable(int variableId, int initValue) {
        List<VariableSnapshot> versions = new ArrayList<>();
        versions.add(new VariableSnapshot(variableId, initValue, 0));
        this.committedSnapshots.put(variableId, versions);
    }

    @Override
    public int getSiteId() {
        return this.siteId;
    }

    @Override
    public Set<Integer> getManagedVariableIds() {
        return this.committedSnapshots.keySet();
    }

    @Override
    public Optional<Integer> read(ReadOperation op) {
        return read(op, true);
    }

    @Override
    public Optional<Integer> read(ReadOperation op, boolean runConsistencyChecks) {
        assert this.bootTimes.size()==this.downTimes.size(); // Otherwise site would be down

        // If uncommitted write exists in same transaction, return that value
        Transaction transaction = op.getTransaction();
        if (this.transactionDataStore.containsKey(transaction.getTransactionId()) &&
                this.transactionDataStore.get(transaction.getTransactionId()).containsKey(op.getVariableId()))
            return Optional.of(this.transactionDataStore.get(transaction.getTransactionId()).get(op.getVariableId()));

        long transactionStartTime = transaction.getStartTimestamp();
        List<VariableSnapshot> versions = this.committedSnapshots.get(op.getVariableId());
        int i = versions.size()-1;
        while(i>0 && versions.get(i).getCommitTimestamp()>transactionStartTime) {
            i--;
        }

        if (runConsistencyChecks) {
            // Check 1: Fail if site went down between last commit and beginning of current transaction, unless the
            //          current transaction wrote and the site has this latest uncommitted write
            long lastTransactionCommitTime = versions.get(i).getCommitTimestamp();
            for (long downTime: this.downTimes) {
                if (downTime>=lastTransactionCommitTime && downTime<=transactionStartTime) {
                    return Optional.empty();
                }
            }

            // Check 2: If a site goes down after the transaction began, don't respond to reads until we see a
            //          write (if the transactions contains writes)

            List<Operation> operations = transaction.getOperations();
            long latestWriteTimestamp = -1;
            for (int j=operations.size()-1; j>=0; j--) {
                Operation operation = operations.get(j);
                if (operation.getOperationType().equals(OperationType.WRITE) &&
                        ((WriteOperation)operation).getVariableId()==op.getVariableId()) {
                    latestWriteTimestamp = operation.getExecutedTimestamp();
                    break;
                }
            }
            if (latestWriteTimestamp!=-1) {
                for (int k=0; k<bootTimes.size(); k++) {
                    if (latestWriteTimestamp>downTimes.get(k) && latestWriteTimestamp<bootTimes.get(k)) {
                        return Optional.empty();
                    }
                }
            }
        }

        return Optional.of(versions.get(i).getValue());
    }

    @Override
    public boolean write(WriteOperation op) {
        assert this.bootTimes.size()==this.downTimes.size(); // Otherwise site would be down

        if (!this.siteUp) {
            return false;
        }
        long transactionId = op.getTransaction().getTransactionId();
        if (!this.transactionDataStore.containsKey(transactionId)) {
            this.transactionDataStore.put(transactionId, new HashMap<>());
        }
        Map<Integer, Integer> localStore = this.transactionDataStore.get(transactionId);
        localStore.put(op.getVariableId(), op.getValue());
        logger.info(String.format("T%d wrote %d to x%d on site %d", transactionId, op.getValue(), op.getVariableId(), this.siteId));
        return true;
    }

    @Override
    public boolean abort(long transactionId) {
        return false;
    }

    @Override
    public boolean precommitTransaction(CommitOperation op) {
        boolean commitStatus = true;

        // First Committer wins rule - Abort if some data item x that T1 has written has also been committed by some
        // other transaction T2 since T1 began
        long transactionStartTime = op.getTransaction().getStartTimestamp();
        Map<Integer, Integer> transactionWorkspace = this.transactionDataStore.get(op.getTransaction().getTransactionId());
        for (int variableId: transactionWorkspace.keySet()) {
            long lastCommittedTimestamp = this.committedSnapshots.get(variableId).get(this.committedSnapshots.get(variableId).size()-1).getCommitTimestamp();
            if (lastCommittedTimestamp > transactionStartTime) {
                commitStatus = false;
                break;
            }
        }
        return commitStatus;
    }

    @Override
    public boolean commitTransaction(CommitOperation op) {
        Map<Integer, Integer> transactionWorkspace = this.transactionDataStore.get(op.getTransaction().getTransactionId());
        transactionWorkspace.forEach((variableId, value) -> this.committedSnapshots.get(variableId).add(new VariableSnapshot(variableId, value, TimeManager.getTime())));
        op.setExecutedTimestamp(TimeManager.getTime());
        this.transactionDataStore.remove(op.getTransaction().getTransactionId());
        return true;
    }

    @Override
    public void fail() {
        // Flush local store
        this.transactionDataStore.clear();
        this.siteUp = false;
        this.downTimes.add(TimeManager.getTime());
    }

    @Override
    public void recover() {
        this.bootTimes.add(TimeManager.getTime());
        this.siteUp = true;
    }

    @Override
    public void printCommittedState() {
        List<Integer> variableIds = new ArrayList<>(this.getManagedVariableIds());
        Collections.sort(variableIds);
        for (int variableId: variableIds) {
            List<VariableSnapshot> versions = this.committedSnapshots.get(variableId);
            int lastCommittedValue = versions.get(versions.size()-1).getValue();
            System.out.printf("x%d: %d, ", variableId, lastCommittedValue);
        }
        System.out.println();
    };
}
