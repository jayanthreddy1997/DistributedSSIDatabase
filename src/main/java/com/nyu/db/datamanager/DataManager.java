package com.nyu.db.datamanager;

import com.nyu.db.model.CommitOperation;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;

import java.util.Set;
import java.util.Optional;

public interface DataManager {

    public void registerVariable(int variableId, int initValue);

    public int getSiteId();

    public Set<Integer> getManagedVariableIds();

    public Optional<Integer> read(ReadOperation op);
    public Optional<Integer> read(ReadOperation op, boolean runConsistencyChecks);

    public boolean write(WriteOperation op);

    public boolean abort(long transactionId);

    public boolean commitTransaction(CommitOperation op);

    public void fail();

    public void recover(long timestamp);

    public void dumpVariableValues();

}
