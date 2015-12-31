package com.github.cheesesoftware.PowerfulPerms.common;

public abstract class ResponseRunnable implements Runnable {

    protected boolean success = false;
    protected String response = "";
    
    public ResponseRunnable() {

    }
    
    public void setResponse(boolean success, String response) {
        this.success = success;
        this.response = response;
    }
}
