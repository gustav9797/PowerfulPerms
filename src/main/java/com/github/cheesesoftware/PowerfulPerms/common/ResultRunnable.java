package com.github.cheesesoftware.PowerfulPerms.common;

public abstract class ResultRunnable implements Runnable {
    
    protected Object result = null;

    public void setResult(Object result) {
        this.result = result;
    }
    
}
