## 1. 好友系統 (Friend System)

### 1.1 基礎系統 (Base System)

* [ ] **請求與驗證機制**
    * [ ] 發送好友申請（包含防刷申請的 CD 限制）
    * [ ] 接受 / 拒絕好友申請
    * [ ] 撤回已發送的好友申請
    * [ ] 刪除好友（雙向解綁）


* [ ] **狀態追蹤**
    * [ ] 上下線通知（向在線好友廣播）
    * [ ] 跨分服狀態同步（在線/離線/所在伺服器名稱）
    * [ ] 伺服器切換通知（例如：*玩家 A 移至了 生存分服-1*）



---

### 1.2 指令系統 (Command)

* [ ] **指令架構**
    * [ ] `/friend help` - 顯示指令說明清單
    * [ ] `/friend add <玩家名>` - 發送好友申請
    * [ ] `/friend accept <玩家名>` - 接受好友申請
    * [ ] `/friend deny <玩家名>` - 拒絕好友申請
    * [ ] `/friend remove <玩家名>` - 刪除好友
    * [ ] `/friend list [頁碼]` - 顯示好友清單（文字版）
    * [ ] `/friend gui` - 打開箱子 GUI 主介面
    * [ ] `/friend block <玩家名>` - 加入黑名單
    * [ ] `/friend unblock <玩家名>` - 移出黑名單
* [ ] **指令補全與Tab優化**
    * [ ] 整合 TabCompleter，自動補全在線玩家與好友名單



---

### 1.3 箱子 GUI 介面 (Chest GUI)

* [ ] **主選單 (Main Menu)**
    * [ ] 個人資訊頭顱（顯示目前好友數/上限、VIP 狀態）
    * [ ] 好友列表按鈕（點擊進入列表頁面）
    * [ ] 待處理申請按鈕（提示未讀申請數量）
    * [ ] 黑名單管理按鈕
    * [ ] 系統設定按鈕（如：關閉接受申請、拒絕傳送等）


* [ ] **好友列表視窗**
    * [ ] 動態玩家頭顱展示（顯示造型、暱稱、當前所在分服、上線狀態）
    * [ ] 點擊頭顱開啟「好友互動選單」：
    * [ ] 私訊快捷按鈕
    * [ ] 傳送快捷按鈕
    * [ ] 刪除好友確認按鈕




* [ ] **申請管理視窗**
    * [ ] 申請者頭顱清單（顯示發送時間）
    * [ ] 一鍵接受 / 一鍵拒絕所有申請



---

### 1.4 資料庫存儲 (Database Storage)

* [x] **資料表架構設計 (Schema Design)**
    * [x] `friends` 表：紀錄好友關係（`player_uuid`, `friend_uuid`, `created_at`）
    * [x] `friend_requests` 表：紀錄未處理申請（`sender_uuid`, `receiver_uuid`, `timestamp`）
    * [x] `friend_blocks` 表：紀錄黑名單（`player_uuid`, `blocked_uuid`）
    * [x] `player_settings` 表：紀錄玩家個人偏好與限制


* [x] **資料庫選型支援**
    * [x] SQLite（單機簡易部署）
    * [x] MySQL / MariaDB（跨服多機連接）



---

### 1.5 Redis 跨服同步 (Redis Multiple Server Sync)

* [ ] **Pub/Sub 訊息廣播**
    * [ ] 上下線/切換伺服器事件頻道（`friend:status_change`）
    * [ ] 跨服私訊頻道（`friend:private_msg`）
    * [ ] 好友申請與實時通知頻道（`friend:request_notify`）


* [ ] **Redis Data Cache**
    * [ ] 快取在線玩家列表與所在伺服器（`online_players` Hash/Set）
    * [ ] 快取玩家好友名單以降低資料庫查詢壓力



---

### 1.6 好友上限與 VIP 特權 (Friend Limits & VIP Perks)

* [ ] **動態上限計算**
    * [ ] 基礎玩家好友數量上限（例如：預設 20 人）
    * [ ] 基於權限節點（LuckPerms）授權額外額度（例如：`friend.limit.vip = 50`, `friend.limit.mvp = 100`）
    * [ ] 突破上限補正（若 VIP 到期，保留現有好友但無法新增）


* [ ] **VIP 專屬特權**
    * [ ] 專屬 GUI 介面邊框 / 頭顱特效
    * [ ] 好友上線專屬提示音與播報
    * [ ] 優先跨服傳送通道



---

### 1.7 黑名單/屏蔽系統 (Blacklist / Block System)

