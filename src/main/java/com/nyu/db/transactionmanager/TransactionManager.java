package com.nyu.db.transactionmanager;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.Transaction;
import com.nyu.db.model.WriteOperation;

import java.util.List;

public interface TransactionManager {
    public Transaction createTransaction();

    public void configureDataManagers(List<DataManager> dataManagers);

    public int read(ReadOperation op);

    public boolean write(WriteOperation op);

    public void fail(int siteId);

    public void recover(int siteId);

    public boolean commitTransaction(long transactionId); // Commit the transaction

}
