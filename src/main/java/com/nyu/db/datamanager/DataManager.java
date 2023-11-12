package com.nyu.db.datamanager;

import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;

public interface DataManager {

    public int read(ReadOperation op);

    public boolean write(WriteOperation op);

    public boolean abort(long transactionId);

    public boolean canPreCommitTransaction(long transactionId);

    public boolean commitTransaction(long transactionId); // Commit the transaction
}
