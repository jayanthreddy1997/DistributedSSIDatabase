package com.nyu.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Variable {
    private int id;
    private int value;
    private long commitTimestamp;
}
