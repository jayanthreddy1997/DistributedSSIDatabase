package com.nyu.db.datamanager;

import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;

import java.util.List;

public interface DataManager {

    public void registerVariable(int variableId);

    public int getSiteId();

    public List<Integer> getVariableIds();

    public int read(ReadOperation op);

    public boolean write(WriteOperation op);

    public boolean abort(long transactionId);

    public boolean canPreCommitTransaction(long transactionId);

    public boolean commitTransaction(long transactionId); // Commit the transaction

    public boolean recover();

    public void dumpVariableValues();

}
