package com.nyu.db.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Operation {
    private static final long NOT_EXECUTED = Integer.MIN_VALUE;

    private final Transaction transaction;
    private final long createdTimestamp;
    @Setter
    private long executedTimestamp;
    private final OperationType operationType;

    public Operation(Transaction transaction, long timestamp, OperationType operationType) {
        this.transaction = transaction;
        this.createdTimestamp = timestamp;
        this.operationType = operationType;
        this.executedTimestamp = NOT_EXECUTED;
    }

    public boolean isExecuted() {
        return this.executedTimestamp != NOT_EXECUTED;
    }

}
