package com.nyu.db.model;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class Operation {
    private final long timestamp;
    private final long transactionId;
    private final OperationType operationType;
}

// TODO: figure out serializability