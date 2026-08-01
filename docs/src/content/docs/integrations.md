---
title: 🔌 第三方整合與跨服
description: Redis Pub/Sub、PlaceholderAPI 變數、DiscordSRV 與 CMI AFK
---

# 🔌 第三方整合與跨服架構 (Integrations & Cross-Server Sync)

ChatConduit 提供強大的跨服 Pub/Sub 通訊架構，並與主流外掛程式無縫整合。

---

## ⚡ 1. Redis Pub/Sub 跨服廣播架構

當在 `config.yml` 中啟用 `redis.enabled: true` 時，所有子伺服器皆會連線至統一的 Redis 頻道。

### 📡 廣播封包對照表

| 封包類別 | 功能說明 | 觸發時機 |
| :--- | :--- | :--- |
| `ChatMessagePacket` | 跨服聊天訊息廣播 | 玩家在公眾/群組頻道發言 |
| `PrivateMessagePacket` | 跨服私訊傳遞 | `/msg` 發送私訊至異服線上玩家 |
| `FriendStatusPacket` | 跨服好友連線/離線狀態廣播 | 玩家 Join / Quit 伺服器 |
| `FriendRequestNotifyPacket` | 跨服好友申請與動作通知 | 發送/接受/拒絕申請與封鎖 |
| `PlayerChannelSyncPacket` | 跨服自訂群組頻道變動同步 | 建立、解散或修改群組頻道 |

---

## 🧩 2. PlaceholderAPI 變數清單 (`%chatconduit_*%`)

只要伺服器安裝並啟用了 **PlaceholderAPI**，ChatConduit 將自動註冊 `chatconduit` 擴充：

| Placeholder 變數 | 傳回值範例 | 說明 |
| :--- | :--- | :--- |
| `%chatconduit_friend_count%` | `15` | 玩家目前的總好友數量 |
| `%chatconduit_online_friends%` | `4` | 玩家目前在線的好友數量（含跨服） |
| `%chatconduit_pending_requests%` | `2` | 玩家收到的待處理好友申請數量 |
| `%chatconduit_channel%` | `global` | 玩家目前說話的頻道 Key |
| `%chatconduit_muted%` | `false` | 玩家是否處於禁言狀態 (`true`/`false`) |

---

## 🤖 3. DiscordSRV 雙向連動與 Webhook

ChatConduit 會自動 Hook **DiscordSRV**，允許將遊戲內聊天頻道訊息推送至 Discord 指定頻道，並接收 Discord 玩家發言：

- **遊戲 ➔ Discord**: 支援自訂格式與 Discord Webhook 頭顱頭像解析。
- **Discord ➔ 遊戲**: 將指定 Discord 頻道的訊息轉換為 MiniMessageComponent 廣播至遊戲內。

---

## 💤 4. CMI AFK 自動掛機監測

掛鉤 **CMI** 插件的 `CMIPlayerAfkStatusChangeEvent`：
- 當玩家進入 AFK 掛機狀態時，自動更新玩家社交狀態。
- 支援透過設定控制 AFK 玩家是否接收死亡或廣播訊息。
