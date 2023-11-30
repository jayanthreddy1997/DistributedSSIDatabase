package com.nyu.db.transactionmanager;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.CommitOperation;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.Transaction;
import com.nyu.db.model.WriteOperation;

import java.util.List;
import java.util.Optional;

public interface TransactionManager {
    public Transaction createTransaction(long transactionId, long startTimestamp);

    public void configureDataManagers(List<DataManager> dataManagers);

    public Optional<Integer> read(ReadOperation op);

    public boolean write(WriteOperation op);

    public void fail(int siteId);

    public void recover(int siteId);

    public boolean commitTransaction(CommitOperation op);

    public void dumpVariableValues();

}
