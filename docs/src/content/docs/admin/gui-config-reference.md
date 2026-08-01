---
title: GUI 佈局設定參考
description: ChatConduit 所有 gui/*.yml 箱子介面的佈局槽位與按鈕功能說明
---

import { Aside } from '@astrojs/starlight/components';

`plugins/ChatConduit/gui/` 目錄下的每個 YAML 檔案控制一個箱子 GUI 的外觀與功能。

<Aside type="tip">
  修改任何 GUI 設定後，在遊戲內執行 `/chatconduit reload` 即可即時套用，**無需重啟伺服器**。
</Aside>

---

## 通用格式說明

每個 GUI 設定檔支援以下頂層欄位：

| 欄位 | 說明 |
| :--- | :--- |
| `title` | 箱子標題，支援 MiniMessage 格式 |
| `size` | 箱子大小，可選 `9`, `18`, `27`, `36`, `45`, `54` |
| `slots` | 功能槽位群組定義（各 GUI 獨立） |
| `items` | 每個按鈕的 `material`、`name`、`lore`、`slot(s)` 定義 |

---

## channel_select.yml — 聊天頻道控制台

玩家執行 `/channel` 或點擊主選單時開啟。

**槽位對應 (54 格，6×9)**：

| 槽位群組 | 預設槽位 | 功能 |
| :--- | :--- | :--- |
| `system-channels` | 10–16, 19–25 | 系統官方頻道圖示區域 |
| `custom-channels` | 37–43 | 玩家自訂群組頻道圖示區域 |
| `filler-glass` | 邊框 | 灰色玻璃板填充裝飾 |
| `divider-line` | 27–35 | 藍色玻璃板水平分隔線 |
| `prev-page` | 46 | 上一頁箭頭 |
| `next-page` | 52 | 下一頁箭頭 |
| `create-channel` | 47 | 建立新群組頻道（翡翠） |
| `manage-channel` | 48 | 管理目前所屬群組（命名牌） |
| `close-menu` | 49 | 關閉介面（障礙物） |
| `pending-invites` | 50 | 查看待處理頻道邀請（書本） |
| `message-settings` | 51 | 個人訊息顯示偏好設定（鐘） |

---

## friend_main.yml — 好友主選單

執行 `/friend gui` 或 `/f` 開啟。包含好友列表入口、申請列表、黑名單與在線玩家查看。

---

## friend_list.yml — 好友列表

顯示目前所有好友的頭顱，支援分頁瀏覽。
- **左鍵點擊好友頭顱**：快速填入 `/msg <好友名>` 私訊
- **右鍵點擊好友頭顱**：開啟好友管理次選單（刪除 / 查看資訊）

---

## friend_requests.yml — 好友申請列表

顯示所有收到的待處理好友申請。
- **左鍵（翡翠）**：接受申請
- **右鍵（紅石）**：拒絕申請

---

## friend_block.yml — 黑名單管理

顯示已封鎖的玩家頭顱列表，點擊頭顱可解除封鎖。

---

## message_settings.yml — 訊息偏好設定

控制個人死亡廣播與進出伺服器通知的顯示開關。狀態儲存於資料庫，重登後保留設定。

---

## online_players.yml — 在線玩家列表

顯示目前在線玩家頭顱（包含跨服玩家，需啟用 Redis）。

---

## channel_settings.yml — 群組頻道管理面板

頻道隊長與成員管理介面：修改頻道名稱、描述、守則、密碼，踢出成員或解散頻道。

---

## player_channel_manage.yml — 玩家頻道控制台

群組頻道成員視角管理介面：查看頻道資訊、退出頻道或設定 Discord Webhook。

---

## pending_invites.yml — 待處理頻道邀請

顯示尚未處理的群組頻道邀請，可點擊接受或拒絕。
