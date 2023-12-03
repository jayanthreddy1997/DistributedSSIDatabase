package com.nyu.db.model;

import lombok.Getter;

@Getter
public class WriteOperation extends Operation {
    private final int variableId;
    private final int value;

    public WriteOperation(Transaction transaction, int variableId, int value, long timestamp) {
        super(transaction, timestamp, OperationType.WRITE);
        this.variableId = variableId;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("W(T%d, x%d, %d)", this.getTransaction().getTransactionId(), variableId, value);
    }
}
