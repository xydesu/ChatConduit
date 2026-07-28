package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChatConduitCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;

            case "help":
                sendHelpMessage(sender);
                break;

            default:
                String unknownMsg = getLang("messages.unknown-command", "<red>Unknown command, please use <yellow>/chatconduit help</yellow>.");
                ChatUtils.sendMessage(sender, unknownMsg);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("chatconduit.admin")) {
            String noPermMsg = getLang("messages.no-permission", "<red>You do not have permission to execute this command!");
            ChatUtils.sendMessage(sender, noPermMsg);
            return;
        }

        Main.getInstance().reloadPluginConfigs();

        String successMsg = getLang("messages.reload-success", "<green>Configuration and language files reloaded successfully!");
        ChatUtils.sendMessage(sender, successMsg);
    }

    private void sendHelpMessage(CommandSender sender) {
        ChatUtils.sendMessage(sender, getLang("help.header", "<green>=== ChatConduit Help ==="));
        ChatUtils.sendMessage(sender, getLang("help.help-cmd", "<yellow>/chatconduit help <gray>- Show help menu"));
        ChatUtils.sendMessage(sender, getLang("help.reload-cmd", "<yellow>/chatconduit reload <gray>- Reload configuration"));
    }

    private String getLang(String path, String def) {
        return Main.getInstance().getLanguageConfig().getString(path, def);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("help", "reload");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    if (sub.equals("reload") && !sender.hasPermission("chatconduit.admin")) {
                        continue;
                    }
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
