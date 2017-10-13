package com.github.gustav9797.PowerfulPerms.command;

public enum CommandResult {
    /**
     * Return if the first argument(s) are correct for the command and the user has permission.
     */
    success,

    /**
     * Return if the user does not have the permission to run this command at all.
     */
    noPermission,

    /**
     * Return if this command is the wrong one for the first argument(s).
     */
    noMatch
}
