package com.github.gustav9797.PowerfulPermsAPI;

import java.util.UUID;

import com.google.common.base.Charsets;

// ONLY!! set [default] to this!
public class DefaultPermissionPlayer {

    public static UUID getUUID() {
        return java.util.UUID.nameUUIDFromBytes(("[default]").getBytes(Charsets.UTF_8));
    }

}
