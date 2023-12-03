package com.nyu.db.model;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Operation {
    private final long timestamp;
    private final long transactionId;
    private final OperationType operationType;
    @Setter
    private boolean isComplete = false;
}
