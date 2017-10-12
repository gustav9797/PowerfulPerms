package com.github.gustav9797.PowerfulPerms.common.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsListener;
import com.github.gustav9797.PowerfulPermsAPI.Event;
import com.github.gustav9797.PowerfulPermsAPI.EventHandler;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulEvent;

public class PowerfulEventHandler implements EventHandler {
    private List<PowerfulPermsListener> listeners;

    public PowerfulEventHandler() {
        listeners = new ArrayList<>();
    }

    public void registerListener(PowerfulPermsListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(PowerfulPermsListener listener) {
        listeners.remove(listener);
    }

    public void fireEvent(Event event) {
        for (PowerfulPermsListener listener : listeners) {
            for (Method method : listener.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PowerfulEvent.class)) {
                    if (method.getParameterTypes().length > 0) {
                        Class<?> methodParameter = method.getParameterTypes()[0];
                        if (Event.class.isAssignableFrom(methodParameter)) {
                            if (event.getClass().equals(methodParameter)) {
                                try {
                                    method.invoke(listener, event);
                                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                }
            }
        }
    }

}
