package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.friend.FriendManager;
import me.xydesu.chatconduit.friend.model.FriendRequest;
import me.xydesu.chatconduit.redis.RedisManager;
import me.xydesu.chatconduit.redis.RedisPlayerRegistry;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

/**
 * 好友系統主指令處理器 (/friend, /f, /friends)
 *
 * @author xydesu
 */
public class FriendCommand implements CommandExecutor, TabCompleter {

    private static final int ITEMS_PER_PAGE = 10;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "add", "accept", "deny", "remove", "list", "block", "unblock", "gui", "help"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令僅供玩家在遊戲內執行！");
            return true;
        }

        if (!player.hasPermission("chatconduit.friend")) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add" -> handleAdd(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player, args);
            case "block" -> handleBlock(player, args);
            case "unblock" -> handleUnblock(player, args);
            case "gui" -> handleGUI(player);
            case "help" -> sendHelp(player);
            default -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.usage"));
        }

        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-add"));
            return;
        }

        String targetName = args[1];
        if (targetName.equalsIgnoreCase(player.getName())) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-cannot-self"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline())) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.player-not-found")
                    .replace("<target>", targetName));
            return;
        }

        FriendManager.getInstance().sendFriendRequestAsync(player.getUniqueId(), target.getUniqueId())
                .thenAccept(result -> Bukkit.getScheduler().runTask(me.xydesu.chatconduit.Main.getInstance(), () -> {
                    switch (result) {
                        case SUCCESS -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-success")
                                .replace("<target>", targetName));
                        case COOLDOWN -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-cooldown"));
                        case BLOCKED -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-blocked"));
                        case ALREADY_FRIENDS -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-already-friends")
                                .replace("<target>", targetName));
                        case CANNOT_ADD_SELF -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-cannot-self"));
                        case REQUESTS_DISABLED -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-disabled"));
                        case AUTO_ACCEPTED -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-auto-accepted")
                                .replace("<target>", targetName));
                        case FAILED -> ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.add-failed"));
                    }
                }));
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-accept"));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.player-not-found")
                    .replace("<target>", targetName));
            return;
        }

        FriendManager.getInstance().acceptFriendRequestAsync(player.getUniqueId(), target.getUniqueId())
                .thenAccept(success -> Bukkit.getScheduler().runTask(me.xydesu.chatconduit.Main.getInstance(), () -> {
                    if (success) {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.accept-success")
                                .replace("<target>", targetName));
                    } else {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.accept-not-found")
                                .replace("<target>", targetName));
                    }
                }));
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-deny"));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.player-not-found")
                    .replace("<target>", targetName));
            return;
        }

        FriendManager.getInstance().denyFriendRequestAsync(player.getUniqueId(), target.getUniqueId())
                .thenAccept(success -> Bukkit.getScheduler().runTask(me.xydesu.chatconduit.Main.getInstance(), () -> {
                    if (success) {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.deny-success")
                                .replace("<target>", targetName));
                    } else {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.deny-not-found")
                                .replace("<target>", targetName));
                    }
                }));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-remove"));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.player-not-found")
                    .replace("<target>", targetName));
            return;
        }

        if (!FriendManager.getInstance().isFriend(player.getUniqueId(), target.getUniqueId())) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.remove-not-friend")
                    .replace("<target>", targetName));
            return;
        }

        FriendManager.getInstance().removeFriendAsync(player.getUniqueId(), target.getUniqueId())
                .thenAccept(success -> Bukkit.getScheduler().runTask(me.xydesu.chatconduit.Main.getInstance(), () -> {
                    if (success) {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.remove-success")
                                .replace("<target>", targetName));
                    } else {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.remove-not-friend")
                                .replace("<target>", targetName));
                    }
                }));
    }

    private void handleBlock(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-block"));
            return;
        }

        String targetName = args[1];
        if (targetName.equalsIgnoreCase(player.getName())) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.block-cannot-self"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.player-not-found")
                    .replace("<target>", targetName));
            return;
        }

        FriendManager.getInstance().blockPlayerAsync(player.getUniqueId(), target.getUniqueId())
                .thenAccept(success -> Bukkit.getScheduler().runTask(me.xydesu.chatconduit.Main.getInstance(), () -> {
                    if (success) {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.block-success")
                                .replace("<target>", targetName));
                    } else {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.block-failed"));
                    }
                }));
    }

    private void handleUnblock(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-unblock"));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.player-not-found")
                    .replace("<target>", targetName));
            return;
        }

        if (!FriendManager.getInstance().isBlocked(player.getUniqueId(), target.getUniqueId())) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.unblock-not-blocked")
                    .replace("<target>", targetName));
            return;
        }

        FriendManager.getInstance().unblockPlayerAsync(player.getUniqueId(), target.getUniqueId())
                .thenAccept(success -> Bukkit.getScheduler().runTask(me.xydesu.chatconduit.Main.getInstance(), () -> {
                    if (success) {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.unblock-success")
                                .replace("<target>", targetName));
                    } else {
                        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.unblock-not-blocked")
                                .replace("<target>", targetName));
                    }
                }));
    }

    private void handleList(Player player, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.list-invalid-page"));
                return;
            }
        }

        Set<UUID> friendUuids = FriendManager.getInstance().getFriends(player.getUniqueId());
        if (friendUuids.isEmpty()) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.list-empty"));
            return;
        }

        List<FriendInfo> friendList = new ArrayList<>();
        for (UUID friendUuid : friendUuids) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(friendUuid);
            String name = offP.getName() != null ? offP.getName() : friendUuid.toString().substring(0, 8);
            boolean isOnline = offP.isOnline();
            String serverName = "本服";

            if (!isOnline && RedisManager.isEnabled()) {
                RedisPlayerRegistry.PlayerData redisData = RedisPlayerRegistry.getPlayerData(name);
                if (redisData != null) {
                    isOnline = true;
                    serverName = redisData.getServerId();
                }
            } else if (isOnline) {
                serverName = "本服";
            }

            friendList.add(new FriendInfo(name, isOnline, serverName));
        }

        // 排序：在線優先，再按名稱不區分大小寫排序
        friendList.sort((a, b) -> {
            if (a.isOnline != b.isOnline) {
                return Boolean.compare(!a.isOnline, !b.isOnline);
            }
            return a.name.compareToIgnoreCase(b.name);
        });

        int totalFriends = friendList.size();
        int totalPages = (int) Math.ceil((double) totalFriends / ITEMS_PER_PAGE);

        if (page < 1 || page > totalPages) {
            ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.list-invalid-page"));
            return;
        }

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalFriends);

        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.list-header")
                .replace("<page>", String.valueOf(page))
                .replace("<total_pages>", String.valueOf(totalPages))
                .replace("<total_friends>", String.valueOf(totalFriends)));

        for (int i = startIndex; i < endIndex; i++) {
            FriendInfo info = friendList.get(i);
            if (info.isOnline) {
                ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.list-item-online")
                        .replace("<name>", info.name)
                        .replace("<server>", info.serverName));
            } else {
                ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.list-item-offline")
                        .replace("<name>", info.name));
            }
        }
    }

    private void handleGUI(Player player) {
        // 開啟好友箱子 GUI (如尚在開發生態中，預留訊息提示)
        ChatUtils.sendMessage(player, "<yellow>好友箱子 GUI 介面準備中...");
    }

    private void sendHelp(Player player) {
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-header"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-add"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-accept"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-deny"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-remove"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-list"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-block"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-unblock"));
        ChatUtils.sendMessage(player, ChatUtils.getMessage("friend.help-gui"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, matches);
            Collections.sort(matches);
            return matches;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            if (sub.equals("add") || sub.equals("block")) {
                Set<String> onlineNames = RedisPlayerRegistry.getOnlinePlayerNames();
                for (String name : onlineNames) {
                    if (!name.equalsIgnoreCase(player.getName())) {
                        completions.add(name);
                    }
                }
            } else if (sub.equals("remove") || sub.equals("unblock")) {
                Set<UUID> friendUuids = FriendManager.getInstance().getFriends(player.getUniqueId());
                for (UUID uuid : friendUuids) {
                    OfflinePlayer offP = Bukkit.getOfflinePlayer(uuid);
                    if (offP.getName() != null) {
                        completions.add(offP.getName());
                    }
                }
            } else if (sub.equals("accept") || sub.equals("deny")) {
                List<FriendRequest> requests = FriendManager.getInstance().getIncomingRequestsAsync(player.getUniqueId()).join();
                if (requests != null) {
                    for (FriendRequest req : requests) {
                        OfflinePlayer senderP = Bukkit.getOfflinePlayer(req.getSenderUuid());
                        if (senderP.getName() != null) {
                            completions.add(senderP.getName());
                        }
                    }
                }
            }

            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], completions, matches);
            Collections.sort(matches);
            return matches;
        }

        return Collections.emptyList();
    }

    private static class FriendInfo {
        private final String name;
        private final boolean isOnline;
        private final String serverName;

        public FriendInfo(String name, boolean isOnline, String serverName) {
            this.name = name;
            this.isOnline = isOnline;
            this.serverName = serverName;
        }
    }
}
