package me.xydesu.chatconduit.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * ChatConduit Velocity 代理端主插件類別
 * 與 InteractiveChat Velocity 代理插件協同進行全域跨服廣播與頻道維護
 *
 * @author xydesu
 */
@Plugin(
        id = "chatconduit",
        name = "ChatConduit",
        version = "1.0",
        description = "Velocity Proxy Multi-Server Chat Plugin",
        authors = {"xydesu"}
)
public class ChatConduitVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public ChatConduitVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("ChatConduit Velocity 代理端插件已成功載入！");
        logger.info("全域代理模式已開啟，將與 InteractiveChat Velocity 代理插件協同運作。");
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        // 讓 PlayerChatEvent 在 Velocity 代理層發揮作用，配合 InteractiveChat Velocity 進行全域同步
        if (!event.getResult().isAllowed()) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        // 可以在代理層進行日誌輸出或全域處理
        logger.debug("[ProxyChat] " + event.getPlayer().getUsername() + ": " + message);
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
