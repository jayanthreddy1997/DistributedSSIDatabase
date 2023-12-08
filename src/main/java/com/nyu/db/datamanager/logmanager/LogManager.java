package com.nyu.db.datamanager.logmanager;

import com.nyu.db.model.LogEntry;

/**
 * Code not used currently
 * Manages Log Buffer(in-memory) and Log(stable storage)
 */
public interface LogManager {

    public void pushToBuffer(LogEntry logEntry);

    public void flushBuffer();

    /**
     * Read from log on stable storage and persist to data file
     * @return
     */
    public boolean recover();

}
