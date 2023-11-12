package com.nyu.db.transactionmanager;

import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.Transaction;
import com.nyu.db.model.WriteOperation;

// TODO: Clean up
public interface TransactionManager {
    public Transaction createTransaction();

    public int read(ReadOperation op);

    public boolean write(WriteOperation op);

    public boolean commitTransaction(long transactionId); // Commit the transaction

}
