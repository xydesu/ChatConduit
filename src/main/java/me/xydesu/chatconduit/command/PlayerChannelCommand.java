package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.gui.ChannelSelectGUI;
import me.xydesu.chatconduit.gui.PlayerChannelManageGUI;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerChannelCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by players!");
            return true;
        }

        if (args.length == 0) {
            PlayerChannelManageGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String currentKey = ChannelManager.getPlayerSelectedKey(player);
        PlayerChannelManager.CustomChannel myChan = PlayerChannelManager.getChannel(currentKey);

        switch (sub) {
            case "gui":
                PlayerChannelManageGUI.open(player);
                break;

            // /pc switch <頻道ID/名稱> 或 /pc join <頻道ID/名稱>
            case "switch":
            case "join":
                if (args.length < 2) {
                    ChannelSelectGUI.open(player);
                    return true;
                }
                String targetKey = args[1].toLowerCase();

                // 檢查是否為系統頻道
                ChannelManager.Channel sysChan = ChannelManager.getChannel(targetKey);
                if (sysChan != null) {
                    ChannelManager.setPlayerChannel(player, sysChan.key());
                    ChatUtils.sendMessage(player, "<green>已切換預設發言頻道至：<yellow>" + sysChan.name() + "</yellow>");
                    return true;
                }

                // 檢查是否為玩家群組頻道
                PlayerChannelManager.CustomChannel custTarget = PlayerChannelManager.getChannel(targetKey);
                if (custTarget != null) {
                    boolean isMember = custTarget.getMembers().contains(player.getUniqueId());
                    boolean isPublic = custTarget.getMode() == PlayerChannelManager.Mode.PUBLIC;

                    if (!isMember) {
                        if (isPublic) {
                            custTarget.getMembers().add(player.getUniqueId());
                            PlayerChannelManager.save();
                            ChatUtils.sendMessage(player, "<green>已成功加入公開頻道 <yellow>" + custTarget.getDisplayName() + "</yellow>！");
                            PlayerChannelManager.broadcastToMembers(custTarget, "<green>▶ 玩家 <yellow>" + player.getName() + "</yellow> 已加入公開頻道 <yellow>" + custTarget.getDisplayName() + "</yellow>！", player.getUniqueId());
                        } else {
                            ChatUtils.sendMessage(player, "<red>你不是該私人群組頻道的成員，且尚未收到邀請！");
                            return true;
                        }
                    }

                    ChannelManager.setPlayerChannel(player, custTarget.getId());
                    ChatUtils.sendMessage(player, "<green>已切換預設發言頻道至：<yellow>" + custTarget.getDisplayName() + "</yellow>");
                    return true;
                }

                ChatUtils.sendMessage(player, "<red>找不到頻道 <yellow>" + args[1] + "</yellow>！");
                break;

            // /pc create <名稱>
            case "create":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, getLang("channel.create-usage", "<red>Usage: /playerchannel create <Name>"));
                    return true;
                }
                if (!player.hasPermission("chatconduit.create")) {
                    ChatUtils.sendMessage(player, getLang("messages.no-permission", "<red>You do not have permission to execute this command!"));
                    return true;
                }
                String name = args[1];
                PlayerChannelManager.CreateResult res = PlayerChannelManager.tryCreateChannel(name, player);
                switch (res) {
                    case SUCCESS -> {
                        String msg = getLang("channel.create-success", "<green>Successfully created channel <yellow><name>!").replace("<name>", name);
                        ChatUtils.sendMessage(player, msg);
                        ChannelManager.setPlayerChannel(player, name.toLowerCase());
                    }
                    case RESERVED_KEYWORD -> {
                        ChatUtils.sendMessage(player, getLang("channel.name-blacklisted", "<red>This channel name contains a reserved keyword!"));
                    }
                    case LIMIT_REACHED -> {
                        int max = Main.getInstance().getConfig().getInt("player-channels.max-per-player", 3);
                        ChatUtils.sendMessage(player, getLang("channel.create-limit-reached", "<red>You have reached the channel limit!").replace("<limit>", String.valueOf(max)));
                    }
                    case ALREADY_EXISTS -> {
                        ChatUtils.sendMessage(player, getLang("channel.create-exists", "<red>A channel with this name already exists!"));
                    }
                    default -> {
                        ChatUtils.sendMessage(player, getLang("channel.name-invalid", "<red>Invalid channel name!"));
                    }
                }
                break;


            // /pc invite <玩家>
            case "invite":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, getLang("channel.invite-usage", "<red>Usage: /playerchannel invite <Player>"));
                    return true;
                }
                if (myChan == null || !myChan.getOwner().equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, getLang("channel.invite-not-owner", "<red>You must be the owner of this channel!"));
                    return true;
                }
                String targetName = args[1];
                Player targetInvite = Bukkit.getPlayer(targetName);

                if (targetInvite != null && targetInvite.isOnline()) {
                    myChan.getPendingInvites().add(targetInvite.getUniqueId());
                    PlayerChannelManager.saveChannel(myChan);

                    String sentMsg = getLang("channel.invite-sent", "<green>Invite sent to <yellow><player>.").replace("<player>", targetInvite.getName());
                    ChatUtils.sendMessage(player, sentMsg);
                    ChatUtils.sendInviteNotification(player, targetInvite, myChan);
                } else if (me.xydesu.chatconduit.redis.RedisManager.isEnabled()) {
                    // 嘗試紀錄離線玩家 UUID (若本服存在該玩家快取)
                    org.bukkit.OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(targetName);
                    if (offPlayer != null && offPlayer.getUniqueId() != null) {
                        myChan.getPendingInvites().add(offPlayer.getUniqueId());
                        PlayerChannelManager.saveChannel(myChan);
                    }

                    // 若本地找不到線上玩家，透過 Redis 發送跨服廣播邀請
                    me.xydesu.chatconduit.redis.ChannelInvitePacket invitePacket = new me.xydesu.chatconduit.redis.ChannelInvitePacket(
                            me.xydesu.chatconduit.redis.ChannelInvitePacket.Action.INVITE,
                            player.getUniqueId().toString(),
                            player.getName(),
                            targetName,
                            myChan.getId(),
                            myChan.getDisplayName(),
                            me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                            System.currentTimeMillis()
                    );
                    me.xydesu.chatconduit.redis.RedisManager.publishInvitePacket(invitePacket);

                    ChatUtils.sendMessage(player, "<green>已嘗試透過 Redis 跨服廣播發送頻道邀請給玩家 <yellow>" + targetName + "</yellow>！");
                } else {
                    ChatUtils.sendMessage(player, getLang("channel.invite-player-not-found", "<red>Player not found!"));
                }
                break;

            // /pc accept <名稱/ID>
            case "accept":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, getLang("channel.accept-usage", "<red>Usage: /playerchannel accept <Name>"));
                    return true;
                }
                PlayerChannelManager.CustomChannel invChan = PlayerChannelManager.getOrLoadChannel(args[1]);
                if (invChan == null || !invChan.getPendingInvites().contains(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, getLang("channel.accept-no-invite", "<red>No pending invite for this channel!"));
                    return true;
                }
                invChan.getPendingInvites().remove(player.getUniqueId());
                invChan.getMembers().add(player.getUniqueId());
                PlayerChannelManager.saveChannel(invChan);

                String acceptMsg = getLang("channel.accept-success", "<green>Joined <yellow><name>!").replace("<name>", invChan.getDisplayName());
                ChatUtils.sendMessage(player, acceptMsg);
                PlayerChannelManager.broadcastToMembers(invChan, "<green>▶ 玩家 <yellow>" + player.getName() + "</yellow> 已加入群組頻道 <yellow>" + invChan.getDisplayName() + "</yellow>！", player.getUniqueId());
                ChannelManager.setPlayerChannel(player, invChan.getId());

                // 跨服廣播成員加入狀態同步
                if (me.xydesu.chatconduit.redis.RedisManager.isEnabled()) {
                    me.xydesu.chatconduit.redis.ChannelInvitePacket acceptPacket = new me.xydesu.chatconduit.redis.ChannelInvitePacket(
                            me.xydesu.chatconduit.redis.ChannelInvitePacket.Action.ACCEPT,
                            player.getUniqueId().toString(),
                            player.getName(),
                            "",
                            invChan.getId(),
                            invChan.getDisplayName(),
                            me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                            System.currentTimeMillis()
                    );
                    me.xydesu.chatconduit.redis.RedisManager.publishInvitePacket(acceptPacket);
                }
                break;

            // /pc deny <名稱/ID>
            case "deny":
            case "reject":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, "<red>用法: /playerchannel deny <頻道名稱/ID>");
                    return true;
                }
                PlayerChannelManager.CustomChannel rejectChan = PlayerChannelManager.getOrLoadChannel(args[1]);
                if (rejectChan == null || !rejectChan.getPendingInvites().contains(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, "<red>你沒有來自該頻道的待處理邀請！");
                    return true;
                }
                rejectChan.getPendingInvites().remove(player.getUniqueId());
                PlayerChannelManager.saveChannel(rejectChan);
                ChatUtils.sendMessage(player, "<gray>已成功拒絕頻道 <yellow>" + rejectChan.getDisplayName() + "</yellow> 的邀請。");

                // 跨服廣播拒絕通知
                if (me.xydesu.chatconduit.redis.RedisManager.isEnabled()) {
                    me.xydesu.chatconduit.redis.ChannelInvitePacket rejectPacket = new me.xydesu.chatconduit.redis.ChannelInvitePacket(
                            me.xydesu.chatconduit.redis.ChannelInvitePacket.Action.REJECT,
                            player.getUniqueId().toString(),
                            player.getName(),
                            player.getName(),
                            rejectChan.getId(),
                            rejectChan.getDisplayName(),
                            me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                            System.currentTimeMillis()
                    );
                    me.xydesu.chatconduit.redis.RedisManager.publishInvitePacket(rejectPacket);
                }
                break;

            // /pc leave
            case "leave":
                if (myChan == null) {
                    ChatUtils.sendMessage(player, "<red>You are not currently in a custom channel!");
                    return true;
                }
                if (myChan.getOwner().equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, "<red>As the owner, you cannot leave! Use /pc manage delete or transfer.");
                    return true;
                }
                myChan.getMembers().remove(player.getUniqueId());
                PlayerChannelManager.save();
                ChannelManager.setPlayerChannel(player, "global");
                ChatUtils.sendMessage(player, "<green>You left channel <yellow>" + myChan.getDisplayName() + "<green>.");
                PlayerChannelManager.broadcastToMembers(myChan, "<red>🚪 玩家 <yellow>" + player.getName() + "</yellow> 已退出群組頻道 <yellow>" + myChan.getDisplayName() + "</yellow>。</red>", player.getUniqueId());
                break;

            // /pc members
            case "members":
                if (myChan == null) {
                    ChatUtils.sendMessage(player, "<red>You are not currently in a custom channel!");
                    return true;
                }
                ChatUtils.sendMessage(player, "<green>=== Members of " + myChan.getDisplayName() + " ===");
                for (UUID mUuid : myChan.getMembers()) {
                    OfflinePlayer mPlayer = Bukkit.getOfflinePlayer(mUuid);
                    String role = mUuid.equals(myChan.getOwner()) ? "<gold>[Owner] " : "<gray>[Member] ";
                    ChatUtils.sendMessage(player, role + "<white>" + (mPlayer.getName() != null ? mPlayer.getName() : mUuid.toString()));
                }
                break;

            // 二級管理指令：/pc manage <kick|transfer|delete>
            case "manage":
                if (args.length < 2) {
                    PlayerChannelManageGUI.open(player);
                    return true;
                }
                if (myChan == null || !myChan.getOwner().equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, getLang("channel.invite-not-owner", "<red>You must be the owner of this channel!"));
                    return true;
                }
                handleManageCommand(player, myChan, args);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleManageCommand(Player player, PlayerChannelManager.CustomChannel myChan, String[] args) {
        String action = args[1].toLowerCase();

        switch (action) {
            // /pc manage kick <玩家>
            case "kick":
                if (args.length < 3) {
                    ChatUtils.sendMessage(player, "<red>Usage: /playerchannel manage kick <Player>");
                    return;
                }
                String targetName = args[2];
                Player onlineKickTarget = Bukkit.getPlayer(targetName);
                UUID targetUuid = onlineKickTarget != null ? onlineKickTarget.getUniqueId() : null;

                if (targetUuid == null) {
                    for (UUID memberUuid : myChan.getMembers()) {
                        OfflinePlayer offP = Bukkit.getOfflinePlayer(memberUuid);
                        if (offP.getName() != null && offP.getName().equalsIgnoreCase(targetName)) {
                            targetUuid = memberUuid;
                            break;
                        }
                    }
                }

                if (targetUuid == null || !myChan.getMembers().contains(targetUuid)) {
                    ChatUtils.sendMessage(player, "<red>This player is not in your channel!");
                    return;
                }
                if (targetUuid.equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, "<red>You cannot kick yourself!");
                    return;
                }

                myChan.getMembers().remove(targetUuid);
                PlayerChannelManager.save();
                ChatUtils.sendMessage(player, "<green>Kicked <yellow>" + targetName + " <green>from channel.");

                if (onlineKickTarget != null && onlineKickTarget.isOnline()) {
                    ChatUtils.sendMessage(onlineKickTarget, "<red>You have been kicked from channel <yellow>" + myChan.getDisplayName() + "<red>.");
                    if (ChannelManager.getPlayerSelectedKey(onlineKickTarget).equalsIgnoreCase(myChan.getId())) {
                        ChannelManager.setPlayerChannel(onlineKickTarget, "global");
                    }
                }
                break;

            // /pc manage transfer <玩家>
            case "transfer":
                if (args.length < 3) {
                    ChatUtils.sendMessage(player, "<red>Usage: /playerchannel manage transfer <Player>");
                    return;
                }
                Player targetTransfer = Bukkit.getPlayer(args[2]);
                if (targetTransfer == null || !myChan.getMembers().contains(targetTransfer.getUniqueId())) {
                    ChatUtils.sendMessage(player, "<red>Target player must be online and a member of this channel!");
                    return;
                }
                if (targetTransfer.getUniqueId().equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, "<red>You are already the owner of this channel!");
                    return;
                }
                myChan.setOwner(targetTransfer.getUniqueId());
                PlayerChannelManager.save();
                ChatUtils.sendMessage(player, "<green>Transferred channel ownership to <yellow>" + targetTransfer.getName() + "<green>.");
                ChatUtils.sendMessage(targetTransfer, "<green>You are now the owner of channel <yellow>" + myChan.getDisplayName() + "<green>!");
                break;

            // /pc manage delete
            case "delete":
                String delName = myChan.getDisplayName();
                PlayerChannelManager.deleteChannel(myChan.getId());
                ChatUtils.sendMessage(player, "<red>Successfully deleted channel <yellow>" + delName + "<red>.");
                break;

            default:
                ChatUtils.sendMessage(player, "<red>Unknown manage action! Use kick, transfer, or delete.");
                break;
        }
    }

    private void sendHelp(Player p) {
        ChatUtils.sendMessage(p, "<green>=== Player Channel Management (/pc) ===");
        ChatUtils.sendMessage(p, "<yellow>/pc <gray>- Open channel management GUI");
        ChatUtils.sendMessage(p, "<yellow>/pc create <Name> <gray>- Create a channel");
        ChatUtils.sendMessage(p, "<yellow>/pc invite <Player> <gray>- Invite player");
        ChatUtils.sendMessage(p, "<yellow>/pc accept <Name> <gray>- Accept invite");
        ChatUtils.sendMessage(p, "<yellow>/pc leave <gray>- Leave current channel");
        ChatUtils.sendMessage(p, "<yellow>/pc members <gray>- List members");
        ChatUtils.sendMessage(p, "<yellow>/pc manage kick <Player> <gray>- Kick member");
        ChatUtils.sendMessage(p, "<yellow>/pc manage transfer <Player> <gray>- Transfer ownership");
        ChatUtils.sendMessage(p, "<yellow>/pc manage delete <gray>- Delete channel");
    }

    private String getLang(String path, String def) {
        return Main.getInstance().getLanguageConfig().getString(path, def);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender instanceof Player player) {
            if (args.length == 1) {
                completions.addAll(List.of("gui", "create", "invite", "accept", "leave", "members", "manage"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("manage")) {
                completions.addAll(List.of("kick", "transfer", "delete"));
            }
        }
        return completions;
    }
}
