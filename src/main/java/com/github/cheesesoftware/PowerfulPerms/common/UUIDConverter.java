package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.UUID;
//import java.util.regex.Pattern;


import com.github.cheesesoftware.PowerfulPermsAPI.IScheduler;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;
import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;

public class UUIDConverter {

    private IScheduler scheduler;
    // private static final Pattern UUID_PATTERN = Pattern.compile("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)");
    private static final HttpProfileRepository repository = new HttpProfileRepository("something");

    public UUIDConverter(IScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public UUID Convert(String playerName, ResultRunnable<UUID> resultRunnable) {
        Profile[] profile = repository.findProfilesByNames(playerName);
        if (profile.length >= 1) {
            String uuid = profile[0].getId();
            // return UUID.fromString(UUID_PATTERN.matcher(profile[0].getId()).replaceFirst("$1-$2-$3-$4-$5"));
            resultRunnable.setResult(UUID.fromString(uuid));
            scheduler.runSync(resultRunnable);
        }
        return null;
    }
}
