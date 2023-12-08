package com.nyu.db.model;

import lombok.Getter;

@Getter
public class SymbolOperation extends Operation {
    private final int variableId;

    public SymbolOperation(Transaction transaction, int variableId, long timestamp, OperationType operationType) {
        super(transaction, timestamp, operationType);
        this.variableId = variableId;
    }
}
