package com.nyu.db.datamanager.databuffer;

import com.nyu.db.model.DataEntry;

import java.util.List;

/**
 * Code not used currently
 */
public interface DataBuffer {

    public List<DataEntry> getAllData(long uptoTimestamp);

    public int read(int variableId, long timestamp);

    public void write(DataEntry dataEntry);

    public void clearUptoTimestamp(long uptoTimestamp);

}
