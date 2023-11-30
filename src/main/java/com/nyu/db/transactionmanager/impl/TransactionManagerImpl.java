package com.nyu.db.transactionmanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.*;
import com.nyu.db.transactionmanager.TransactionManager;

import java.util.*;

public class TransactionManagerImpl implements TransactionManager {

    // TODO: implement SSI graph
    private Map<Integer, DataManager> siteToDataManagerMap;
    private Map<Integer, List<DataManager>> variableToDataManagerMap;
    private Map<Long, Transaction> transactionStore; // transactionId to transaction object
    private Set<Integer> replicatedVariables;
    private Map<Integer, Boolean> siteActiveStatus;
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
        Transaction t = new Transaction(transactionId, startTimestamp, new ArrayList<>(), new HashMap<>());
        this.transactionStore.put(transactionId, t);
        return t;
    }

    @Override
    public void configureDataManagers(List<DataManager> dataManagers) {
        for (DataManager dm: dataManagers) {
            this.siteToDataManagerMap.put(dm.getSiteId(), dm);
            for (Integer variableId: dm.getManagedVariableIds()) {
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
     * @return Integer value read from database, None if operation put to wait
     */
    @Override
    public Optional<Integer> read(ReadOperation op) {
        List<DataManager> dataManagers = this.variableToDataManagerMap.get(op.getVariableId());
        if (dataManagers.isEmpty()) {
            throw new RuntimeException("No data node available to serve request: "+op);
        }

        Transaction transaction = this.transactionStore.get(op.getTransactionId());
        if (transaction.getWrites().containsKey(op.getVariableId())) {
            // Read value written inside same transaction if exists
            // TODO: Is it ok for the transaction manager to serve reads? probably fine!
            return Optional.of(transaction.getWrites().get(op.getVariableId()));
        }

        if (replicatedVariables.contains(op.getVariableId())) {
            for (DataManager dm: dataManagers) {
                if (siteActiveStatus.get(dm.getSiteId())) {
                    Optional<Integer> val = dm.read(op, true);
                    if (val.isPresent()) {
                        return val;
                    }
                }
            }
        } else {
            DataManager dm = dataManagers.get(0);

            if (this.siteActiveStatus.get(dm.getSiteId())) {
                return dm.read(op, false);
            } else {
                // Wait for site to become available
                this.waitingOperations.get(dm.getSiteId()).add(op);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean write(WriteOperation op) {
        List<DataManager> dataManagers = this.variableToDataManagerMap.get(op.getVariableId());
        if (dataManagers.isEmpty()) {
            throw new RuntimeException("No data node available to serve request: "+op);
        }
        boolean writeStatus = false;
        if (replicatedVariables.contains(op.getVariableId())) {
            for (DataManager dm: dataManagers) {
                writeStatus = writeStatus || dm.write(op);
            }
        } else {
            DataManager dm = dataManagers.get(0);
            if (this.siteActiveStatus.get(dm.getSiteId())) {
                writeStatus = dm.write(op);
            } else {
                // Wait for site to become available
                this.waitingOperations.get(dm.getSiteId()).add(op);
            }
        }
        return writeStatus;
    }

    @Override
    public void fail(int siteId) {
        // TODO: Manage failure history
        this.siteToDataManagerMap.get(siteId).fail();
    }

    @Override
    public void recover(int siteId) {
        // TODO: check for waiting reads!
    }

    @Override
    public boolean commitTransaction(CommitOperation op) {
        return true;
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
