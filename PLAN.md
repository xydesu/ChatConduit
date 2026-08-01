# 🚀 ChatConduit 開發計畫書 (Project Roadmap)

> **專案作者 (Author)**: xydesu  
> **基礎套件 (Base Package)**: `me.xydesu.chatconduit`  
> **專案環境 (Environment)**: Paper / Purpur 1.20+, Java 25, HikariCP, Redis Pub/Sub

---

## 📌 優先級規劃 (Priority Overview)

* **🔥 第一階段（核心同步 - 已完成）**: Redis 跨服狀態廣播、跨服私訊與即時申請通知、Redis 快取與黑名單保護。
* **⚡ 第一階段（進行中）**: Chest GUI Symbol 動態佈局重構、GUI 分頁與即时刷新。
* **✨ 第二階段（進階擴充）**: VIP 專屬好友特效與視覺擴充、異步頭顱 Skin 加載優化、跨服傳送對接 (`/friend tp`)。
* **📚 第三階段（文件與維基 Roadmap）**: 現代化開源 WIKI (Starlight/Docusaurus) 維護、API 開發者文件。

---

## 1. 🔥 第一階段：核心開發與跨服/GUI強化 (High Priority)

### 1.1 Redis 跨服好友狀態與廣播 (Redis Sync & PubSub)
* [x] **Pub/Sub 訊息廣播**
    * [x] 上下線/切換伺服器事件頻道 (`friend:status_change` - `FriendStatusPacket`)
    * [x] 跨服私訊頻道 (`friend:private_msg` - `PrivateMessagePacket`)
    * [x] 好友申請與實時通知頻道 (`friend:request_notify` - `FriendRequestNotifyPacket`)
* [x] **Redis Data Cache**
    * [x] 快取在線玩家列表與所在伺服器 (`online_players` Hash/Set - `RedisPlayerRegistry`)
    * [x] 快取玩家好友名單與在線狀態以降低資料庫查詢壓力

---

### 1.2 Chest GUI Symbol 佈局架構重構 (Symbol-based GUI System)
* [x] **GUI 佈局解耦**
    * [x] 將 GUI 選單 slots 設定由固定數字 (`0, 1, 2...`) 重構成基於符號地圖 (Symbol Map) 的結構 (例如用 `'#'` 代表邊框, `'F'` 代表好友, `'P'` 代表分頁)
    * [x] 允許管理員在 YAML 中自訂圖案佈局與按鈕放置位置

---

### 1.3 頁面翻頁與動態 GUI 刷新 (Pagination & Dynamic GUI Refresh)
* [x] **動態分頁演算法**
    * [x] 支援多頁自動切割（前一頁 / 下一頁 / 第一頁 / 最後一頁）
    * [x] 當前頁碼與總頁數顯示（如：`第 2 / 5 頁`）
* [x] **即時 GUI 刷新**
    * [x] 監聽玩家狀態變化（好友上線/下線），自動更新目前開啟 GUI 的玩家視窗
    * [x] 異步載入頭顱 Skin（避免翻頁時伺服器卡頓）

---

## 2. ✨ 第二階段：好友特權與黑名單保護 (Medium Priority)

### 2.1 好友上限與 VIP 特權 (Friend Limits & VIP Perks)
* [x] **動態上限計算**
    * [x] 基礎玩家好友數量上限（預設 20 人）
    * [x] 基於權限節點（LuckPerms）授權額外額度（例如：`chatconduit.friend.limit.50`, `chatconduit.friend.limit.100`）
    * [x] 管理員與 VIP 無上限權限 (`chatconduit.admin.bypasslimit`)
* [ ] **VIP 專屬特權**
    * [ ] 專屬 GUI 介面邊框 / 頭顱特效
    * [ ] 好友上線專屬提示音與播報
    * [ ] 優先跨服傳送通道

---

### 2.2 黑名單/屏蔽系統 (Blacklist / Block System)
* [x] **黑名單邏輯**
    * [x] 屏蔽好友申請（被屏蔽者發送申請時自動忽略或提示失敗）
    * [x] 自動解除好友關係（若加入黑名單時雙方已是好友，強制解綁）
    * [x] GUI 內點擊可解鎖/檢視黑名單成員
* [x] **屏蔽私訊與跨服保護**
    * [x] 阻止被封鎖玩家發送跨服/本地私訊 (`PrivateMessageManager` 封鎖攔截)
    * [x] 阻止被封鎖玩家發起好友傳送與邀請

---

### 2.3 🤖 Agent 建議擴充功能 (Agent Suggestions & Extensions)

> *(Note: Agent 已在此為您規劃與補充具體優化方向)*

