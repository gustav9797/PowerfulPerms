package com.github.cheesesoftware.PowerfulPerms.Vault;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class VaultHook {

    public VaultHook() {

    }

    public void hook(PowerfulPermsPlugin plugin) {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        PowerfulPerms_Vault_Permissions vaultPermsHook = new PowerfulPerms_Vault_Permissions(plugin);
        PowerfulPerms_Vault_Chat vaultChatHook = new PowerfulPerms_Vault_Chat(vaultPermsHook, plugin);
        Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPermsHook, vault, ServicePriority.High);
        Bukkit.getServicesManager().register(net.milkbowl.vault.chat.Chat.class, vaultChatHook, vault, ServicePriority.High);
    }
}
