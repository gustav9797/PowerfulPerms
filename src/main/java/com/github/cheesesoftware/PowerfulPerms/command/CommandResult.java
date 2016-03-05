package com.github.cheesesoftware.PowerfulPerms.command;

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
    noMatch,

    /**
     * Return if this command is the correct one and the user has permission but the arguments are not correct.
     */
    showUsage,

    /**
     * Return if not used.
     */
    NONE
}
