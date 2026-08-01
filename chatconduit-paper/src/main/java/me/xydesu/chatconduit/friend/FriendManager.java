package me.xydesu.chatconduit.friend;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.dao.FriendBlockDAO;
import me.xydesu.chatconduit.database.dao.FriendDAO;
import me.xydesu.chatconduit.database.dao.FriendRequestDAO;
import me.xydesu.chatconduit.database.dao.PlayerSettingsDAO;
import me.xydesu.chatconduit.friend.model.FriendRequest;
import me.xydesu.chatconduit.friend.model.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 好友系統核心管理器，負責記憶體快取、異步 SQL 讀寫與防刷 CD 機制
 *
 * @author xydesu
 */
public class FriendManager {

    private static FriendManager instance;

    // 快取資料（在線玩家）
    private final Map<UUID, Set<UUID>> friendCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> blockCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();

    // 好友申請冷卻控制（防止連續刷申請，單位：毫秒）
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    private static final long DEFAULT_COOLDOWN_MS = 5000L;

    public FriendManager() {
        instance = this;
    }

    public static FriendManager getInstance() {
        return instance;
    }

    /**
     * 玩家連線時異步載入好友列表、黑名單與設定至記憶體快取
     *
     * @param playerUuid 玩家 UUID
     * @return CompletableFuture<Void> 載入完成的 Future
     */
    public CompletableFuture<Void> loadPlayerDataAsync(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            Set<UUID> friends = FriendDAO.getFriends(playerUuid);
            Set<UUID> blocked = FriendBlockDAO.getBlockedPlayers(playerUuid);
            PlayerSettings settings = PlayerSettingsDAO.getSettings(playerUuid);

            friendCache.put(playerUuid, Collections.synchronizedSet(friends));
            blockCache.put(playerUuid, Collections.synchronizedSet(blocked));
            settingsCache.put(playerUuid, settings);
        });
    }

    /**
     * 玩家離線時清理快取
     *
     * @param playerUuid 玩家 UUID
     */
    public void unloadPlayerData(UUID playerUuid) {
        friendCache.remove(playerUuid);
        blockCache.remove(playerUuid);
        settingsCache.remove(playerUuid);
        requestCooldowns.remove(playerUuid);
    }

    /**
     * 發送好友申請（異步處理驗證與存庫）
     *
     * @param sender 申請發送者 UUID
     * @param receiver 申請接收者 UUID
     * @return CompletableFuture<RequestResult> 申請處理結果
     */
    public CompletableFuture<RequestResult> sendFriendRequestAsync(UUID sender, UUID receiver) {
        return sendFriendRequestAsync(sender, receiver, null, null);
    }

    public CompletableFuture<RequestResult> sendFriendRequestAsync(UUID sender, UUID receiver, String senderNameHint, String receiverNameHint) {
        return CompletableFuture.supplyAsync(() -> {
            if (sender.equals(receiver)) {
                return RequestResult.CANNOT_ADD_SELF;
            }

            // 冷卻時間檢查
            long now = System.currentTimeMillis();
            Long lastTime = requestCooldowns.get(sender);
            if (lastTime != null && (now - lastTime) < DEFAULT_COOLDOWN_MS) {
                return RequestResult.COOLDOWN;
            }

            // 檢查是否已被對方黑名單
            if (isBlocked(receiver, sender)) {
                return RequestResult.BLOCKED;
            }

            // 檢查是否已是好友
            if (isFriend(sender, receiver)) {
                return RequestResult.ALREADY_FRIENDS;
            }

            // 檢查對方設定是否允許接受好友申請
            PlayerSettings receiverSettings = getSettings(receiver);
            if (!receiverSettings.isAllowFriendRequests()) {
                return RequestResult.REQUESTS_DISABLED;
            }

            // 檢查是否已有雙向申請，若對方已發送過申請給自己，則直接升級為好友
            if (FriendRequestDAO.hasPendingRequest(receiver, sender)) {
                acceptFriendRequestAsync(sender, receiver).join();
                return RequestResult.AUTO_ACCEPTED;
            }

            // 檢查發送者好友數量上限
            Player senderPlayer = Bukkit.getPlayer(sender);
            if (senderPlayer != null) {
                int limit = getMaxFriendLimit(senderPlayer);
                Set<UUID> currentFriends = getFriends(sender);
                if (currentFriends.size() >= limit) {
                    return RequestResult.SENDER_LIMIT_REACHED;
                }
            }

            boolean success = FriendRequestDAO.sendRequest(sender, receiver);
            if (success) {
                requestCooldowns.put(sender, now);
                String senderName = resolvePlayerName(sender, senderNameHint);
                String receiverName = resolvePlayerName(receiver, receiverNameHint);
                me.xydesu.chatconduit.redis.RedisManager.publishFriendRequestNotify(
                        new me.xydesu.chatconduit.redis.FriendRequestNotifyPacket(
                                sender.toString(), senderName, receiver.toString(), receiverName,
                                me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                me.xydesu.chatconduit.redis.FriendRequestNotifyPacket.Action.SEND,
                                System.currentTimeMillis()
                        )
                );
                return RequestResult.SUCCESS;
            } else {
                return RequestResult.FAILED;
            }
        });
    }

    /**
     * 動態計算玩家的好友數量上限（基於 LuckPerms / 權限點 chatconduit.friend.limit.<數量>）
     *
     * @param player 目標玩家
     * @return int 好友數量上限
     */
    public int getMaxFriendLimit(Player player) {
        if (player == null) {
            return 20;
        }
        if (player.hasPermission("chatconduit.admin.bypasslimit") || player.hasPermission("chatconduit.friend.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }

        int maxLimit = 20; // 預設 20 人
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission().toLowerCase();
            if (perm.startsWith("chatconduit.friend.limit.")) {
                String limitStr = perm.substring("chatconduit.friend.limit.".length());
                try {
                    int limit = Integer.parseInt(limitStr);
                    if (limit > maxLimit) {
                        maxLimit = limit;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxLimit;
    }

    /**
     * 撤回已發送的好友申請
     *
     * @param sender 發送者 UUID
     * @param receiver 接收者 UUID
     * @return CompletableFuture<Boolean> 是否成功撤回
     */
    public CompletableFuture<Boolean> revokeFriendRequestAsync(UUID sender, UUID receiver) {
        return revokeFriendRequestAsync(sender, receiver, null, null);
    }

    public CompletableFuture<Boolean> revokeFriendRequestAsync(UUID sender, UUID receiver, String senderNameHint, String receiverNameHint) {
        return CompletableFuture.supplyAsync(() -> {
            boolean removed = FriendRequestDAO.removeRequest(sender, receiver);
            if (removed) {
                String senderName = resolvePlayerName(sender, senderNameHint);
                String receiverName = resolvePlayerName(receiver, receiverNameHint);
                me.xydesu.chatconduit.redis.RedisManager.publishFriendRequestNotify(
                        new me.xydesu.chatconduit.redis.FriendRequestNotifyPacket(
                                sender.toString(), senderName, receiver.toString(), receiverName,
                                me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                me.xydesu.chatconduit.redis.FriendRequestNotifyPacket.Action.REVOKE,
                                System.currentTimeMillis()
                        )
                );
            }
            return removed;
        });
    }

    /**
     * 接受好友申請
     *
     * @param receiver 接受者 UUID
     * @param sender 發送者 UUID
     * @return CompletableFuture<Boolean> 是否成功接受
     */
    public CompletableFuture<Boolean> acceptFriendRequestAsync(UUID receiver, UUID sender) {
        return acceptFriendRequestAsync(receiver, sender, null, null);
    }

    public CompletableFuture<Boolean> acceptFriendRequestAsync(UUID receiver, UUID sender, String receiverNameHint, String senderNameHint) {
        return CompletableFuture.supplyAsync(() -> {
            boolean hasRequest = FriendRequestDAO.hasPendingRequest(sender, receiver);
            if (!hasRequest) {
                return false;
            }

            boolean added = FriendDAO.addFriend(sender, receiver);
            if (added) {
                FriendRequestDAO.removeRequest(sender, receiver);

                // 更新快取
                Set<UUID> receiverFriends = friendCache.get(receiver);
                if (receiverFriends != null) {
                    receiverFriends.add(sender);
                }
                Set<UUID> senderFriends = friendCache.get(sender);
                if (senderFriends != null) {
                    senderFriends.add(receiver);
                }

                String receiverName = resolvePlayerName(receiver, receiverNameHint);
                String senderName = resolvePlayerName(sender, senderNameHint);

                me.xydesu.chatconduit.redis.RedisManager.publishFriendRequestNotify(
                        new me.xydesu.chatconduit.redis.FriendRequestNotifyPacket(
                                receiver.toString(), receiverName, sender.toString(), senderName,
                                me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                me.xydesu.chatconduit.redis.FriendRequestNotifyPacket.Action.ACCEPT,
                                System.currentTimeMillis()
                        )
                );
                return true;
            }
            return false;
        });
    }

    /**
     * 拒絕好友申請
     *
     * @param receiver 拒絕者 UUID
     * @param sender 發送者 UUID
     * @return CompletableFuture<Boolean> 是否成功刪除申請
     */
    public CompletableFuture<Boolean> denyFriendRequestAsync(UUID receiver, UUID sender) {
        return denyFriendRequestAsync(receiver, sender, null, null);
    }

    public CompletableFuture<Boolean> denyFriendRequestAsync(UUID receiver, UUID sender, String receiverNameHint, String senderNameHint) {
        return CompletableFuture.supplyAsync(() -> {
            boolean removed = FriendRequestDAO.removeRequest(sender, receiver);
            if (removed) {
                String receiverName = resolvePlayerName(receiver, receiverNameHint);
                String senderName = resolvePlayerName(sender, senderNameHint);
                me.xydesu.chatconduit.redis.RedisManager.publishFriendRequestNotify(
                        new me.xydesu.chatconduit.redis.FriendRequestNotifyPacket(
                                receiver.toString(), receiverName, sender.toString(), senderName,
                                me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                me.xydesu.chatconduit.redis.FriendRequestNotifyPacket.Action.DENY,
                                System.currentTimeMillis()
                        )
                );
            }
            return removed;
        });
    }

    /**
     * 刪除好友關係
     *
     * @param player 玩家 UUID
     * @param friend 好友 UUID
     * @return CompletableFuture<Boolean> 是否成功刪除
     */
    public CompletableFuture<Boolean> removeFriendAsync(UUID player, UUID friend) {
        return removeFriendAsync(player, friend, null, null, true);
    }

    public CompletableFuture<Boolean> removeFriendAsync(UUID player, UUID friend, String playerNameHint, String friendNameHint) {
        return removeFriendAsync(player, friend, playerNameHint, friendNameHint, true);
    }

    private CompletableFuture<Boolean> removeFriendAsync(UUID player, UUID friend, String playerNameHint, String friendNameHint, boolean publishNotify) {
        return CompletableFuture.supplyAsync(() -> {
            boolean removed = FriendDAO.removeFriend(player, friend);
            if (removed) {
                Set<UUID> playerFriends = friendCache.get(player);
                if (playerFriends != null) {
                    playerFriends.remove(friend);
                }
                Set<UUID> friendFriends = friendCache.get(friend);
                if (friendFriends != null) {
                    friendFriends.remove(player);
                }
                if (publishNotify) {
                    me.xydesu.chatconduit.redis.RedisManager.publishFriendRequestNotify(
                            new me.xydesu.chatconduit.redis.FriendRequestNotifyPacket(
                                    player.toString(), resolvePlayerName(player, playerNameHint),
                                    friend.toString(), resolvePlayerName(friend, friendNameHint),
                                    me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                    me.xydesu.chatconduit.redis.FriendRequestNotifyPacket.Action.REMOVE,
                                    System.currentTimeMillis()
                            )
                    );
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 將目標加入黑名單（同時自動解綁好友關係與刪除申請）
     *
     * @param player 執行者 UUID
     * @param target 目標 UUID
     * @return CompletableFuture<Boolean> 是否成功屏蔽
     */
    public CompletableFuture<Boolean> blockPlayerAsync(UUID player, UUID target) {
        return blockPlayerAsync(player, target, null, null);
    }

    public CompletableFuture<Boolean> blockPlayerAsync(UUID player, UUID target, String playerNameHint, String targetNameHint) {
        return CompletableFuture.supplyAsync(() -> {
            if (player.equals(target)) {
                return false;
            }

            boolean blocked = FriendBlockDAO.blockPlayer(player, target);
            if (blocked) {
                // 自動強制雙向解綁好友關係
                removeFriendAsync(player, target, playerNameHint, targetNameHint, false).join();
                // 刪除相關申請
                FriendRequestDAO.removeRequest(player, target);
                FriendRequestDAO.removeRequest(target, player);

                Set<UUID> playerBlocks = blockCache.get(player);
                if (playerBlocks != null) {
                    playerBlocks.add(target);
                }
                me.xydesu.chatconduit.redis.RedisManager.publishFriendRequestNotify(
                        new me.xydesu.chatconduit.redis.FriendRequestNotifyPacket(
                                player.toString(), resolvePlayerName(player, playerNameHint),
                                target.toString(), resolvePlayerName(target, targetNameHint),
                                me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                me.xydesu.chatconduit.redis.FriendRequestNotifyPacket.Action.BLOCK,
                                System.currentTimeMillis()
                        )
                );
                return true;
            }
            return false;
        });
    }

    /**
     * 解除黑名單
     *
     * @param player 執行者 UUID
     * @param target 目標 UUID
     * @return CompletableFuture<Boolean> 是否成功解鎖
     */
    public CompletableFuture<Boolean> unblockPlayerAsync(UUID player, UUID target) {
        return CompletableFuture.supplyAsync(() -> {
            boolean unblocked = FriendBlockDAO.unblockPlayer(player, target);
            if (unblocked) {
                Set<UUID> playerBlocks = blockCache.get(player);
                if (playerBlocks != null) {
                    playerBlocks.remove(target);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 檢查兩玩家是否為好友（優先快取）
     */
    public boolean isFriend(UUID player, UUID friend) {
        Set<UUID> friends = friendCache.get(player);
        if (friends != null) {
            return friends.contains(friend);
        }
        return FriendDAO.isFriend(player, friend);
    }

    /**
     * 檢查 player 是否封鎖了 target（優先快取）
     */
    public boolean isBlocked(UUID player, UUID target) {
        Set<UUID> blocked = blockCache.get(player);
        if (blocked != null) {
            return blocked.contains(target);
        }
        return FriendBlockDAO.isBlocked(player, target);
    }

    /**
     * 獲取玩家好友列表（優先快取）
     */
    public Set<UUID> getFriends(UUID player) {
        Set<UUID> cached = friendCache.get(player);
        if (cached != null) {
            return new HashSet<>(cached);
        }
        return FriendDAO.getFriends(player);
    }

    /**
     * 獲取玩家接收到的未處理申請
     */
    public CompletableFuture<List<FriendRequest>> getIncomingRequestsAsync(UUID receiver) {
        return CompletableFuture.supplyAsync(() -> FriendRequestDAO.getIncomingRequests(receiver));
    }

    /**
     * 獲取玩家發出的未處理申請
     */
    public CompletableFuture<List<FriendRequest>> getOutgoingRequestsAsync(UUID sender) {
        return CompletableFuture.supplyAsync(() -> FriendRequestDAO.getOutgoingRequests(sender));
    }

    /**
     * 獲取玩家個人設定（優先快取）
     */
    public PlayerSettings getSettings(UUID player) {
        PlayerSettings cached = settingsCache.get(player);
        if (cached != null) {
            return cached;
        }
        return PlayerSettingsDAO.getSettings(player);
    }

    /**
     * 更新玩家個人設定
     */
    public CompletableFuture<Boolean> updateSettingsAsync(PlayerSettings settings) {
        return CompletableFuture.supplyAsync(() -> {
            boolean saved = PlayerSettingsDAO.saveSettings(settings);
            if (saved) {
                settingsCache.put(settings.getUuid(), settings);
                return true;
            }
            return false;
        });
    }

    /**
     * 跨服社交事件抵達時，讓本服在線玩家的快取重新對齊資料庫。
     */
    public void refreshOnlinePlayerData(UUID playerUuid) {
        if (playerUuid == null || Bukkit.getPlayer(playerUuid) == null) {
            return;
        }
        loadPlayerDataAsync(playerUuid).thenRun(() ->
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        me.xydesu.chatconduit.gui.GUIRefresher.refreshForPlayer(Bukkit.getPlayer(playerUuid))
                )
        );
    }

    private String resolvePlayerName(UUID playerUuid, String nameHint) {
        if (nameHint != null && !nameHint.trim().isEmpty() && !"Unknown".equalsIgnoreCase(nameHint)) {
            return nameHint;
        }

        Player online = Bukkit.getPlayer(playerUuid);
        if (online != null) {
            return online.getName();
        }

        me.xydesu.chatconduit.redis.RedisPlayerRegistry.PlayerData redisData =
                me.xydesu.chatconduit.redis.RedisPlayerRegistry.getPlayerDataByUuid(playerUuid.toString());
        if (redisData != null && redisData.getName() != null && !redisData.getName().trim().isEmpty()) {
            return redisData.getName();
        }

        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(playerUuid);
        if (offline.getName() != null && !offline.getName().trim().isEmpty()) {
            return offline.getName();
        }

        return playerUuid.toString().substring(0, 8);
    }

    /**
     * 發送好友申請回應結果列舉
     */
    public enum RequestResult {
        SUCCESS,
        COOLDOWN,
        BLOCKED,
        ALREADY_FRIENDS,
        CANNOT_ADD_SELF,
        REQUESTS_DISABLED,
        AUTO_ACCEPTED,
        SENDER_LIMIT_REACHED,
        RECEIVER_LIMIT_REACHED,
        FAILED
    }
}
