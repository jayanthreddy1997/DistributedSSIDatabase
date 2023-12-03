package com.nyu.db.model;

import lombok.Getter;

@Getter
public class CommitOperation extends Operation {
    public CommitOperation(Transaction transaction, long timestamp) {
        super(transaction, timestamp, OperationType.COMMIT);
    }
}
