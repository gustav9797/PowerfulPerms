package com.github.gustav9797.PowerfulPermsAPI;

public interface EventHandler {

    public void registerListener(PowerfulPermsListener listener);

    public void unregisterListener(PowerfulPermsListener listener);

    public void fireEvent(Event event);

}
