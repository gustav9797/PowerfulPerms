package com.github.cheesesoftware.PowerfulPerms.database;

public abstract class DBRunnable implements Runnable {

    protected DBResult result;

    public void setResult(DBResult result) {
        this.result = result;
    }

}
