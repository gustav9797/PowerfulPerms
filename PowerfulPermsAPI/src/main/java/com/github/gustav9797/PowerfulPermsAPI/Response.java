package com.github.gustav9797.PowerfulPermsAPI;

public class Response {

    protected boolean success = false;
    protected String response = "";

    public Response(boolean success, String response) {
        this.success = success;
        this.response = response;
    }

    public String getResponse() {
        return this.response;
    }

    public boolean succeeded() {
        return this.success;
    }
}
