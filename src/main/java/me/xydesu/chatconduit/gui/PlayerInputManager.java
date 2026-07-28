package me.xydesu.chatconduit.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInputManager implements Listener {

    private static final Map<UUID, InputSession> pendingInputs = new ConcurrentHashMap<>();
    private static final Set<UUID> currentlyProcessing = ConcurrentHashMap.newKeySet();

    public enum InputType {
        CREATE_CHANNEL,
        RENAME_CHANNEL
    }

    public record InputSession(InputType type, String extraData) {}

    public static boolean isInputPending(UUID uuid) {
        return pendingInputs.containsKey(uuid) || currentlyProcessing.contains(uuid);
    }

    public static void expectInput(Player player, InputType type) {
        expectInput(player, type, null);
    }

    public static void expectInput(Player player, InputType type, String extraData) {
        pendingInputs.put(player.getUniqueId(), new InputSession(type, extraData));
        player.closeInventory();

        ChatUtils.sendMessage(player, "");
        if (type == InputType.CREATE_CHANNEL) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 建立頻道對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框直接輸入新頻道名稱 <gray>(輸入 cancel 可取消)：");
        } else if (type == InputType.RENAME_CHANNEL) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 重命名頻道對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框輸入新的頻道顯示名稱 <gray>(輸入 cancel 可取消)：");
        }
        ChatUtils.sendMessage(player, "");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChatInput(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        InputSession session = pendingInputs.get(uuid);
        if (session == null) return;

        currentlyProcessing.add(uuid);
        pendingInputs.remove(uuid);

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                if (input.equalsIgnoreCase("cancel")) {
                    ChatUtils.sendMessage(player, "<gray>已取消對話框輸入。");
                    ChannelSelectGUI.open(player);
                    return;
                }

                if (session.type() == InputType.CREATE_CHANNEL) {
                    if (input.isEmpty() || input.length() > 20) {
                        ChatUtils.sendMessage(player, "<red>頻道名稱長度必須介於 1 至 20 個字元！");
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (PlayerChannelManager.createChannel(input, player)) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.create-success", "<green>成功建立群組頻道 <yellow><name>！").replace("<name>", input);
                        ChatUtils.sendMessage(player, msg);
                        ChannelManager.setPlayerChannel(player, input.toLowerCase());

                        PlayerChannelManager.CustomChannel newChan = PlayerChannelManager.getChannel(input.toLowerCase());
                        if (newChan != null) {
                            PlayerChannelManageGUI.openForChannel(player, newChan);
                        } else {
                            ChannelSelectGUI.open(player);
                        }
                    } else {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.create-exists", "<red>該頻道名稱已存在！");
                        ChatUtils.sendMessage(player, msg);
                        ChannelSelectGUI.open(player);
                    }
                } else if (session.type() == InputType.RENAME_CHANNEL) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (input.isEmpty() || input.length() > 20) {
                        ChatUtils.sendMessage(player, "<red>頻道顯示名稱長度必須介於 1 至 20 個字元！");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setDisplayName(input);
                    PlayerChannelManager.save();
                    ChatUtils.sendMessage(player, "<green>已成功將頻道顯示名稱修改為：<yellow>" + input + "</yellow>！");
                    ChannelSettingsGUI.open(player, customChan);
                }
            } finally {
                currentlyProcessing.remove(uuid);
            }
        });
    }
}
