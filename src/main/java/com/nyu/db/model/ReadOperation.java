package com.nyu.db.model;

import lombok.Getter;

@Getter
public class ReadOperation extends Operation {
    private final int variableId;
    private int numQueuedSites = 0;
    public ReadOperation(Transaction transaction, int variableId, long timestamp) {
        super(transaction, timestamp, OperationType.READ);
        this.variableId = variableId;
    }

    @Override
    public String toString() {
        return String.format("R(T%d, x%d)", this.getTransaction().getTransactionId(), variableId);
    }

    public void incrementNumQueuedSites() {
        this.numQueuedSites += 1;
    }
    public void decrementNumQueuedSites() {
        this.numQueuedSites -= 1;
    }
}
