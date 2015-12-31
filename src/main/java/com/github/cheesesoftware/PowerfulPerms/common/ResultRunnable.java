package com.github.cheesesoftware.PowerfulPerms.common;

public abstract class ResultRunnable implements Runnable {
    
    protected Object result = null;
    protected boolean sameThread = false;
    
    public ResultRunnable() {

    }
    
    public ResultRunnable(boolean sameThread) {
        this.sameThread = sameThread;
    }

    public void setResult(Object result) {
        this.result = result;
    }
    
    public boolean sameThread() {
        return this.sameThread;
    }
    
}
