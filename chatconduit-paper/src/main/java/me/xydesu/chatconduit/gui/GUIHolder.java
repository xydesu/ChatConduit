package me.xydesu.chatconduit.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class GUIHolder implements InventoryHolder {

    public static final int[] SYS_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    public static final int[] CUST_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    public static int[] getSysSlots() {
        return GUIManager.getSlots(GUIManager.getConfig("channel_select"), "slots.system-channels", SYS_SLOTS);
    }

    public static int[] getCustSlots() {
        return GUIManager.getSlots(GUIManager.getConfig("channel_select"), "slots.custom-channels", CUST_SLOTS);
    }

    private final GUIType guiType;

    private final String extraData;
    private final int page;
    private Inventory inventory;

    public GUIHolder(GUIType guiType) {
        this(guiType, null, 1);
    }

    public GUIHolder(GUIType guiType, String extraData) {
        this(guiType, extraData, 1);
    }

    public GUIHolder(GUIType guiType, String extraData, int page) {
        this.guiType = guiType;
        this.extraData = extraData;
        this.page = page;
    }

    public GUIType getGuiType() {
        return guiType;
    }

    public String getExtraData() {
        return extraData;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory != null ? inventory : org.bukkit.Bukkit.createInventory(this, 54);
    }

    public enum GUIType {
        CHANNEL_SELECT,
        PLAYER_CHANNEL_MANAGE,
        PENDING_INVITES,
        ONLINE_PLAYERS_SELECT,
        CHANNEL_SETTINGS,
        MESSAGE_SETTINGS,
        CHAT_COLOR
    }
}
