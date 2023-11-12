package com.nyu.db.datamanager.persistentstoremanager;

import com.nyu.db.model.DataEntry;

import java.util.List;
import java.util.Map;

public interface PersistentStoreManager {

    public int read(int variableId, long timestamp);

    /**
     * Persist data to stable storage
     * @return
     */
    public boolean write(List<DataEntry> data);

}
