package com.nyu.db.model;

import lombok.Getter;

@Getter
public class WriteOperation extends Operation {
    private final int variableId;
    private final int value;

    public WriteOperation(int variableId, int value, long timestamp, long transactionId) {
        super(timestamp, transactionId, OperationType.WRITE);
        this.variableId = variableId;
        this.value = value;
    }
}
