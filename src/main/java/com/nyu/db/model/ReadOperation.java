package com.nyu.db.model;

import lombok.Getter;

@Getter
public class ReadOperation extends Operation {
    private final int variableId;

    public ReadOperation(Transaction transaction, int variableId, long timestamp) {
        super(transaction, timestamp, OperationType.READ);
        this.variableId = variableId;
    }

    @Override
    public String toString() {
        return String.format("R(T%d, x%d)", this.getTransaction().getTransactionId(), variableId);
    }
}
