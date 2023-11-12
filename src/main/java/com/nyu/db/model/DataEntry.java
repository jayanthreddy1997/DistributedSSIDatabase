package com.nyu.db.model;

import lombok.Data;

@Data
public class DataEntry {
    private int variableId;
    private int value;
    private long commitTimestamp;
}
