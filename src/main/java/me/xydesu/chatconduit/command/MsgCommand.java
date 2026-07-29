package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.message.PrivateMessageManager;
import me.xydesu.chatconduit.redis.RedisPlayerRegistry;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 跨服與本地私訊 (/msg, /tell, /w, /pm, /whisper) 與快速回覆 (/reply, /r) 指令處理器
 *
 * @author xydesu
 */
public class MsgCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此指令僅供玩家在遊戲內執行！");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        boolean isReply = cmdName.equals("reply") || label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply");

        if (isReply) {
            if (args.length < 1) {
                ChatUtils.sendMessage(player, ChatUtils.getMessage("msg.reply-usage"));
                return true;
            }

            String replyTarget = PrivateMessageManager.getReplyTarget(player.getUniqueId());
            if (replyTarget == null || replyTarget.isEmpty()) {
                ChatUtils.sendMessage(player, ChatUtils.getMessage("msg.no-reply-target"));
                return true;
            }

            String message = String.join(" ", args);
            PrivateMessageManager.sendPrivateMessage(player, replyTarget, message);
            return true;
        } else {
            if (args.length < 2) {
                ChatUtils.sendMessage(player, ChatUtils.getMessage("msg.usage"));
                return true;
            }

            String targetName = args[0];
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            PrivateMessageManager.sendPrivateMessage(player, targetName, message);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();
        boolean isReply = cmdName.equals("reply") || alias.equalsIgnoreCase("r") || alias.equalsIgnoreCase("reply");

        if (!isReply && args.length == 1) {
            Set<String> onlineNames = RedisPlayerRegistry.getOnlinePlayerNames();
            List<String> completions = new ArrayList<>();

            String current = args[0];
            for (String name : onlineNames) {
                if (sender instanceof Player player && name.equalsIgnoreCase(player.getName())) {
                    continue;
                }
                completions.add(name);
            }

            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(current, completions, matches);
            Collections.sort(matches);
            return matches;
        }

        return Collections.emptyList();
    }
}
