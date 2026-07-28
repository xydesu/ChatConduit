package me.xydesu.chatconduit.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class GUIHolder implements InventoryHolder {

    private final GUIType guiType;
    private final String extraData;

    public GUIHolder(GUIType guiType) {
        this(guiType, null);
    }

    public GUIHolder(GUIType guiType, String extraData) {
        this.guiType = guiType;
        this.extraData = extraData;
    }

    public GUIType getGuiType() {
        return guiType;
    }

    public String getExtraData() {
        return extraData;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public enum GUIType {
        CHANNEL_SELECT,
        PLAYER_CHANNEL_MANAGE,
        PENDING_INVITES,
        ONLINE_PLAYERS_SELECT,
        CHANNEL_SETTINGS
    }
}
