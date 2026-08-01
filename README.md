# ChatConduit

![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![PaperMC](https://img.shields.io/badge/Paper-1.20%2B-blue.svg)
![Build](https://img.shields.io/badge/Build-Maven-brightgreen.svg)
![Author](https://img.shields.io/badge/Author-xydesu-blueviolet.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**ChatConduit** 是一款專為現代 Minecraft 伺服器設計的高效能、全功能多頻道聊天插件。具備全 GUI 箱子介面零指令操作系統、HikariCP 非同步資料庫連線池、Redis 跨服聊天與頻道邀請廣播、Discord 雙向溝通、獨立 Webhook 整合、CMI AFK 狀態監測以及全功能 ChatColor 炫彩字體系統。

---

## 核心特色 (Core Features)

### 1. 全 Chest GUI 零指令頻道控制台
- 玩家只需輸入 `/channel` 或 `/pc` 即可開啟箱子選單介面。
- 支援一鍵快速切換官方頻道、切換頻道訂閱狀態、瀏覽群組頻道、邀請成員、管理權限與調整色彩主題。
- 支援安全的對話框文字輸入 (`PlayerInputManager`)，適用於重命名頻道、設定簡介守則與 Webhook URL。

### 2. 官方頻道與前綴快捷鍵 (System Channels)
- 支援管理員自由配置官方系統頻道（如全域廣播、交易頻道、公會頻道、管理員頻道等）。
- 支援頻道可取消訂閱功能，玩家可自由開關特定非強制頻道的訊息接收。
- 支援前綴快速鍵（例如 `$`, `!`, `@`, `?`），玩家無須切換頻道即可快速發送對話至特定頻道。
- 頻道支援豪華的 Hover (懸停視窗顯示頻道守則與簡介) 與 Click (點擊切換頻道) 互動功能。

### 3. 玩家自建群組頻道 (Player Group Channels)
- 玩家可自訂個人專屬的群組頻道，自選公共 (PUBLIC) 或私人 (PRIVATE) 模式。
- **頻道隊長權限**：頻道隊長可管理成員、踢出玩家、轉讓隊長職位、編輯簡介規範與主題顏色。
- **即時邀請推播**：支援點擊式 `[點擊接受]` 與 `[點擊拒絕]` 聊天欄推播邀請。

### 4. Redis 多伺服器跨服廣播 (Multi-Server Network)
- 採用 Redis PubSub 非同步網路傳輸機制，實現毫秒級跨服聊天訊息廣播、跨服私訊與跨服頻道邀請。

### 5. CMI AFK 自動掛機狀態監測 (CMI Integration)
- 整合 CMI 插件監聽 `CMIPlayerAfkStatusChangeEvent`，當目標玩家處於離開 (AFK) 狀態時自動進行標註與提示。

### 6. 跨服私訊與禁言管理系統 (PM & Mute System)
- 支援 `/msg` 與 `/reply` 進行本地與跨服私訊傳訊。
- 提供完整管理員禁言系統 (`/mute`, `/unmute`, `/mutelist`)，支援定時禁言與跨服禁言同步。

### 7. 炫彩對話字體與 GUI 選擇器 (Chat Color System)
- 提供 `/chatcolor` 箱子 GUI 選單，支援基礎顏色、RGB Hex 自訂色碼碼、Gradient 漸變雙色調、VIP 尊爵限定色與 Rainbow 七彩彩虹特效。

### 8. Discord 雙向溝通與外接 Webhook
- **DiscordSRV 掛載**：支援 Discord 頻道與遊戲內頻道的雙向映射轉發。
- **獨立群組 Webhook**：每個玩家群組頻道均可綁定獨立 Discord Webhook，將遊戲群組訊息實時推送至 Discord 頻道。
- **連線測試按鈕**：提供選單與指令 `testWebhook` 測試按鈕，自動化驗證 Webhook 是否綁定成功。

### 9. HikariCP 高效能資料庫連線池
- 支援 SQLite (單機預設) 與 MySQL (跨服分散式資料庫)。
- 完全採用非同步 SQL 讀寫作業，避免任何主伺服器執行緒 (Main Thread) 阻塞。

---

## 開發與建置需求 (Requirements)

- **Java Development Kit (JDK)**: 25 或以上
- **伺服器核心**: Paper / Purpur 1.20+ 或更新版本 (支援 Adventure API)
- **建置工具**: Maven 3.8+
- **選用相依插件**:
  - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (支援全域佔位符解析)
  - [DiscordSRV](https://www.spigotmc.org/resources/discordsrv.18494/) (雙向群組溝通)
  - [CMI](https://www.spigotmc.org/resources/cmi.3742/) (AFK 狀態自動監察)
  - Redis Server (若需跨服同步功能)

---

## 指令與權限說明 (Commands & Permissions)

### 常用指令列表
| 指令 | 別名 | 說明 | 預設權限 |
| :--- | :--- | :--- | :--- |
| `/channel [頻道ID]` | `/ch` | 開啟頻道選擇選單或切換發言頻道 | `chatconduit.use` |
| `/playerchannel` | `/pc` | 開啟玩家群組頻道管理 GUI 面板 | `chatconduit.use` |
| `/pc create <名稱>` | - | 建立新的玩家自訂群組頻道 | `chatconduit.create` |
| `/pc invite <玩家>` | - | 邀請線上或跨服玩家加入頻道 | 頻道隊長權限 |
| `/pc accept <頻道ID>` | - | 接受群組頻道邀請 | `chatconduit.use` |
| `/pc deny <頻道ID>` | - | 拒絕群組頻道邀請 | `chatconduit.use` |
| `/pc leave` | - | 退出當前所在的群組頻道 | `chatconduit.use` |
| `/pc members` | - | 查看當前群組頻道的成員清單 | `chatconduit.use` |
| `/pc manage kick <玩家>` | - | 踢出群組頻道成員 | 頻道隊長權限 |
| `/pc manage transfer <玩家>` | - | 轉讓頻道隊長職位給指定玩家 | 頻道隊長權限 |
| `/pc manage delete` | - | 解散並刪除群組頻道 | 頻道隊長權限 |
| `/msg <玩家> <訊息>` | `/tell`, `/w`, `/pm` | 發送跨服/本地私訊給指定玩家 | `chatconduit.use` |
| `/reply <訊息>` | `/r` | 快速回覆最後一位與您私訊的玩家 | `chatconduit.use` |
| `/chatcolor` | `/ccolor`, `/color` | 開啟發言文字顏色選擇 GUI 選單 | `chatconduit.chatcolor` |
| `/mute <玩家> [時間] [原因]` | - | 禁言指定玩家發言 | `chatconduit.admin.mute` |
| `/unmute <玩家>` | - | 解除指定玩家的禁言 | `chatconduit.admin.unmute` |
| `/mutelist` | - | 查看目前受禁言的玩家清單 | `chatconduit.admin.mutelist` |
| `/chatconduit help` | `/cc` | 顯示 ChatConduit 幫助選單 | `chatconduit.use` |
| `/chatconduit reload` | - | 重載插件設定與語言檔案 | `chatconduit.admin` |

### 特殊權限節點
- `chatconduit.chatcolor.gradient` - 允許解鎖與使用漸變限定聊天顏色 (Sunset, Cyberpunk, Ocean, Emerald)。
- `chatconduit.chatcolor.vip` - 允許解鎖與使用 VIP 尊爵限定聊天顏色 (Sakura, Royal Gold)。
- `chatconduit.chatcolor.rainbow` - 允許解鎖與使用七彩幻光彩虹聊天顏色 (Rainbow)。
- `chatconduit.chatcolor.hex` - 允許使用自訂 RGB Hex 顏色碼 (`#RRGGBB`)。
- `chatconduit.chat.color` - 允許在發言對話中使用舊版 Legacy 顏色碼 (如 `&a`, `&c`)。
- `chatconduit.chat.color.hex` - 允許在對話中使用 RGB Hex 顏色碼 (`&#RRGGBB`)。
- `chatconduit.chat.format.*` - 允許在對話中使用粗體 (`&l`)、斜體 (`&o`)、底線 (`&n`)、刪除線 (`&m`) 與亂碼 (`&k`)。
- `chatconduit.admin.bypasslimit` - 允許繞過玩家建立群組頻道的數量上限限制。

---

## 設定檔說明 (Configuration)

主設定檔 `config.yml` 包含以下模組區塊：
- `database`: 設定資料庫模式 (`sqlite` 或 `mysql`) 與 HikariCP 連線池參數。
- `channels`: 配置官方系統頻道名稱、顏色、前綴快捷鍵與權限。
- `player-channels`: 設定玩家自訂群組頻道數量上限、名稱長度限制與對話框輸入逾時時間。
- `redis`: 設定 Redis 伺服器主機、埠號、密碼與 PubSub 廣播頻道。
- `discordsrv`: 設定 DiscordSRV 雙向頻道映射與 Webhook 頭像/名稱格式。

多語言支援存於 `plugins/ChatConduit/lang/` 目錄：
- `zh-TW.yml` (正體中文 - 預設)
- `en-US.yml` (English)

---

## 單元測試與編譯打包 (Building & Testing)

專案已導入 **JUnit 5** 單元測試模組，覆蓋頻道核心、Redis 封包、Webhook 淨化、禁言管理與 ChatUtils 工具。

### 執行單元測試
```bash
mvn test
```

### 建置 Uber Jar 專案包
```bash
mvn clean package
```
編譯完成後，輸出的 Jar 檔案位於 `chatconduit-paper/target/chatconduit-paper-1.0.jar`。

---

## 作者與版權資訊 (Author & License)

- **作者 (Author)**: xydesu
- **基礎套件 (Base Package)**: `me.xydesu.chatconduit`
- **版權宣告**: 本專案採用 MIT 授權條款釋出。
