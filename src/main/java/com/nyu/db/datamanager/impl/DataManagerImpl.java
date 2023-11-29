package com.nyu.db.datamanager.impl;

import com.nyu.db.datamanager.DataManager;
import com.nyu.db.model.ReadOperation;
import com.nyu.db.model.WriteOperation;

import java.util.ArrayList;
import java.util.List;

public class DataManagerImpl implements DataManager {
    // TODO: Complete implementation
    private int siteId;
    private List<Integer> managedVariables;
    public DataManagerImpl(int siteId){
        this.siteId = siteId;
        this.managedVariables = new ArrayList<>();
    }

    @Override
    public void registerVariable(int variableId) {
        this.managedVariables.add(variableId);
    }

    @Override
    public int getSiteId() {
        return this.siteId;
    }

    @Override
    public List<Integer> getVariableIds() {
        return this.managedVariables;
    }

    @Override
    public int read(ReadOperation op) {
        return 0;
    }

    @Override
    public boolean write(WriteOperation op) {
        return false;
    }

    @Override
    public boolean abort(long transactionId) {
        return false;
    }

    @Override
    public boolean canPreCommitTransaction(long transactionId) {
        return false;
    }

    @Override
    public boolean commitTransaction(long transactionId) {
        return false;
    }

    @Override
    public boolean recover() {
        return false;
    }

    @Override
    public void dumpVariableValues() {
        // TODO: print variable values in sorted order on same line
    };
}
