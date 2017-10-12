package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.ChatColor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class BaseCommand extends SubCommand {

    public BaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);

        subCommands.add(new UserBaseCommand(plugin, permissionManager));
        subCommands.add(new ShowGroupsCommand(plugin, permissionManager));
        subCommands.add(new GroupBaseCommand(plugin, permissionManager));
        subCommands.add(new ShowLaddersCommand(plugin, permissionManager));
        subCommands.add(new HasPermissionCommand(plugin, permissionManager));
        subCommands.add(new ReloadCommand(plugin, permissionManager));
        subCommands.add(new TestBaseCommand(plugin, permissionManager));
        subCommands.add(new AboutCommand(plugin, permissionManager));
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        plugin.getPermissionManager().getExecutor().submit(() -> {
            String[] argsTemp = resolveArgs(args);
            resolveSequences(invoker, sender, argsTemp);
        });
        return CommandResult.success;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        List<String> output = new ArrayList<>();
        for (SubCommand subCommand : subCommands) {
            List<String> out = subCommand.tabComplete(invoker, sender, args);
            if (out != null)
                output.addAll(out);
        }
        return output;
    }

    private void continueExecute(ICommand invoker, String sender, String[] args) {
        boolean hasSomePermission = false;
        for (SubCommand subCommand : subCommands) {
            CommandResult result;
            try {
                result = subCommand.execute(invoker, sender, args);
                if (result == CommandResult.success)
                    return;
                else if (result == CommandResult.noMatch)
                    hasSomePermission = true;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (hasSomePermission) {
            sendSender(invoker, sender, getUsage());
        } else {
            sendSender(invoker, sender, "You do not have permission.");
        }
    }

    @Override
    public List<String> getUsage() {
        List<String> usage = new ArrayList<>();
        usage.add(ChatColor.RED + "~ " + ChatColor.BLUE + "PowerfulPerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
        for (SubCommand subCommand : subCommands)
            usage.addAll(subCommand.getUsage());
        return usage;
    }

    public static String[] resolveArgs(String[] args) {
        List<String> output = new ArrayList<>();
        String current = "";
        boolean adding = false;

        for (int i = 0; i < args.length; ++i) {
            String s = args[i];

            if (!adding) {
                current = "";

                if (!s.contains("\"")) {
                    output.add(s);
                    continue;
                }
            }

            for (char c : s.toCharArray()) {
                if (c == '"' || c == '\'') {
                    adding = !adding;
                } else
                    current += c;
            }

            if ((!adding || (adding && i == args.length - 1)) && !current.isEmpty())
                output.add(current);
            else if (adding)
                current += " ";

        }
        return output.toArray(new String[output.size()]);
    }

    /*
     * Runs command for every sequence in command.
     */
    private void resolveSequences(ICommand invoker, String sender, String[] args) {
        int beginIndex = -1;
        int endIndex = -1;
        for (int a = 0; a < args.length; ++a) {
            beginIndex = -1;
            endIndex = -1;
            String arg = args[a];
            char[] chars = arg.toCharArray();
            for (int i = 0; i < chars.length; ++i) {
                if (beginIndex == -1 && chars[i] == '{')
                    beginIndex = i;
                if (endIndex == -1 && beginIndex != -1 && chars[i] == '}') {
                    endIndex = i;
                    // Found sequence
                    String sequence = arg.substring(beginIndex + 1, endIndex);
                    String[] sequenceList = sequence.split(",");
                    for (String s : sequenceList) {
                        StringBuilder builder = new StringBuilder(arg);
                        builder.replace(beginIndex, endIndex + 1, s);
                        String[] temp = args.clone();
                        temp[a] = builder.toString();
                        resolveSequences(invoker, sender, temp);
                    }
                    return;
                }
            }
        }

        // Didn't find any more sequence
        continueExecute(invoker, sender, args);
    }

}
