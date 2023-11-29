package com.nyu.db.model;

import lombok.Getter;

@Getter
public class WriteOperation extends Operation {
    private final int variableId;
    private final int value;

    public WriteOperation(long transactionId, int variableId, int value, long timestamp) {
        super(timestamp, transactionId, OperationType.WRITE);
        this.variableId = variableId;
        this.value = value;
    }
}
