package com.github.cheesesoftware.PowerfulPerms.Redis;

import java.util.UUID;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisConnection {

    private PermissionManagerBase permissionManager;
    private PowerfulPermsPlugin plugin;

    private String ip;
    private int port;
    private String password;
    private int timeout = 2000;

    private JedisPool pool;
    private JedisPubSub subscriber;
    private Jedis subscriberJedis;
    private boolean needsSubscription = true;

    private static String redisMessage = "Unable to connect to your Redis server. Make sure the config is correct.";

    private int taskId;

    public RedisConnection(PermissionManagerBase permissionManager, PowerfulPermsPlugin plugin, String ip, int port, String password) {
        this.permissionManager = permissionManager;
        this.plugin = plugin;
        this.ip = ip;
        this.port = port;
        this.password = password;
        setupPool();
        setupSubscriber();
        subscribeSubscriber();
        taskId = permissionManager.getScheduler().runRepeating(new Runnable() {

            @Override
            public void run() {
                Jedis jedis = getConnection();
                if (jedis != null)
                    jedis.close();
            }
        }, 60);
    }

    private void setupPool() {
        if (password == null || password.isEmpty())
            pool = new JedisPool(new GenericObjectPoolConfig(), ip, port, timeout);
        else
            pool = new JedisPool(new GenericObjectPoolConfig(), ip, port, timeout, password);
    }

    private void setupSubscriber() {
        subscriber = (new JedisPubSub() {
            @Override
            public void onMessage(String channel, final String msg) {
                permissionManager.getScheduler().runAsync(new Runnable() {
                    public void run() {
                        // Reload player or groups depending on message
                        String[] split = msg.split(" ");
                        if (split.length >= 2) {
                            String first = split[0];
                            String server = split[1];

                            if (server.equals(PermissionManagerBase.serverId))
                                return;
                            if (first.equals("[groups]")) {
                                permissionManager.loadGroups();
                                plugin.getLogger().info(PermissionManagerBase.consolePrefix + "Reloaded all groups.");
                            } else if (first.equals("[players]")) {
                                permissionManager.loadGroups();
                                plugin.getLogger().info(PermissionManagerBase.consolePrefix + "Reloaded all players.");
                            } else if (first.equals("[ping]") && split.length >= 3) {
                                String sender = split[2];
                                Jedis temp = pool.getResource();
                                temp.publish("PowerfulPerms", "[pingreply]" + " " + PermissionManagerBase.serverName + " " + sender);
                                temp.close();
                            } else if (first.equals("[pingreply]") && split.length >= 3) {
                                String sender = split[2];
                                plugin.sendPlayerMessage(sender, "Received Redis ping from server \"" + server + "\".");
                            } else {
                                UUID uuid = UUID.fromString(first);
                                permissionManager.loadPlayer(uuid, plugin.getPlayerName(uuid), false, false);
                                plugin.getLogger().info(PermissionManagerBase.consolePrefix + "Reloaded player \"" + first + "\".");
                            }
                        }
                    }
                }, false);
            }
        });
    }

    private void subscribeSubscriber() {
        permissionManager.getScheduler().runAsync(new Runnable() {
            public void run() {
                try {
                    subscriberJedis = new Jedis(ip, port);
                    if (password != null && !password.isEmpty())
                        subscriberJedis.auth(password);
                    needsSubscription = false;
                    subscriberJedis.subscribe(subscriber, "PowerfulPerms");
                } catch (JedisConnectionException e) {
                    if (pool != null)
                        pool.destroy();
                    needsSubscription = true;
                    e.printStackTrace();
                    plugin.getLogger().warning(redisMessage);
                } finally {
                    if (subscriberJedis != null)
                        subscriberJedis.close();
                }
            }
        }, false);
    }

    public Jedis getConnection() {
        if (pool == null || pool.isClosed()) {
            plugin.getLogger().info("Setting up Redis pool.");
            setupPool();
        } else if (subscriber == null) {
            plugin.getLogger().info("Setting up Redis subscriber.");
            setupSubscriber();
        }

        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            if (!subscriber.isSubscribed() || needsSubscription) {
                subscribeSubscriber();
            }
        } catch (JedisConnectionException e) {
            if (jedis != null)
                jedis.close();
            if (pool != null)
                pool.destroy();
            needsSubscription = true;
            e.printStackTrace();
            plugin.getLogger().severe(redisMessage);
        }
        return jedis;
    }

    public void destroy() {
        if (subscriber != null)
            subscriber.unsubscribe();
        if (pool != null)
            pool.destroy();
        permissionManager.getScheduler().stopRepeating(taskId);
    }
}
