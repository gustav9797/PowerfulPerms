package com.github.cheesesoftware.PowerfulPerms.common;

public interface IScheduler {
    public void runAsync(Runnable runnable);
    
    public void runSync(Runnable runnable);
}
