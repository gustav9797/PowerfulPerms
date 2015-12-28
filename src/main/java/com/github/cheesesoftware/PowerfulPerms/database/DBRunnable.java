package com.github.cheesesoftware.PowerfulPerms.database;

public abstract class DBRunnable implements Runnable {
   
    protected DBResult result;
    protected boolean sameThread = false;
    
    public DBRunnable() {

    }
    
    public DBRunnable(boolean sameThread) {
        this.sameThread = sameThread;
    }

    public void setResult(DBResult result) {
        this.result = result;
    }
    
    public boolean sameThread() {
        return this.sameThread;
    }

}
