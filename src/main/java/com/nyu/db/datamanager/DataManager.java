package com.nyu.db.datamanager;

import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;

import java.util.Set;
import java.util.Optional;

public interface DataManager {

    public void registerVariable(int variableId, int initValue);

    public int getSiteId();

    public Set<Integer> getManagedVariableIds();

    public Optional<Integer> read(ReadOperation op, boolean runConsistencyChecks);

    public boolean write(WriteOperation op);

    public boolean abort(long transactionId);

    public boolean canPreCommitTransaction(long transactionId);

    public boolean commitTransaction(long transactionId); // Commit the transaction

    public boolean fail();

    public boolean recover();

    public void dumpVariableValues();

}
