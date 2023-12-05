package com.nyu.db.model;

import lombok.Getter;

@Getter
public class WriteOperation extends SymbolOperation {

    private final int value;

    public WriteOperation(Transaction transaction, int variableId, int value, long timestamp) {
        super(transaction, variableId, timestamp, OperationType.WRITE);
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("W(T%d, x%d, %d)", this.getTransaction().getTransactionId(), this.getVariableId(), value);
    }
}
