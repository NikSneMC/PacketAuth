package com.niksne.packetauth;

import java.sql.ResultSet;

public class ExecuteReturn {
    private int updateResult;
    private ResultSet queryResult;
    private boolean executeResult;

    public ExecuteReturn(int updateResult) {
        this.updateResult = updateResult;
    }

    public ExecuteReturn(ResultSet queryResult) {
        this.queryResult = queryResult;
    }

    public ExecuteReturn(boolean executeResult) {
        this.executeResult = executeResult;
    }

    public ExecuteReturn(Object o) {
    }

    public int getUpdateResult() {
        return updateResult;
    }

    public ResultSet getQueryResult() {
        return queryResult;
    }

    public boolean getExecuteResult() {
        return executeResult;
    }
}
