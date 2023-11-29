package com.nyu.db.transactionmanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.*;
import com.nyu.db.transactionmanager.TransactionManager;

import java.util.*;

public class TransactionManagerImpl implements TransactionManager {

    // TODO: implement SSI graph
    private Map<Integer, DataManager> siteToDataManagerMap;
    private Set<Integer> replicatedVariables;
    private Map<Integer, List<DataManager>> variableToDataManagerMap;

    private Map<Integer, Boolean> siteActiveStatus;

    // TODO: Print every time a transaction waits because a site is down
    private Map<Integer, Queue<Operation>> waitingOperations; // Waiting operations on each site

    private void init() {
        this.siteToDataManagerMap = new HashMap<>();
        this.replicatedVariables = new HashSet<>();
        this.variableToDataManagerMap = new HashMap<>();
        this.siteActiveStatus = new HashMap<>();
        this.waitingOperations = new HashMap<>();
    }

    public TransactionManagerImpl() {
        init();
    }

    public TransactionManagerImpl(List<DataManager> dataManagers) {
        init();
        this.configureDataManagers(dataManagers);
    }

    @Override
    public Transaction createTransaction(long transactionId, long startTimestamp) {
        return new Transaction(transactionId, startTimestamp, new ArrayList<>());
    }

    @Override
    public void configureDataManagers(List<DataManager> dataManagers) {
        for (DataManager dm: dataManagers) {
            this.siteToDataManagerMap.put(dm.getSiteId(), dm);
            List<Integer> variablesManaged = dm.getVariableIds();
            for (Integer variableId: variablesManaged) {
                this.variableToDataManagerMap.get(variableId).add(dm);
            }
            this.siteActiveStatus.put(dm.getSiteId(), true);
            this.waitingOperations.put(dm.getSiteId(), new LinkedList<>());
        }
        for (Map.Entry<Integer, List<DataManager>> e: this.variableToDataManagerMap.entrySet()) {
            if (e.getValue().size()>1)
                this.replicatedVariables.add(e.getKey());
        }
    }

    /**
     * Read a value from the database
     * If a transaction must read from a particular site and that site is down, the transaction will wait.
     * @param op Read Operation
     * @return Integer value read from database
     */
    @Override
    public int read(ReadOperation op) {
        List<DataManager> availableDataManagers = this.variableToDataManagerMap.get(op.getVariableId());
        if (availableDataManagers.isEmpty()) {
            throw new RuntimeException("No active node available to serve this read request: "+op);
        } else if (availableDataManagers.size() == 1) {
            // Un-replicated variables
            DataManager dm = availableDataManagers.get(0);
            if (this.siteActiveStatus.get(dm.getSiteId())) {
                return dm.read(op);
            } else {
                // Wait for site to become available
                this.waitingOperations.get(dm.getSiteId()).add(op);
            }
        } else {
            // TODO
        }
        return 0;
    }

    @Override
    public boolean write(WriteOperation op) {
        return false;
    }

    @Override
    public void fail(int siteId) {

    }

    @Override
    public void recover(int siteId) {
        // TODO: check for waiting reads!
    }

    @Override
    public boolean commitTransaction(CommitOperation op) {
        return false;
    }

    public void dumpVariableValues() {
        // Print committed values of all copies of all variables at all sites,
        List<Integer> siteIds = new ArrayList<>(siteToDataManagerMap.keySet());
        Collections.sort(siteIds);
        for (int siteId: siteIds) {
            System.out.printf("site %d - ", siteId);
            this.siteToDataManagerMap.get(siteId).dumpVariableValues();
        }
    }
}
