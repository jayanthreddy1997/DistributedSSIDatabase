package com.nyu.db.model;

import lombok.Data;
import java.util.List;

@Data
public class Transaction {
    private long transactionId;
    private long startTimestamp;
    private List<Operation> operations;
}