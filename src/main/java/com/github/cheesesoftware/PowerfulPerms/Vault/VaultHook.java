package com.github.cheesesoftware.PowerfulPerms.Vault;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import com.github.cheesesoftware.PowerfulPerms.PowerfulPermissionManager;

public class VaultHook {

    public VaultHook() {

    }

    public void hook(PowerfulPermissionManager permissionManager) {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        PowerfulPerms_Vault_Permissions vaultPermsHook = new PowerfulPerms_Vault_Permissions(permissionManager);
        PowerfulPerms_Vault_Chat vaultChatHook = new PowerfulPerms_Vault_Chat(vaultPermsHook, permissionManager);
        Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPermsHook, vault, ServicePriority.Normal);
        Bukkit.getServicesManager().register(net.milkbowl.vault.chat.Chat.class, vaultChatHook, vault, ServicePriority.Normal);
    }
}
