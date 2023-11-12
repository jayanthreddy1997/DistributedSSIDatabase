package com.nyu.db.model;

import lombok.Getter;

@Getter
public class CommitOperation extends Operation {
    public CommitOperation(long timestamp, long transactionId) {
        super(timestamp, transactionId, OperationType.COMMIT);
    }
}
