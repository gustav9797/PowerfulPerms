package com.github.gustav9797.PowerfulPermsAPI;

import java.util.Date;

public interface IScheduler {
    public void runAsync(Runnable runnable, boolean sameThread);

    /*
     * Note: Bungee can't run sync tasks, all of them are asynchronous.
     */
    public void runSync(Runnable runnable, boolean sameThread);

    /*
     * Note: Bungee can't run sync tasks, all of them are asynchronous.
     */
    public void runSync(Runnable runnable);

    public int runRepeating(Runnable runnable, int seconds);

    public void stopRepeating(int taskId);

    public int runDelayed(Runnable runnable, Date when);

    public int runDelayed(Runnable runnable, long seconds);
}
