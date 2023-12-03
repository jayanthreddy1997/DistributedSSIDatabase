package com.nyu.db.transactionmanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.*;
import com.nyu.db.transactionmanager.TransactionManager;
import com.nyu.db.utils.TimeManager;

import java.util.*;

public class TransactionManagerImpl implements TransactionManager {

    // TODO: implement SSI graph
    private Map<Integer, DataManager> siteToDataManagerMap;
    private Map<Integer, List<DataManager>> variableToDataManagerMap;
    private Map<Long, Transaction> transactionStore; // transactionId to transaction object
    private Set<Integer> replicatedVariables; // TODO: redundant
    private Map<Integer, Boolean> siteActiveStatus;
    private Map<Integer, Queue<Operation>> waitingOperations; // Waiting operations on each site

    // Store all active transactions that have had write on each site
    private Map<Integer, Set<Long>> siteToActiveWriteTransactions;

    private void init() {
        this.siteToDataManagerMap = new HashMap<>();
        this.replicatedVariables = new HashSet<>();
        this.variableToDataManagerMap = new HashMap<>();
        this.siteActiveStatus = new HashMap<>();
        this.waitingOperations = new HashMap<>();
        this.siteToActiveWriteTransactions = new HashMap<>();
        this.transactionStore = new HashMap<>();
    }

    public TransactionManagerImpl() {
        init();
    }

    public TransactionManagerImpl(List<DataManager> dataManagers) {
        init();
        this.configureDataManagers(dataManagers);
    }

    @Override
    public Transaction createTransaction(long transactionId) {
        Transaction t = new Transaction(transactionId, TimeManager.getTime(), new ArrayList<>());
        this.transactionStore.put(transactionId, t);
        return t;
    }

    @Override
    public void configureDataManagers(List<DataManager> dataManagers) {
        for (DataManager dm: dataManagers) {
            this.siteToDataManagerMap.put(dm.getSiteId(), dm);
            this.siteToActiveWriteTransactions.put(dm.getSiteId(), new HashSet<>());
            for (Integer variableId: dm.getManagedVariableIds()) {
                if (!this.variableToDataManagerMap.containsKey(variableId)) {
                    this.variableToDataManagerMap.put(variableId, new ArrayList<>());
                }
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

        Optional<Integer> val = Optional.empty();
        if (this.replicatedVariables.contains(op.getVariableId())) {
            for (DataManager dm: dataManagers) {
                if (this.siteActiveStatus.get(dm.getSiteId())) {
                    val = dm.read(op);
                    if (val.isPresent()) {
                        op.setExecutedTimestamp(TimeManager.getTime());
                        return val;
                    }
                }
            }
        } else {
            DataManager dm = dataManagers.get(0);

            if (this.siteActiveStatus.get(dm.getSiteId())) {
                val = dm.read(op, false);
                if (val.isPresent()) {
                    op.setExecutedTimestamp(TimeManager.getTime());
                }
            } else {
                // Wait for site to become available
                this.waitingOperations.get(dm.getSiteId()).add(op);
            }
        }
        return val;
    }

    @Override
    public boolean write(WriteOperation op) {
        List<DataManager> dataManagers = this.variableToDataManagerMap.get(op.getVariableId());
        if (dataManagers.isEmpty()) {
            throw new RuntimeException("No data node available to serve request: "+op);
        }

        boolean writeStatus = false;
        boolean currentWriteStatus;
        for (DataManager dm: dataManagers) {
            currentWriteStatus = dm.write(op);
            if (currentWriteStatus) {
                Set<Long> activeTransactions = this.siteToActiveWriteTransactions.get(dm.getSiteId());
                activeTransactions.add(op.getTransaction().getTransactionId());
            }
            writeStatus = writeStatus || currentWriteStatus;
        }

        if (!writeStatus) {
            // Wait for site to become available
            for (DataManager dm: dataManagers) {
                this.waitingOperations.get(dm.getSiteId()).add(op);
            }
        } else {
            op.setExecutedTimestamp(TimeManager.getTime());
        }
        return writeStatus;
    }

    @Override
    public void fail(int siteId) {
        // TODO: Manage failure history
        // TODO: Site goes down, immediately fail the transactions that wrote on that site!
        if (this.siteActiveStatus.get(siteId)) {
            this.siteActiveStatus.put(siteId, false);
            this.siteToDataManagerMap.get(siteId).fail();
        }
    }

    @Override
    public void recover(int siteId) {
        // TODO: check for waiting reads!
    }

    @Override
    public boolean commitTransaction(CommitOperation op) {
        // TODO: Do we queue in any case?

        boolean commitStatus = true;
        for (int site: this.siteToActiveWriteTransactions.keySet()) {
            if (this.siteToActiveWriteTransactions.get(site).contains(op.getTransaction().getTransactionId())) {
                // Commit has to succeed on every site, else abort // TODO: think if condition is correct
                commitStatus = commitStatus && this.siteToDataManagerMap.get(site).commitTransaction(op);
            }
        }

        return commitStatus;
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

    public Transaction getTransaction(long transactionId) {
        return this.transactionStore.get(transactionId);
    }
}
