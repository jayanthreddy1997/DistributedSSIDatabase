package com.nyu.db.datamanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.VariableSnapshot;
import com.nyu.db.model.WriteOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataManagerImpl implements DataManager {
    /**
     * Simple implementation of DataManger
     * Does not involve Two phase commit
     * Assumes committed data is persisted in stable storage
     */
    private static final Logger logger = LoggerFactory.getLogger(DataManagerImpl.class);
    private int siteId;
    private boolean siteUp;

    // Multiple versions of committed values for every variable
    private Map<Integer, List<VariableSnapshot>> committedSnapshots;

    private Map<Long, Map<Integer, Integer>> transactionDataStore; // Uncommitted data for each transaction

    public DataManagerImpl(int siteId){
        this.siteId = siteId;
        this.siteUp = true;
        this.committedSnapshots = new HashMap<>();
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
    public Optional<Integer> read(ReadOperation op, boolean runConsistencyChecks) {
        if (runConsistencyChecks) {
            // Check 1: Fail if site went down between last commit and beginning of current transaction
            // Check 2: (Available Copies) Once a site goes down, dont respond to reads until we see a committed write
            // TODO
        } else {
            // TODO: Change Operation to have reference to Transaction instead of just id
            long transactionStartTime = op.getTransaction().getStartTimestamp();
            List<VariableSnapshot> versions = this.committedSnapshots.get(op.getVariableId());
            int i=versions.size()-1;
            while(i>0 && versions.get(i).getCommitTimestamp()>=transactionStartTime) {
                i--;
            }
            return Optional.of(versions.get(i).getValue());
        }
        // TODO: cleanup
    }

    @Override
    public boolean write(WriteOperation op) {
        if (!this.siteUp) {
            throw new RuntimeException("YOYOYOYOYO, somethings wrong");
//            return false;
        }
        Map<Integer, Integer> localStore = this.transactionDataStore.get(op.getTransactionId());
        localStore.put(op.getVariableId(), op.getValue());
        logger.info(String.format("T%d wrote %d to x%d on site %d", op.getTransactionId(), op.getValue(), op.getVariableId(), this.siteId));
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
    public boolean fail() {
        return false;
    }

    @Override
    public boolean recover() {
        return false;
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
