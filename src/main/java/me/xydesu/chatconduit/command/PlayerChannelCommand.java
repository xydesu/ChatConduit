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

            // /pc create <名稱>
            case "create":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, getLang("channel.create-usage", "<red>Usage: /playerchannel create <Name>"));
                    return true;
                }
                String name = args[1];
                if (PlayerChannelManager.createChannel(name, player)) {
                    String msg = getLang("channel.create-success", "<green>Successfully created channel <yellow><name>!")
                            .replace("<name>", name);
                    ChatUtils.sendMessage(player, msg);
                    ChannelManager.setPlayerChannel(player, name.toLowerCase());
                } else {
                    ChatUtils.sendMessage(player, getLang("channel.create-exists", "<red>A channel with this name already exists!"));
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
                Player targetInvite = Bukkit.getPlayer(args[1]);
                if (targetInvite == null) {
                    ChatUtils.sendMessage(player, getLang("channel.invite-player-not-found", "<red>Player not found!"));
                    return true;
                }
                myChan.getPendingInvites().add(targetInvite.getUniqueId());
                PlayerChannelManager.save();

                String sentMsg = getLang("channel.invite-sent", "<green>Invite sent to <yellow><player>.").replace("<player>", targetInvite.getName());
                ChatUtils.sendMessage(player, sentMsg);

                ChatUtils.sendInviteNotification(player, targetInvite, myChan);
                break;

            // /pc accept <名稱/ID>
            case "accept":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, getLang("channel.accept-usage", "<red>Usage: /playerchannel accept <Name>"));
                    return true;
                }
                PlayerChannelManager.CustomChannel invChan = PlayerChannelManager.getChannel(args[1]);
                if (invChan == null || !invChan.getPendingInvites().contains(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, getLang("channel.accept-no-invite", "<red>No pending invite for this channel!"));
                    return true;
                }
                invChan.getPendingInvites().remove(player.getUniqueId());
                invChan.getMembers().add(player.getUniqueId());
                PlayerChannelManager.save();

                String acceptMsg = getLang("channel.accept-success", "<green>Joined <yellow><name>!").replace("<name>", invChan.getDisplayName());
                ChatUtils.sendMessage(player, acceptMsg);
                ChannelManager.setPlayerChannel(player, invChan.getId());
                break;

            // /pc deny <名稱/ID>
            case "deny":
            case "reject":
                if (args.length < 2) {
                    ChatUtils.sendMessage(player, "<red>用法: /playerchannel deny <頻道名稱/ID>");
                    return true;
                }
                PlayerChannelManager.CustomChannel rejectChan = PlayerChannelManager.getChannel(args[1]);
                if (rejectChan == null || !rejectChan.getPendingInvites().contains(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, "<red>你沒有來自該頻道的待處理邀請！");
                    return true;
                }
                rejectChan.getPendingInvites().remove(player.getUniqueId());
                PlayerChannelManager.save();
                ChatUtils.sendMessage(player, "<gray>已成功拒絕頻道 <yellow>" + rejectChan.getDisplayName() + "</yellow> 的邀請。");
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