* [ ] **跨服好友傳送與請求對接 (`/friend tp <玩家>`)**
    * [ ] 經由 Redis 發送跨服傳送請求 `friend:tp_request`
    * [ ] 支援目標玩家畫面可點擊 `[接受傳送]` / `[拒絕傳送]` Component 按鈕
* [ ] **PlaceholderAPI 好友動態變數**
    * [ ] `%chatconduit_friend_count%` (總好友數)
    * [ ] `%chatconduit_online_friends%` (線上好友數)
    * [ ] `%chatconduit_pending_requests%` (待處理申請數)
* [ ] **好友私密群組語音/聊天房**
    * [ ] 支援一鍵建立好友專屬通道，跨服聊天下自動過濾非好友成員

---

## 3. 📚 官方維基與開發者文件 (Wiki Roadmap)

> *(Note: 建議使用 **Starlight (Astro)** 或 **Docusaurus** 建立極簡現代開源維基網站)*

* [ ] **概述與快速入門 (Overview & Quick Start)**
    * [ ] 插件簡介與核心特色（零指令聊天頻道、箱子 GUI 控制台、跨服同步）
    * [ ] 系統需求（Paper 1.20+, Java 25, HikariCP, Jedis 5.x）
    * [ ] 安裝步驟與權限點（LuckPerms 節點說明）
* [ ] **頻道與聊天系統 (Channels & Private Messaging)**
    * [ ] 系統頻道配置 (`config.yml` 頻道前綴與格式)
    * [ ] 玩家自建頻道與密碼機制 (`/playerchannel`)
    * [ ] 跨服私訊與回覆 (`/msg`, `/reply`)
* [ ] **好友與社交系統 (Friend & Social System)**
    * [ ] 好友關係管理與指令清單 (`/friend add`, `/friend accept`, `/friend deny`, `/friend revoke`, `/friend remove`)
    * [ ] 好友箱子 GUI 互動說明
    * [ ] 跨服好友狀態通知與傳送機制
    * [ ] 屏蔽與黑名單保護機制
* [ ] **第三方整合與跨服架構 (Integrations & Cross-Server Sync)**
    * [ ] Redis Pub/Sub 廣播機制說明
    * [ ] DiscordSRV 雙向連動與 Webhook 設置
    * [ ] CMI AFK 掛機狀態自動偵測
    * [ ] PlaceholderAPI 變數清單

---

## 4. ✅ 已完成功能紀錄 (Completed Features Archive)

### 4.1 好友系統基礎與指令 (Friend Core & Commands)
* [x] **請求與驗證機制**
    * [x] 發送好友申請（包含防刷申請的 CD 限制）
    * [x] 接受 / 拒絕好友申請
    * [x] 撤回已發送的好友申請 (`/friend revoke <玩家名>`)
    * [x] 刪除好友（雙向解綁）
* [x] **完整指令與 Tab 補全**
    * [x] `/friend help`, `/friend add`, `/friend accept`, `/friend deny`, `/friend revoke`, `/friend remove`, `/friend list`, `/friend block`, `/friend unblock`, `/friend gui`
    * [x] 整合 `TabCompleter` 自動補全在線玩家、發出申請與好友名單

### 4.2 箱子 GUI 控制台 (Chest GUI Console)
* [x] **主選單 (Main Menu)**: 個人資訊頭顱、好友列表、待處理申請、黑名單管理
* [x] **好友列表視窗**: 動態頭顱展示、左鍵私訊、右鍵刪除好友、手動新增好友
* [x] **申請管理與黑名單視窗**: 一鍵接受/拒絕申請、一鍵解除黑名單

### 4.3 資料庫與非同步 I/O (Database Storage)
* [x] `friends`, `friend_requests`, `friend_blocks`, `player_settings` 表 Schema 設計
* [x] 支援 SQLite 與 MySQL (HikariCP 連線池)
* [x] 採用 `CompletableFutures` 與 Bukkit Scheduler 完全非同步 SQL 操作

### 4.4 跨服廣播、黑名單保護與隱藏訊息
* [x] Redis 跨服好友連線/離線狀態即時廣播 (`FriendStatusPacket`)
* [x] Redis 跨服好友申請實時通知與可點擊互動 (`FriendRequestNotifyPacket`)
* [x] 全區黑名單私訊與傳送屏蔽保護 (`PrivateMessageManager` Block Guard)
* [x] CMI AFK 自動掛機狀態監測 (`CMIPlayerAfkStatusChangeEvent`)
* [x] 官方頻道取消訂閱功能
* [x] 移除 Velocity 殘留目錄，合併 Paper 與 Common 模組
* [x] README.md 與目前原始碼差異核對與同步 (`Gemini diff checked`)