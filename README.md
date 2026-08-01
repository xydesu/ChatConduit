# ChatConduit

[![Build](https://github.com/xydesu/ChatConduit/actions/workflows/deploy-docs.yml/badge.svg)](https://github.com/xydesu/ChatConduit/actions)
![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![PaperMC](https://img.shields.io/badge/Paper-1.20%2B-blue.svg)
![Build Tool](https://img.shields.io/badge/Build-Maven-brightgreen.svg)
![Author](https://img.shields.io/badge/Author-xydesu-blueviolet.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Version](https://img.shields.io/badge/Version-1.0-informational.svg)

**ChatConduit** 是一款專為現代 Minecraft 伺服器設計的高效能、全功能多頻道聊天插件。具備全 GUI 箱子介面零指令操作系統、HikariCP 非同步資料庫連線池、Redis 跨服聊天與頻道邀請廣播、Discord 雙向溝通與整合 CMI AFK 掛機狀態監測。

**官方維基文件**: [xydesu.github.io/ChatConduit](https://xydesu.github.io/ChatConduit)

---

## 核心特色 (Core Features)

### 1. 全 Chest GUI 零指令頻道控制台
- 玩家只需輸入 `/channel` 或 `/pc` 即可開啟箱子選單介面。
- 支援一鍵快速切換官方頻道、切換頻道訂閱狀態、瀏覽群組頻道、邀請成員、管理權限。
- 支援安全的對話框文字輸入 (`PlayerInputManager`)，適用於重命名頻道、設定簡介守則與 Webhook URL。

### 2. 官方頻道與前綴快捷鍵 (System Channels)
- 管理員可在 `config.yml` 自由配置官方系統頻道（如全域廣播、交易頻道、組隊頻道等）。
- 支援前綴快速鍵（例如 `!`, `$`, `+`, `?`），玩家無須切換頻道即可快速發送對話。
- 頻道支援 Hover 懸停視窗顯示守則與 Click 互動切換功能。

### 3. 玩家自建群組頻道 (Player Group Channels)
- 玩家可自訂個人專屬的群組頻道，自選公共 (PUBLIC) 或私人 (PRIVATE) 模式。
- **頻道隊長**：可管理成員、踢人、轉讓隊長、編輯簡介、設定密碼。
- **即時邀請推播**：支援跨服點擊式 `[接受]` / `[拒絕]` 聊天欄推播邀請。

### 4. Redis 多伺服器跨服廣播 (Multi-Server Network)
- 採用 Redis Pub/Sub 非同步機制，實現毫秒級跨服聊天廣播、跨服私訊。
- 全服在線玩家名冊由 Redis 快取維護，確保跨服好友狀態即時同步。

### 5. 好友與社交系統 (Friend System)
- 完整 `/friend` 指令與全 Chest GUI 面板。
- 好友申請、接受、拒絕、撤回、刪除、黑名單封鎖，以及申請冷卻防刷機制。
- 黑名單封鎖時自動強制解除好友關係，並全維度屏蔽私訊與邀請。

### 6. 跨服私訊 (PM System)
- 支援 `/msg` 與 `/r` 進行本地與跨服私訊傳訊。

### 7. Discord 雙向溝通與外接 Webhook
- **DiscordSRV 掛載**：支援 Discord 頻道與遊戲內頻道的雙向映射轉發。
- **獨立群組 Webhook**：每個玩家群組頻道均可綁定獨立 Discord Webhook。
- **頭像渲染**：自動透過 mc-heads.net API 渲染玩家皮膚頭像。

### 8. HikariCP 非同步資料庫
- 支援 SQLite（單機預設）與 MySQL（跨服分散式）。
- 完全採用非同步 SQL 讀寫，確保 0 阻塞主執行緒 (Main Thread)。

---

## 系統需求 (Requirements)

| 項目 | 最低需求 | 推薦 |
| :--- | :--- | :--- |
| Java | 21 | **GraalVM JDK 25** |
| 伺服器核心 | Paper 1.20 | Paper / Purpur 1.20.4+ |
| 建置工具 | Maven 3.8 | Maven 3.9+ |

### 選用相依插件
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — 支援全域佔位符解析
- [DiscordSRV](https://www.spigotmc.org/resources/discordsrv.18494/) — 雙向 Discord 頻道整合
- [CMI](https://www.spigotmc.org/resources/cmi.3742/) — AFK 掛機狀態自動監測
- Redis Server — 跨服廣播與好友狀態同步（多服必備）

---

## 安裝方式 (Installation)

1. 下載最新版本的 `chatconduit-paper-1.0.jar`（見 [Releases](https://github.com/xydesu/ChatConduit/releases)）
2. 放入伺服器的 `plugins/` 目錄
3. 啟動伺服器，插件自動生成 `plugins/ChatConduit/` 配置目錄
4. 依需求修改 `config.yml` 後執行 `/chatconduit reload`

詳細安裝說明請參閱 **[官方維基 - 快速安裝指南](https://xydesu.github.io/ChatConduit/getting-started/installation/)**。

---

## 指令總覽 (Commands)

| 指令 | 別名 | 說明 |
| :--- | :--- | :--- |
| `/channel [頻道ID]` | `/ch` | 開啟頻道選擇 GUI 或切換發言頻道 |
| `/playerchannel` | `/pc` | 開啟玩家群組頻道管理 GUI |
| `/msg <玩家> <訊息>` | `/tell`, `/w` | 發送跨服私訊 |
| `/r <訊息>` | `/reply` | 快速回覆最後一位私訊玩家 |
| `/friend [gui]` | `/f` | 開啟好友社交 GUI 控制台 |
| `/friend add <玩家>` | - | 發送好友申請 |
| `/friend accept <玩家>` | - | 接受好友申請 |
| `/friend deny <玩家>` | - | 拒絕好友申請 |
| `/friend remove <玩家>` | - | 刪除好友 |
| `/friend block <玩家>` | - | 加入黑名單 |
| `/friend unblock <玩家>` | - | 解除黑名單 |
| `/chatconduit reload` | `/cc reload` | 重載所有設定檔 |

---

## 設定檔說明 (Configuration)

主設定檔 `config.yml` 包含以下模組區塊：

| 區塊 | 說明 |
| :--- | :--- |
| `server-id` | 子伺服器識別名稱（跨服時使用） |
| `chat-format` | 全服統一聊天格式模板 |
| `channels` | 官方系統頻道定義（名稱、色彩、前綴符號、權限） |
| `player-channels` | 玩家自建頻道限制（數量上限、名稱長度等） |
| `database` | 資料庫模式（`sqlite` / `mysql`）與 HikariCP 參數 |
| `redis` | Redis 跨服連線設定 |
| `discordsrv` | Discord 頻道映射與 Webhook 設定 |

多語言文件位於 `plugins/ChatConduit/lang/`：
- `zh-TW.yml`（正體中文，預設）
- `en-US.yml`（English）

詳細設定參數請參閱 **[config.yml 完整參數參考](https://xydesu.github.io/ChatConduit/admin/config-reference/)**。

---

## 建置 (Building)

```powershell
# 設定 Java 25 路徑（Windows）
$env:JAVA_HOME = "C:\Users\xy\AppData\Roaming\PrismLauncher\java\graalvm-jdk-25+37.1"

# 清理並打包
mvn clean package -DskipTests
```

輸出 Jar 位於 `chatconduit-paper/target/chatconduit-paper-1.0.jar`。

---

## 文件 (Documentation)

完整的管理員與玩家使用說明請前往官方維基：

**[https://xydesu.github.io/ChatConduit](https://xydesu.github.io/ChatConduit)**

- [快速入門](https://xydesu.github.io/ChatConduit/getting-started/overview/)
- [管理員指南](https://xydesu.github.io/ChatConduit/admin/channels/)
- [玩家指南](https://xydesu.github.io/ChatConduit/player/channels/)
- [config.yml 完整參考](https://xydesu.github.io/ChatConduit/admin/config-reference/)

---

## 作者與授權 (Author & License)

- **作者**: [xydesu](https://github.com/xydesu)
- **基礎套件**: `me.xydesu.chatconduit`
- **授權**: MIT License
