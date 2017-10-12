package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

import redis.clients.jedis.Jedis;

public class TestRedisCommand extends SubCommand {

    public TestRedisCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp test redis");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.test.redis")) {
            if (args != null && args.length >= 1 && args[0].equalsIgnoreCase("redis")) {
                final PermissionManagerBase base = (PermissionManagerBase) permissionManager;
                if (PermissionManagerBase.redisEnabled) {
                    plugin.runTaskAsynchronously(() -> {
                        Jedis jedis = base.getRedisConnection();
                        if (jedis != null) {
                            sendSender(invoker, sender, "Redis ping message has been sent. If you get no reply, no other servers are connected to Redis.");
                            jedis.publish("PowerfulPerms", "[ping]" + " " + PermissionManagerBase.serverName + " " + sender);
                            jedis.close();
                        } else
                            sendSender(invoker, sender, "Could not connect to the Redis server.");
                    });
                } else
                    sendSender(invoker, sender, "Redis is not enabled in your config.");
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("redis".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("redis");
            return output;
        }
        return null;
    }

}
