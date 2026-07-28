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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInputManager implements Listener {

    private static final Map<UUID, InputType> pendingInputs = new ConcurrentHashMap<>();

    public enum InputType {
        CREATE_CHANNEL
    }

    public static void expectInput(Player player, InputType type) {
        pendingInputs.put(player.getUniqueId(), type);
        player.closeInventory();

        ChatUtils.sendMessage(player, "");
        ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 建立頻道對話框提示 ===</bold></gradient>");
        ChatUtils.sendMessage(player, "<yellow>請在對話框直接輸入新頻道名稱 <gray>(輸入 cancel 可取消)：");
        ChatUtils.sendMessage(player, "");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChatInput(AsyncChatEvent event) {
        Player player = event.getPlayer();
        InputType type = pendingInputs.remove(player.getUniqueId());
        if (type == null) return;

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (input.equalsIgnoreCase("cancel")) {
                ChatUtils.sendMessage(player, "<gray>已取消建立頻道。");
                ChannelSelectGUI.open(player);
                return;
            }

            if (type == InputType.CREATE_CHANNEL) {
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
            }
        });
    }
}
