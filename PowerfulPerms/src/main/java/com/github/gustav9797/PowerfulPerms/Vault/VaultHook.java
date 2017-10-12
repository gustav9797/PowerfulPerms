package com.github.gustav9797.PowerfulPerms.Vault;

import com.github.gustav9797.PowerfulPerms.PowerfulPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public class VaultHook {

    public VaultHook() {

    }

    public void hook(PowerfulPerms plugin) {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        PowerfulPerms_Vault_Permissions vaultPermsHook = new PowerfulPerms_Vault_Permissions(plugin);
        PowerfulPerms_Vault_Chat vaultChatHook = new PowerfulPerms_Vault_Chat(vaultPermsHook, plugin);
        Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPermsHook, vault, ServicePriority.High);
        Bukkit.getServicesManager().register(net.milkbowl.vault.chat.Chat.class, vaultChatHook, vault, ServicePriority.High);
    }
}
