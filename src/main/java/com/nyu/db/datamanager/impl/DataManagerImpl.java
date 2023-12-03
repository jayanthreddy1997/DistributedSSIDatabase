package com.nyu.db.datamanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.VariableSnapshot;
import com.nyu.db.model.WriteOperation;
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
        // If uncommitted write exists in same transaction, return that value
        if (this.transactionDataStore.get(op.getTransaction().getTransactionId()).containsKey(op.getVariableId()))
            return Optional.of(this.transactionDataStore.get(op.getTransaction().getTransactionId()).get(op.getVariableId()))

        long transactionStartTime = op.getTransaction().getStartTimestamp();
        List<VariableSnapshot> versions = this.committedSnapshots.get(op.getVariableId());
        int i = versions.size()-1;
        while(i>0 && versions.get(i).getCommitTimestamp()>transactionStartTime) {
            i--;
        }

        if (runConsistencyChecks) {
            // Check 1: Fail if site went down between last commit and beginning of current transaction, unless the
            //          current transaction wrote and the site has this latest uncommitted write
            long lastTransactionCommitTime = versions.get(i).getCommitTimestamp();
            for (long downTime: downTimes) {
                if (downTime>=lastTransactionCommitTime && downTime<=transactionStartTime) {
                    return Optional.empty();
                }
            }

            // Check 2: If a site goes down after the transaction began, don't respond to reads until we see a
            //          write (if the transactions contains writes)


        }

        return Optional.of(versions.get(i).getValue());
    }

    @Override
    public boolean write(WriteOperation op) {
        if (!this.siteUp) {
            return false;
        }
        long transactionId = op.getTransaction().getTransactionId();
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
    public boolean canPreCommitTransaction(long transactionId) {
        return false;
    }

    @Override
    public boolean commitTransaction(long transactionId) {
        return false;
    }

    @Override
    public void fail() {
        // Flush local store
        this.transactionDataStore.clear();
        this.siteUp = false;
    }

    @Override
    public boolean recover(long timestamp) {
        this.bootTimes.add(timestamp);
        this.siteUp = true;
    }

    @Override
    public void dumpVariableValues() {
        List<Integer> variableIds = new ArrayList<>(this.getManagedVariableIds());
        Collections.sort(variableIds);
        for (int variableId: variableIds) {
            List<VariableSnapshot> versions = this.committedSnapshots.get(variableId);
            int lastCommittedValue = versions.get(versions.size()-1).getValue();
            System.out.printf("x%d: %d", variableId, lastCommittedValue);
        }
        System.out.println();
    };
}
