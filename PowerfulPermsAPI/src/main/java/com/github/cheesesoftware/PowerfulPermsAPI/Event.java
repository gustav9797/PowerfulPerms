package com.github.cheesesoftware.PowerfulPermsAPI;

public class Event {
    protected String name;

    public String getEventName() {
        if (name == null) {
            name = getClass().getSimpleName();
        }
        return name;
    }
}
