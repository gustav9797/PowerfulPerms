package com.github.cheesesoftware.PowerfulPerms;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;

public class PermissibleBaseInjector {
    private static final String CRAFTBUKKIT_PREFIX = "org.bukkit.craftbukkit";
    private static final String VERSION;

    static {
        @SuppressWarnings("rawtypes")
        Class serverClass = Bukkit.getServer().getClass();
        if (!serverClass.getSimpleName().equals("CraftServer")) {
            VERSION = null;
        } else if (serverClass.getName().equals("org.bukkit.craftbukkit.CraftServer")) {
            VERSION = ".";
        } else {
            String name = serverClass.getName();
            name = name.substring("org.bukkit.craftbukkit".length());
            name = name.substring(0, name.length() - "CraftServer".length());
            VERSION = name;
        }
    }

    public static String getCBClassName(String name) {
        if (VERSION == null) {
            return null;
        }
        return CRAFTBUKKIT_PREFIX + VERSION + name;
    }
    
    protected final String className, fieldName;
    
    public PermissibleBaseInjector() {
        this.className = getCBClassName("entity.CraftHumanEntity"); 
        this.fieldName = "perm";
    }
    
    public Permissible inject(Player player, Permissible permissible) throws NoSuchFieldException, IllegalAccessException {
        Field permField = getPermissibleField(player);
        if (permField == null) {
            return null;
        }
        Permissible oldPerm = (Permissible) permField.get(player);
        if (permissible instanceof PermissibleBase) {
            PermissibleBase newBase = (PermissibleBase) permissible;
            permField.set(player, newBase);
        }

        // Inject permissible
        permField.set(player, permissible);
        return oldPerm;
    }
    
    private Field getPermissibleField(Player player) throws NoSuchFieldException {
        Class<?> humanEntity;
        try {
            humanEntity = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "You're not using Spigot. Spigot must be used for permissions to work properly.");
            return null;
        }

        if (!humanEntity.isAssignableFrom(player.getClass())) {
            Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "Could not inject permissible. Using default Bukkit permissions.");
            return null;
        }

        Field permField = humanEntity.getDeclaredField(fieldName);
        // Make it public for reflection
        permField.setAccessible(true);
        return permField;
    }
}
