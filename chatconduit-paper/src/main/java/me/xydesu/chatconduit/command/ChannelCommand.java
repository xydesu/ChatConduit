package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.gui.ChannelSelectGUI;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChannelCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players!");
            return true;
        }

        // 無參數時直接為玩家開啟 GUI 箱子介面
        if (args.length == 0) {
            ChannelSelectGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 處理訂閱 / 取消訂閱相關子指令
        if (sub.equalsIgnoreCase("toggle") || sub.equalsIgnoreCase("subscribe") || sub.equalsIgnoreCase("unsubscribe")) {
            if (args.length < 2) {
                ChatUtils.sendMessage(player, "<yellow>用法: /" + label + " " + sub + " <頻道KEY>");
                return true;
            }
            String targetId = args[1].toLowerCase();
            ChannelManager.Channel sysChan = ChannelManager.getChannel(targetId);
            if (sysChan == null) {
                ChatUtils.sendMessage(player, getLang("channel.not-found", "<red>找不到指定的系統頻道！"));
                return true;
            }

            boolean isSubscribing;
            if (sub.equalsIgnoreCase("toggle")) {
                isSubscribing = ChannelManager.toggleChannelListening(player, targetId);
            } else if (sub.equalsIgnoreCase("subscribe")) {
                ChannelManager.setChannelListening(player.getUniqueId(), targetId, true);
                isSubscribing = true;
            } else {
                ChannelManager.setChannelListening(player.getUniqueId(), targetId, false);
                isSubscribing = false;
            }

            if (isSubscribing) {
                String subMsg = getLang("channel.subscribed", "<green>已開啟並訂閱頻道: <yellow><channel_name>")
                        .replace("<channel_name>", sysChan.name());
                ChatUtils.sendMessage(player, subMsg);
            } else {
                String unsubMsg = getLang("channel.unsubscribed", "<red>已取消訂閱頻道: <yellow><channel_name> <gray>(將不再接收此頻道訊息)")
                        .replace("<channel_name>", sysChan.name());
                ChatUtils.sendMessage(player, unsubMsg);
            }

            return true;
        }

        String targetId = sub;

        // 1. 檢查系統頻道
        ChannelManager.Channel sysChan = ChannelManager.getChannel(targetId);
        if (sysChan != null) {
            if (!sysChan.permission().isEmpty() && !player.hasPermission(sysChan.permission())) {
                ChatUtils.sendMessage(player, getLang("channel.no-permission", "<red>No permission!"));
                return true;
            }
            ChannelManager.setPlayerChannel(player, targetId);
            String switchMsg = getLang("channel.switched", "<green>Switched to: <yellow><channel_name>").replace("<channel_name>", sysChan.name());
            ChatUtils.sendMessage(player, switchMsg);
            return true;
        }

        // 2. 檢查玩家自訂頻道
        PlayerChannelManager.CustomChannel custChan = PlayerChannelManager.getChannel(targetId);
        if (custChan != null) {
            if (!custChan.getMembers().contains(player.getUniqueId())) {
                ChatUtils.sendMessage(player, getLang("channel.no-permission", "<red>You are not a member of this channel!"));
                return true;
            }
            ChannelManager.setPlayerChannel(player, custChan.getId());
            String switchMsg = getLang("channel.switched", "<green>Switched to: <yellow><channel_name>").replace("<channel_name>", custChan.getDisplayName());
            ChatUtils.sendMessage(player, switchMsg);
            return true;
        }

        ChatUtils.sendMessage(player, getLang("channel.not-found", "<red>Channel not found!"));
        return true;
    }

    private String getLang(String path, String def) {
        return Main.getInstance().getLanguageConfig().getString(path, def);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player player) {
            List<String> options = List.of("toggle", "subscribe", "unsubscribe");
            for (String opt : options) {
                if (opt.startsWith(args[0].toLowerCase())) {
                    completions.add(opt);
                }
            }
            for (ChannelManager.Channel sys : ChannelManager.getChannels().values()) {
                if (sys.permission().isEmpty() || player.hasPermission(sys.permission())) {
                    if (sys.key().startsWith(args[0].toLowerCase())) {
                        completions.add(sys.key());
                    }
                }
            }
            for (PlayerChannelManager.CustomChannel c : PlayerChannelManager.getCustomChannels().values()) {
                if (c.getMembers().contains(player.getUniqueId()) && c.getId().startsWith(args[0].toLowerCase())) {
                    completions.add(c.getId());
                }
            }
        } else if (args.length == 2 && sender instanceof Player player) {
            String sub = args[0].toLowerCase();
            if (sub.equals("toggle") || sub.equals("subscribe") || sub.equals("unsubscribe")) {
                for (ChannelManager.Channel sys : ChannelManager.getChannels().values()) {
                    if (sys.permission().isEmpty() || player.hasPermission(sys.permission())) {
                        if (sys.key().startsWith(args[1].toLowerCase())) {
                            completions.add(sys.key());
                        }
                    }
                }
            }
        }
        return completions;
    }
}
