package com.github.cheesesoftware.PowerfulPerms.common;

public interface ICommand {
    public void sendSender(String sender, String reply);

    public String getVersion();

    public boolean hasPermission(String name, String permission);
}
