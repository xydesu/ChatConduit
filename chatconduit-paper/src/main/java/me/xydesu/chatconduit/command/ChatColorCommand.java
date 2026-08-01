package me.xydesu.chatconduit.command;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.chatcolor.ChatColorManager;
import me.xydesu.chatconduit.gui.ChatColorGUI;
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

/**
 * /chatcolor 指令執行器與 Tab 自動補全
 * 提供玩家以 GUI 圖形選單或直接指令輸入方式自訂個人預設發言聊天顏色
 *
 * @author xydesu
 */
public class ChatColorCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.parse(null, "<red>此指令僅限遊戲內玩家使用！"));
            return true;
        }

        if (!player.hasPermission("chatconduit.chatcolor")) {
            player.sendMessage(ChatUtils.parse(player, Main.getInstance().getLanguageConfig().getString("prefix", "") + "<red>您沒有權限使用聊天顏色選擇功能！"));
            return true;
        }

        if (args.length == 0) {
            ChatColorGUI.openGUI(player);
            return true;
        }

        String input = args[0].trim();
        if (input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("clear")) {
            ChatColorManager.removeChatColor(player.getUniqueId());
            player.sendMessage(ChatUtils.parse(player, Main.getInstance().getLanguageConfig().getString("prefix", "") + "<green>已成功重置聊天發言顏色！"));
            return true;
        }

        String colorCode = input;
        if (!colorCode.startsWith("&") && !colorCode.startsWith("#") && !colorCode.startsWith("<")) {
            colorCode = "&" + colorCode;
        }

        String lower = colorCode.toLowerCase();
        if (lower.startsWith("<gradient:")) {
            if (!player.hasPermission("chatconduit.chatcolor.gradient") && !player.hasPermission("chatconduit.chatcolor.vip")) {
                player.sendMessage(ChatUtils.parse(player, Main.getInstance().getLanguageConfig().getString("prefix", "") + "<red>您需要權限 <yellow>chatconduit.chatcolor.gradient<red> 才能使用漸變色！"));
                return true;
            }
        } else if (lower.contains("<rainbow>") || lower.contains("rainbow")) {
            if (!player.hasPermission("chatconduit.chatcolor.rainbow")) {
                player.sendMessage(ChatUtils.parse(player, Main.getInstance().getLanguageConfig().getString("prefix", "") + "<red>您需要權限 <yellow>chatconduit.chatcolor.rainbow<red> 才能使用彩虹色！"));
                return true;
            }
        } else if (lower.startsWith("#") || lower.startsWith("<#")) {
            if (!player.hasPermission("chatconduit.chatcolor.hex")) {
                player.sendMessage(ChatUtils.parse(player, Main.getInstance().getLanguageConfig().getString("prefix", "") + "<red>您需要權限 <yellow>chatconduit.chatcolor.hex<red> 才能使用自訂 Hex 色碼！"));
                return true;
            }
        }

        ChatColorManager.setChatColor(player.getUniqueId(), colorCode);
        player.sendMessage(ChatUtils.parse(player, Main.getInstance().getLanguageConfig().getString("prefix", "") + "<green>已將您的發言聊天顏色設定為 " + colorCode + "預設對話展示<green>！"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("reset", "clear", "&a", "&c", "&e", "&b", "&d", "&6", "&7", "&8", "&9", "&5", "&f", "&2", "&4"));
            if (sender.hasPermission("chatconduit.chatcolor.gradient")) {
                list.add("<gradient:#ff7e5f:#feb47b>");
                list.add("<gradient:#8a2387:#e94057:#f27121>");
            }
            if (sender.hasPermission("chatconduit.chatcolor.rainbow")) {
                list.add("<rainbow>");
            }
            if (sender.hasPermission("chatconduit.chatcolor.hex")) {
                list.add("#FF5555");
            }
            String input = args[0].toLowerCase();
            return list.stream().filter(s -> s.toLowerCase().startsWith(input)).toList();
        }
        return List.of();
    }
}
