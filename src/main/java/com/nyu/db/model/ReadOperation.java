package com.nyu.db.model;

import lombok.Getter;

@Getter
public class ReadOperation extends Operation {
    private final int variableId;

    public ReadOperation(long transactionId, int variableId, long timestamp) {
        super(timestamp, transactionId, OperationType.READ);
        this.variableId = variableId;
    }
}
