package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.google.common.base.Charsets;

public class DefaultPermissionPlayer extends PermissionPlayerBase {

    public DefaultPermissionPlayer(HashMap<String, List<CachedGroup>> groups, List<Permission> permissions, String prefix, String suffix, PowerfulPermsPlugin plugin) {
        super(groups, permissions, prefix, suffix, plugin);
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    public static UUID getUUID() {
        return java.util.UUID.nameUUIDFromBytes(("[default]").getBytes(Charsets.UTF_8));
    }

}
