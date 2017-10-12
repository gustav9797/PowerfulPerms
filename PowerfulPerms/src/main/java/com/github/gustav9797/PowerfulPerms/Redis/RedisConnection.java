package com.github.gustav9797.PowerfulPerms.Redis;

import java.util.UUID;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

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
    private Jedis jedis;
    private boolean isSubscribing = false;

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
        taskId = permissionManager.getScheduler().runRepeating(() -> {
            Jedis jedis = getConnection();
            if (jedis != null)
                jedis.close();
        }, 60);
    }

    private void setupPool() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setTestOnBorrow(true);
        if (password == null || password.isEmpty())
            pool = new JedisPool(config, ip, port, timeout);
        else
            pool = new JedisPool(config, ip, port, timeout, password);
    }

    private void setupSubscriber() {
        subscriber = (new JedisPubSub() {
            @Override
            public void onMessage(String channel, final String msg) {
                permissionManager.getScheduler().runAsync(() -> {
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
                            if (!plugin.isBungeeCord() || sender.equalsIgnoreCase("console"))
                                plugin.sendPlayerMessage(sender, "Received Redis ping from server \"" + server + "\".");
                        } else {
                            UUID uuid = UUID.fromString(first);
                            permissionManager.reloadPlayer(uuid);
                        }
                    }
                }, false);
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                isSubscribing = false;
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                isSubscribing = false;
            }
        });
    }

    private void subscribeSubscriber() {
        try {
            jedis = new Jedis(ip, port);
            if (password != null && !password.isEmpty())
                jedis.auth(password);
            isSubscribing = true;
            permissionManager.getScheduler().runAsync(() -> {
                try {
                    if (subscriber != null && subscriber.isSubscribed())
                        subscriber.unsubscribe();
                    setupSubscriber();
                    jedis.subscribe(subscriber, "PowerfulPerms");
                } catch (JedisConnectionException e) {
                    isSubscribing = false;
                    plugin.getLogger().warning("Redis connection failed: " + e.getMessage());
                } finally {
                    if (jedis != null)
                        jedis.close();
                    subscriber = null;
                    if (pool != null)
                        pool.close();
                }
            }, false);

        } catch (JedisConnectionException e) {
            isSubscribing = false;
            if (jedis != null)
                jedis.close();
            subscriber = null;
            if (pool != null)
                pool.close();
            plugin.getLogger().warning("Could not connect to your Redis server: " + e.getMessage());
        }
    }

    public Jedis getConnection() {
        if (pool == null || pool.isClosed()) {
            if (pool != null)
                pool.destroy();
            plugin.getLogger().info("Setting up Redis pool.");
            setupPool();
        }
        if (subscriber == null) {
            plugin.getLogger().info("Setting up Redis subscriber.");
            setupSubscriber();
        }
        if (subscriber != null && !subscriber.isSubscribed() && !isSubscribing)
            subscribeSubscriber();

        Jedis jedis = null;
        try {
            jedis = pool.getResource();
        } catch (JedisConnectionException e) {
            if (jedis != null)
                jedis.close();
            if (pool != null)
                pool.close();
            plugin.getLogger().warning("Could not connect to your Redis server: " + e.getMessage());
        }
        return jedis;
    }

    public void destroy() {
        if (subscriber != null)
            subscriber.unsubscribe();
        if (pool != null)
            pool.close();
        permissionManager.getScheduler().stopRepeating(taskId);
    }
}
