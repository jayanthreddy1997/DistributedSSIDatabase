package com.nyu.db.datamanager;

import com.nyu.db.model.CommitOperation;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;

import java.util.Set;
import java.util.Optional;


/**
 * Data Manager which manages operations on each data node
 */
public interface DataManager {

    /**
     * Register a variable to be managed under this data manager
     * @param variableId Variable id
     * @param initValue Initial variable value (considered to be the first committed value)
     */
    public void registerVariable(int variableId, int initValue);

    /**
     * Get site ID of the node
     * @return Integer representing the site id
     */
    public int getSiteId();

    /**
     * Gets variable ids managed by this data manager
     * @return Set of variable ids
     */
    public Set<Integer> getManagedVariableIds();

    /**
     * Read a variable's value
     * @param op Read operation with details of transaction and which variable to read
     * @return Optional value of variable, Optional.empty() if site cannot serve the read
     */
    public Optional<Integer> read(ReadOperation op);

    /**
     * Read a variable's value
     * @param op Read operation with details of transaction and which variable to read
     * @param runConsistencyChecks Boolean configuring whether to run consistency checks
     * @return Optional value of variable, Optional.empty() if site cannot serve the read
     */
    public Optional<Integer> read(ReadOperation op, boolean runConsistencyChecks);

    /**
     * Write to a variable
     * @param op Write operation with details on transaction, variable and value
     * @return true if write succeeds, false otherwise
     */
    public boolean write(WriteOperation op);

    /**
     * Aborts a transaction
     * @param transactionId ID of the transaction
     */
    public void abortTransaction(long transactionId);

    /**
     * Tests if the transaction is allowed to commit based on the First Committer wins rule
     * @param op Commit operation with details of the transaction to commit
     * @return true if the commit can go through, false if the checks failed
     */
    public boolean precommitTransaction(CommitOperation op);

    /**
     * Commits the values from the transaction to the committed snapshots
     * @param op Commit operation with details of the transaction to commit
     * @return true if the commit succeeded, false otherwise
     */
    public boolean commitTransaction(CommitOperation op);

    /**
     * Method to manually fail a data manager. This is only for simulation purposes
     */
    public void fail();

    /**
     * Method to manually recover a data manager. This is only for simulation purposes
     */
    public void recover();

    /**
     * Prints the committed variable values on this site
     */
    public void printCommittedState();

}
