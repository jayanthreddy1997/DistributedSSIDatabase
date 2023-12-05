package com.nyu.db.transactionmanager.impl;

import com.nyu.db.Simulation;
import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.*;
import com.nyu.db.transactionmanager.SerializationGraph;
import com.nyu.db.transactionmanager.TransactionManager;
import com.nyu.db.utils.TimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TransactionManagerImpl implements TransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionManagerImpl.class);
    private Map<Integer, DataManager> siteToDataManagerMap;
    private Map<Integer, List<DataManager>> variableToDataManagerMap;
    private Map<Long, Transaction> transactionStore; // transactionId to transaction object
    private Set<Integer> replicatedVariables; // TODO: redundant
    private Map<Integer, Boolean> siteActiveStatus;
    private Map<Integer, Queue<Operation>> waitingOperations; // Waiting operations on each site

    // Store all active transactions that have had write on each site
    private Map<Integer, Set<Long>> siteToActiveWriteTransactions;
    private Set<Long> activeTransactions;
    private SerializationGraph serializationGraph;

    private void init() {
        this.siteToDataManagerMap = new HashMap<>();
        this.replicatedVariables = new HashSet<>();
        this.variableToDataManagerMap = new HashMap<>();
        this.siteActiveStatus = new HashMap<>();
        this.waitingOperations = new HashMap<>();
        this.siteToActiveWriteTransactions = new HashMap<>();
        this.transactionStore = new HashMap<>();
        this.activeTransactions = new HashSet<>();
        this.serializationGraph = new SerializationGraph();
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
        Transaction t = new Transaction(transactionId);
        this.transactionStore.put(transactionId, t);
        this.activeTransactions.add(transactionId);
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
        if (!this.checkTransactionActive(op)) {
            return Optional.empty();
        }
        List<DataManager> dataManagers = this.variableToDataManagerMap.get(op.getVariableId());
        if (dataManagers.isEmpty()) {
            throw new RuntimeException("No data node available to serve request: "+op);
        }

        Optional<Integer> val = Optional.empty();
        if (this.replicatedVariables.contains(op.getVariableId())) {
            boolean allSitesUp = true;
            for (DataManager dm: dataManagers) {
                if (this.siteActiveStatus.get(dm.getSiteId())) {
                    val = dm.read(op);
                    if (val.isPresent()) {
                        op.setExecutedTimestamp(TimeManager.getTime());
                        // TODO: Cleanup, replicated var queueing?
                        return val;
                    }
                } else {
                    allSitesUp = false;
                }
            }
            //val is guaranteed to be empty here
            if (allSitesUp) {
                abortTransaction(op.getTransaction().getTransactionId());
                return Optional.empty();
            }
            for (DataManager dm : dataManagers) {
                if (!this.siteActiveStatus.get(dm.getSiteId())) {
                    this.waitingOperations.get(dm.getSiteId()).add(op);
                    op.incrementNumQueuedSites();
                }
            }
        } else { //unreplicated
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
        if (val.isEmpty())
            logger.info(String.format(op + " put on wait since site is down"));

        return val;
    }

    @Override
    public boolean write(WriteOperation op) {
        if (!this.checkTransactionActive(op)) {
            return false;
        }
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
            logger.info(String.format(op + " put on wait since site is down"));
        } else {
            op.setExecutedTimestamp(TimeManager.getTime());
        }
        return writeStatus;
    }

    @Override
    public void fail(int siteId) {
        if (!this.siteActiveStatus.get(siteId)) {
            return;
        }
        logger.info("Failing site "+siteId);
        this.siteActiveStatus.put(siteId, false);
        this.siteToDataManagerMap.get(siteId).fail();
        for (long transactionId: this.siteToActiveWriteTransactions.get(siteId)) {
            logger.info(String.format("Aborting T%d since it wrote to site %d that went down before T%d committed",
                    transactionId, siteId, transactionId));
            this.abortTransaction(transactionId);
        }
    }

    @Override
    public void recover(int siteId) {
        // TODO: check for waiting operations!
        assert !this.siteActiveStatus.get(siteId) : "Cannot call recover on a site that is currently up!";

        DataManager dm = siteToDataManagerMap.get(siteId);
        for (Operation pendingOperation : this.waitingOperations.get(siteId)) {
            if (pendingOperation.getOperationType().equals(OperationType.READ)) {
                ReadOperation pendingReadOperation = ((ReadOperation) pendingOperation); // casting here :/
                if (pendingReadOperation.isExecuted()) { // Operation done
                    continue;
                }
                Transaction transaction = pendingOperation.getTransaction();
                Optional<Integer> val = dm.read(pendingReadOperation);
                if (val.isPresent()) {
                    pendingReadOperation.setExecutedTimestamp(TimeManager.getTime());
                    logger.info(
                            String.format("x%d: %d (T%d)",
                                    pendingReadOperation.getVariableId(),
                                    val.get(),
                                    pendingReadOperation.getTransaction().getTransactionId()
                            )
                    );
                }
                pendingReadOperation.decrementNumQueuedSites();
                if (!pendingReadOperation.isExecuted() && pendingReadOperation.getNumQueuedSites() == 0) {
                    abortTransaction(transaction.getTransactionId());
                }
            } else if (pendingOperation.getOperationType().equals(OperationType.WRITE)) {
                WriteOperation pendingWriteOperation = ((WriteOperation) pendingOperation);
                dm.write(pendingWriteOperation);
            }
        }
        logger.info("Recovering site "+siteId);
        this.siteActiveStatus.put(siteId, true);
        this.siteToDataManagerMap.get(siteId).recover();
    }

    @Override
    public boolean commitTransaction(CommitOperation op) {
        if (!this.checkTransactionActive(op)) {
            return false;
        }
        long transactionId = op.getTransaction().getTransactionId();
        boolean precommitStatus = true;
        for (int site: this.siteToActiveWriteTransactions.keySet()) {
            if (this.siteToActiveWriteTransactions.get(site).contains(transactionId)) {
                // Commit conditions have to succeed on every site, else abort
                precommitStatus = precommitStatus && this.siteToDataManagerMap.get(site).precommitTransaction(op);
                if (!precommitStatus) {
                    break;
                }
            }
        }
        precommitStatus = precommitStatus && this.serializationGraph.addTransactionAndRunChecks(op.getTransaction());
        if (!precommitStatus) {
            logger.info("T"+transactionId+" aborts");
            cleanupTransaction(transactionId);
            return false;
        }

        boolean commitStatus = true;
        for (int site: this.siteToActiveWriteTransactions.keySet()) {
            if (this.siteToActiveWriteTransactions.get(site).contains(transactionId)) {
                commitStatus = commitStatus && this.siteToDataManagerMap.get(site).commitTransaction(op);
                if (!commitStatus) {
                    break;
                }
            }
        }

        logger.info("T"+transactionId+(commitStatus?" commits":" aborts"));
        op.getTransaction().setCommitTimestamp(TimeManager.getTime());
        cleanupTransaction(transactionId);
        return commitStatus;
    }

    private void abortTransaction(long transactionId) {
        logger.info("T"+transactionId+" aborts");
        this.cleanupTransaction(transactionId);
    }

    private void cleanupTransaction(long transactionId) {
        this.activeTransactions.remove(transactionId);
        for (int siteId: this.siteToActiveWriteTransactions.keySet()) {
            Set<Long> activeTransactions = this.siteToActiveWriteTransactions.get(siteId);
            if (activeTransactions.contains(transactionId)) {
                this.siteToDataManagerMap.get(siteId).abortTransaction(transactionId);
                activeTransactions.remove(transactionId);
            }
        }
    }

    private boolean checkTransactionActive(Operation op) {
        if (!this.activeTransactions.contains(op.getTransaction().getTransactionId())) {
            logger.warn(op + " received operation on transaction that has either already aborted or committed");
            return false;
        }
        return true;
    }

    public void printCommittedState() {
        // Print committed values of all copies of all variables at all sites,
        List<Integer> siteIds = new ArrayList<>(siteToDataManagerMap.keySet());
        Collections.sort(siteIds);
        for (int siteId: siteIds) {
            System.out.printf("site %d - ", siteId);
            this.siteToDataManagerMap.get(siteId).printCommittedState();
        }
    }

    public Transaction getTransaction(long transactionId) {
        return this.transactionStore.get(transactionId);
    }
}
