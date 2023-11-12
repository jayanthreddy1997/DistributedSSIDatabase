package com.nyu.db.model;

import lombok.Getter;

@Getter
public class ReadOperation extends Operation {
    private final int variableId;

    public ReadOperation(int variableId, long timestamp, long transactionId) {
        super(timestamp, transactionId, OperationType.READ);
        this.variableId = variableId;
    }
}
