---
title: 🚀 概述與快速入門
description: ChatConduit Paper/Purpur 跨服聊天與社交外掛官方維基
template: splash
hero:
  tagline: 強大、零指令、跨服與高度解耦的 Minecraft 社交頻道插件
  actions:
    - text: 開始閱讀文檔
      link: /channels/
      icon: right-arrow
      variant: primary
    - text: 查看 GitHub 倉庫
      link: https://github.com/xydesu/ChatConduit
      icon: external
---

> **專案作者 (Author)**: xydesu  
> **基礎套件 (Base Package)**: `me.xydesu.chatconduit`  
> **專案環境 (Environment)**: Paper / Purpur 1.20+, Java 25, HikariCP, Redis Pub/Sub

---

## 📌 插件簡介與核心特色

**ChatConduit** 是一款專為大型跨服 (BungeeCord / Velocity) 與單服深度的 Minecraft 伺服器設計的新一代社交與聊天頻道管理插件。

### 🌟 核心亮點

* **零指令箱子 GUI 控制台**: 玩家可透過全可自訂的箱子 GUI 選單（基於符號 Symbol Map 佈局解耦）完成好友申請、黑名單解鎖、發送私訊與頻道切換。
* **Redis Pub/Sub 跨服廣播與同步**:
  * 跨服好友連線/離線狀態即時廣播 (`FriendStatusPacket`)。
  * 跨服好友申請與動作實時點擊推播 (`FriendRequestNotifyPacket`)。
  * 跨服私訊 (`PrivateMessagePacket`)。
  * Redis 記憶體快取全服線上玩家註冊冊 (`online_players`)。
* **高併發非同步資料庫架構**: 支援 SQLite 與 MySQL (HikariCP 連線池)，採用 `CompletableFuture` 與 Bukkit Scheduler，保證 SQL 操作 0 阻塞主執行緒。
* **全維度黑名單與隱私保護**: 強制自動解綁好友關係，並於本地與跨服維度全方位屏蔽私訊與邀請。
* **第三方生態擴充整合**:
  * 原生 **PlaceholderAPI** 變數擴充 (`%chatconduit_*%`)。
  * **DiscordSRV** 雙向連動與 Discord Webhook 支援。
  * **CMI AFK** 自動掛機狀態監測與播報過濾。

---

## ⚙️ 系統需求 (System Requirements)

| 組件名稱 | 最低需求 | 推薦版本 |
| :--- | :--- | :--- |
| **伺服器核心** | Paper / Purpur 1.20.x | Paper 1.20.4+ |
| **Java 執行環境** | Java 21 | **GraalVM JDK 25** |
| **資料庫** | SQLite (內建) | MySQL 8.0+ / MariaDB (HikariCP) |
| **跨服快取 (選填)** | Jedis 5.x | **Redis 6.x+** |

---

## 📦 安裝步驟 (Installation)

1. 將 `chatconduit-paper-1.0.jar` 放入伺服器的 `plugins/` 目錄中。
2. 啟動伺服器以自動產生預設配置檔案資料夾 `plugins/ChatConduit/`。
3. （可選）編輯 `plugins/ChatConduit/config.yml` 配置 MySQL 資料庫連線與 Redis 跨服設定。
4. 在遊戲內執行 `/chatconduit reload` 或重啟伺服器套用新設定。

---

## 🔑 權限點說明 (LuckPerms Node Overview)

| 權限節點 | 說明 | 預設擁有者 |
| :--- | :--- | :--- |
| `chatconduit.friend` | 允許使用好友基礎系統與 GUI 介面 | 所有玩家 (OP / Non-OP) |
| `chatconduit.friend.limit.<數量>` | 動態指定玩家最大好友容量 (如 `limit.50`) | VIP / 特權群組 |
| `chatconduit.friend.limit.unlimited` | 無限制好友容量上限 | VIP / 管理員 |
| `chatconduit.admin.bypasslimit` | 無視好友數量限制與冷卻時間 | 管理員 / OP |
| `chatconduit.chat.color` | 允許在聊天與私訊中使用 MiniMessage / Legacy 彩色文字 | VIP / 管理員 |
| `chatconduit.vip.notify` | 啟用好友上線專屬提示音 (`Sound.ENTITY_PLAYER_LEVELUP`) | VIP 玩家 |
| `chatconduit.admin.reload` | 重載插件配置檔與 GUI 選單 (`/chatconduit reload`) | 管理員 / OP |
