package com.nyu.db.model;

import com.nyu.db.utils.TimeManager;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Transaction {
    private final long transactionId;
    private final long startTimestamp;
    private long commitTimestamp;
    private final List<SymbolOperation> operations;

    public Transaction(long transactionId) {
        this.transactionId = transactionId;
        this.startTimestamp = TimeManager.getTime();
        this.commitTimestamp = -1;
        this.operations = new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj)
            return true;
        if (obj==null || obj.getClass()!=this.getClass())
            return false;
        return this.getTransactionId()==((Transaction)obj).getTransactionId();
    }

    @Override
    public int hashCode() {
        return (int)(this.getTransactionId()%Integer.MAX_VALUE);
    }
}