* [ ] **黑名單邏輯**
    * [ ] 屏蔽好友申請（被屏蔽者發送申請時自動忽略或提示失敗）
    * [ ] 屏蔽私訊與跨服傳送
    * [ ] 自動解除好友關係（若加入黑名單時雙方已是好友，強制解綁）


* [ ] **黑名單管理**
    * [ ] GUI 內點擊可解鎖/檢視黑名單成員



---

### 1.8 頁面翻頁與動態 GUI 刷新 (Pagination & Dynamic GUI Refresh)

* [ ] **動態分頁演算法**
    * [ ] 支援多頁自動切割（前一頁 / 下一頁 / 第一頁 / 最後一頁）
    * [ ] 當前頁碼與總頁數顯示（如：`第 2 / 5 頁`）


* [ ] **即時 GUI 刷新**
    * [ ] 監聽玩家狀態變化（好友上線/下線），自動更新目前開啟 GUI 的玩家視窗
    * [ ] 異步載入頭顱 Skin（避免翻頁時伺服器卡頓）



---

### 1.9 異步資料庫 I/O (Asynchronous DB I/O)

* [x] **線程池管理**
    * [x] 使用 Bukkit Scheduler 異步任務或 CompletableFuture 處理所有 SQL 操作
    * [x] 避免在主執行緒（Server Thread）執行任何 Blocking DB 操作


* [x] **連線池優化 (HikariCP)**
    * [x] 整合 HikariCP 管理 MySQL 資料庫連線，提高查詢效能與併發穩定度



---

## 2. 隱藏訊息系統 (Hidding Message System)

### 2.1 CMI 插件整合

* [x] **AFK 自動掛機狀態監測**
    * [x] 監聽 CMI 的 `CMIPlayerAfkStatusChangeEvent`
    * [x] 判斷目標玩家是否處於 AFK（離開）狀態

---

## 3. 頻道系統加強
* [x] **官方頻道**
    * [x] 可取消訂閱頻道

---

## 4. Plugins platform
* [x] Remove Velocity Platform
* [x] Merge both paper and common

## 5. Readme
* [x] Add missing part to readme
    * [x] Gemini You can edit this part to check what diff to current src and readme

---

## 6. 建議後續開發順序 (Suggested Next Tasks)
* [x] **第一階段：好友系統基礎資料層 (Friend Database & Core Model)**
    * [x] 設計與實作 `FriendDAO`, `FriendRequestDAO`, `FriendBlockDAO`, `PlayerSettingsDAO` (支援 SQLite 與 MySQL)
    * [x] 實作 `FriendManager` 處理記憶體快取與異步 SQL 讀寫
* [ ] **第二階段：好友指令與基礎驗證 (Friend Commands & Validation)**
    * [ ] 實作 `/friend add`, `/friend accept`, `/friend deny`, `/friend remove`, `/friend list`
    * [ ] 整合冷卻時間 (CD) 防止刷申請
* [ ] **第三階段：好友箱子 GUI 控制台 (Friend Chest GUI Console)**
    * [ ] 實作 `FriendGUIListener`, `FriendMainGUI`, `FriendListGUI`, `FriendRequestsGUI`



## 7. WIKI Page (Mark As Not Important, agent, you can edit here)

### ChatConduit 官方維基文件架構 (Wiki Roadmap & Documentation Outline)

* [ ] **概述與快速入門 (Overview & Quick Start)**
    * [ ] 插件簡介與核心特色（零指令聊天頻道、箱子 GUI 控制台、跨服同步）
    * [ ] 系統需求（Paper 1.20+, Java 25, HikariCP, Jedis 5.x）
    * [ ] 安裝步驟與權限點（LuckPerms 節點說明）

* [ ] **頻道與聊天系統 (Channels & Private Messaging)**
    * [ ] 系統頻道配置 (`config.yml` 頻道前綴與格式)
    * [ ] 玩家自建頻道與密碼機制 (`/playerchannel`)
    * [ ] 跨服私訊與回覆 (`/msg`, `/reply`)

* [ ] **好友與社交系統 (Friend & Social System)**
    * [ ] 好友關係管理與指令清單 (`/friend`)
    * [ ] 好友箱子 GUI 互動說明
    * [ ] 跨服好友狀態通知與傳送機制
    * [ ] 屏蔽與黑名單保護機制

* [ ] **第三方整合與跨服架構 (Integrations & Cross-Server Sync)**
    * [ ] Redis Pub/Sub 廣播機制說明
    * [ ] DiscordSRV 雙向連動與 Webhook 設置
    * [ ] CMI AFK 掛機狀態自動偵測
    * [ ] PlaceholderAPI 變數清單
