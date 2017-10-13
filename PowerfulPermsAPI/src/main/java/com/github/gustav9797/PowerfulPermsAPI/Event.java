package com.github.gustav9797.PowerfulPermsAPI;

public class Event {
    protected String name;

    public String getEventName() {
        if (name == null) {
            name = getClass().getSimpleName();
        }
        return name;
    }
}
