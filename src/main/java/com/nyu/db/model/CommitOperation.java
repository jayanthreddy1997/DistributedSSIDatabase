package com.nyu.db.model;

import lombok.Getter;

@Getter
public class CommitOperation extends Operation {
    public CommitOperation(long transactionId, long timestamp) {
        super(timestamp, transactionId, OperationType.COMMIT);
    }
}
