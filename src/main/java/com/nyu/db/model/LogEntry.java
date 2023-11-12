package com.nyu.db.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Optional;

@Data
public class LogEntry implements Serializable {
    private Operation operation; // CommitOperation or WriteOperation
}
