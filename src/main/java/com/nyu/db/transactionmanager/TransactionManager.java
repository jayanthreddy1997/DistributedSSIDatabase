package com.nyu.db.transactionmanager;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.CommitOperation;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.Transaction;
import com.nyu.db.model.WriteOperation;

import java.util.List;
import java.util.Optional;

/**
 * Transaction Manager, routes operations to the appropriate data manager, maintains SSI graph
 */
public interface TransactionManager {
    /**
     * Create a new transaction
     * @param transactionId ID of the new transaction
     * @return Transaction object
     */
    public Transaction createTransaction(long transactionId);

    /**
     * Configure the transaction manager to manage the provided data managers
     * @param dataManagers List of data managers
     */
    public void configureDataManagers(List<DataManager> dataManagers);

    /**
     * Read a variable's value
     * @param op Read operation with details of transaction and which variable to read
     * @return Optional value of variable, Optional.empty() if site cannot serve the read
     */
    public Optional<Integer> read(ReadOperation op);

    /**
     * Write to a variable
     * @param op Write operation with details on transaction, variable and value
     * @return true if write succeeds, false otherwise
     */
    public boolean write(WriteOperation op);

    /**
     * Method to manually fail a site. This is only for simulation purposes
     */
    public void fail(int siteId);

    /**
     * Method to manually recover a site. This is only for simulation purposes
     */
    public void recover(int siteId);

    /**
     * Commits a transaction
     * @param op Commit operation with details of the transaction to commit
     * @return true if the commit succeeded, false otherwise
     */
    public boolean commitTransaction(CommitOperation op);

    /**
     * Prints the committed variable values across all sites
     */
    public void printCommittedState();

    /**
     * Get transaction object from transaction ID
     * @param transactionId ID of transaction to fetch
     * @return Transaction object
     */
    public Transaction getTransaction(long transactionId);
}
