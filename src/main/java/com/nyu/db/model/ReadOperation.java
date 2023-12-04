package com.nyu.db.model;

import lombok.Getter;

@Getter
public class ReadOperation extends SymbolOperation {

    public ReadOperation(Transaction transaction, int variableId, long timestamp) {
        super(transaction, variableId, timestamp, OperationType.READ);
    }

    @Override
    public String toString() {
        return String.format("R(T%d, x%d)", this.getTransaction().getTransactionId(), this.getVariableId());
    }
}
