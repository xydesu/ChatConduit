package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.mute.MuteManager;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 禁言與解禁指令執行器 (/mute, /unmute, /mutelist)
 *
 * @author xydesu
 */
public class MuteCommand implements CommandExecutor, TabCompleter {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([sSmMhHdDyY])$");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("unmute")) {
            return handleUnmute(sender, args);
        } else if (cmdName.equals("mutelist")) {
            return handleMuteList(sender, args);
        } else {
            return handleMute(sender, args);
        }
    }

    /**
     * 處理 /mute <玩家> [時間] [原因]
     */
    public static boolean handleMute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatconduit.admin.mute")) {
            String noPerm = Main.getInstance().getLanguageConfig().getString("messages.no-permission", "<red>You do not have permission to execute this command!");
            ChatUtils.sendMessage(sender, noPerm);
            return true;
        }

        if (args.length < 1) {
            String usage = Main.getInstance().getLanguageConfig().getString("mute.mute-usage", "<red>Usage: /mute <player> [duration (e.g. 5m, 1h)] [reason]");
            ChatUtils.sendMessage(sender, usage);
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        UUID targetUuid = null;
        if (targetPlayer != null) {
            targetUuid = targetPlayer.getUniqueId();
            targetName = targetPlayer.getName();
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                targetUuid = offlinePlayer.getUniqueId();
                if (offlinePlayer.getName() != null) {
                    targetName = offlinePlayer.getName();
                }
            } else {
                // 若找不到離線玩家但有給 UUID 格式，嘗試解析
                try {
                    targetUuid = UUID.fromString(targetName);
                } catch (IllegalArgumentException e) {
                    targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes());
                }
            }
        }

        long durationMillis = -1; // 預設永久
        int reasonStartIndex = 1;

        if (args.length >= 2) {
            long parsed = parseDuration(args[1]);
            if (parsed != -2) {
                durationMillis = parsed;
                reasonStartIndex = 2;
            }
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = reasonStartIndex; i < args.length; i++) {
            if (reasonBuilder.length() > 0) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }

        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : Main.getInstance().getLanguageConfig().getString("mute.default-reason", "No reason provided");

        long expireAt = durationMillis > 0 ? System.currentTimeMillis() + durationMillis : -1;
        String senderName = sender.getName();

        MuteManager.mutePlayer(targetUuid, targetName, reason, expireAt, senderName);

        String durationStr = formatDuration(durationMillis);

        String successMsg = Main.getInstance().getLanguageConfig().getString(
                "mute.mute-success",
                "<green>Successfully muted player <yellow><player></yellow> for <yellow><time></yellow>! Reason: <gray><reason></gray>"
        ).replace("<player>", targetName).replace("<time>", durationStr).replace("<reason>", reason);

        ChatUtils.sendMessage(sender, successMsg);

        // 若被禁言玩家在線，直接向其發送通知訊息
        if (targetPlayer != null && targetPlayer.isOnline()) {
            String noticeMsg;
            if (expireAt > 0) {
                noticeMsg = Main.getInstance().getLanguageConfig().getString(
                        "mute.chat-blocked",
                        "<red>You are muted for <time> Reason: <reason>"
                ).replace("<time>", durationStr).replace("<reason>", reason);
            } else {
                noticeMsg = Main.getInstance().getLanguageConfig().getString(
                        "mute.chat-blocked-perm",
                        "<red>You are permanently muted. Reason: <reason>"
                ).replace("<reason>", reason);
            }
            ChatUtils.sendMessage(targetPlayer, noticeMsg);
        }

        return true;
    }

    /**
     * 處理 /unmute <玩家>
     */
    public static boolean handleUnmute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatconduit.admin.unmute")) {
            String noPerm = Main.getInstance().getLanguageConfig().getString("messages.no-permission", "<red>You do not have permission to execute this command!");
            ChatUtils.sendMessage(sender, noPerm);
            return true;
        }

        if (args.length < 1) {
            String usage = Main.getInstance().getLanguageConfig().getString("mute.unmute-usage", "<red>Usage: /unmute <player>");
            ChatUtils.sendMessage(sender, usage);
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        UUID targetUuid = null;
        if (targetPlayer != null) {
            targetUuid = targetPlayer.getUniqueId();
            targetName = targetPlayer.getName();
        } else {
            // 從現有 Mute 清單比對
            for (MuteManager.MuteEntry entry : MuteManager.getAllActiveMutes()) {
                if (entry.playerName().equalsIgnoreCase(targetName)) {
                    targetUuid = entry.uuid();
                    targetName = entry.playerName();
                    break;
                }
            }
            if (targetUuid == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                targetUuid = offlinePlayer.getUniqueId();
            }
        }

        if (!MuteManager.isMuted(targetUuid)) {
            String notMuted = Main.getInstance().getLanguageConfig().getString("mute.not-muted", "<red>Player <yellow><player></yellow> is not currently muted!").replace("<player>", targetName);
            ChatUtils.sendMessage(sender, notMuted);
            return true;
        }

        MuteManager.unmutePlayer(targetUuid, sender.getName());

        String successMsg = Main.getInstance().getLanguageConfig().getString("mute.unmute-success", "<green>Successfully unmuted player <yellow><player></yellow>!").replace("<player>", targetName);
        ChatUtils.sendMessage(sender, successMsg);

        if (targetPlayer != null && targetPlayer.isOnline()) {
            String notice = Main.getInstance().getLanguageConfig().getString("mute.unmuted-notice", "<green>You have been unmuted. You can now chat again.");
            ChatUtils.sendMessage(targetPlayer, notice);
        }

        return true;
    }

    /**
     * 處理 /mutelist
     */
    public static boolean handleMuteList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatconduit.admin.mutelist")) {
            String noPerm = Main.getInstance().getLanguageConfig().getString("messages.no-permission", "<red>You do not have permission to execute this command!");
            ChatUtils.sendMessage(sender, noPerm);
            return true;
        }

        List<MuteManager.MuteEntry> mutes = MuteManager.getAllActiveMutes();
        if (mutes.isEmpty()) {
            String emptyMsg = Main.getInstance().getLanguageConfig().getString("mute.list-empty", "<yellow>Currently there are no muted players.");
            ChatUtils.sendMessage(sender, emptyMsg);
            return true;
        }

        String header = Main.getInstance().getLanguageConfig().getString("mute.list-header", "<gradient:#ff416c:#ff4b2b><bold>=== 當前被禁言玩家清單 (共 <count> 人) ===</bold></gradient>").replace("<count>", String.valueOf(mutes.size()));
        ChatUtils.sendMessage(sender, header);

        for (MuteManager.MuteEntry entry : mutes) {
            String remainingStr = formatDuration(entry.getRemainingMillis());
            String line = Main.getInstance().getLanguageConfig().getString(
                    "mute.list-item",
                    "<gray>- <yellow><player></yellow> | 剩餘時間: <aqua><time></aqua> | 原因: <white><reason></white> | 執行者: <gray><by></gray>"
            ).replace("<player>", entry.playerName())
             .replace("<time>", remainingStr)
             .replace("<reason>", entry.reason())
             .replace("<by>", entry.mutedBy());
            ChatUtils.sendMessage(sender, line);
        }

        return true;
    }

    /**
     * 解析時間字串 (如 30s, 5m, 2h, 1d, 1y, perm)
     *
     * @return 毫秒數，-1 代表永久，-2 代表格式不符合
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -2;
        if (input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("forever")) {
            return -1;
        }

        Matcher matcher = TIME_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return -2;
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        return switch (unit) {
            case "s" -> value * 1000L;
            case "m" -> value * 60 * 1000L;
            case "h" -> value * 3600 * 1000L;
            case "d" -> value * 86400 * 1000L;
            case "y" -> value * 365 * 86400 * 1000L;
            default -> -2;
        };
    }

    /**
     * 格式化毫秒為易讀的時間字串
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return Main.getInstance().getLanguageConfig().getString("mute.permanent", "Permanent");
        }

        long seconds = millis / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();
        List<String> completions = new ArrayList<>();

        if (cmdName.equals("mute")) {
            if (args.length == 1) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 2) {
                List<String> times = Arrays.asList("30s", "5m", "15m", "1h", "1d", "7d", "perm");
                for (String t : times) {
                    if (t.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(t);
                    }
                }
            }
        } else if (cmdName.equals("unmute")) {
            if (args.length == 1) {
                for (MuteManager.MuteEntry entry : MuteManager.getAllActiveMutes()) {
                    if (entry.playerName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(entry.playerName());
                    }
                }
            }
        }

        return completions;
    }
}
