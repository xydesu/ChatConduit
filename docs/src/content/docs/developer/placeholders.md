---
title: 🔌 PlaceholderAPI 變數大全
description: ChatConduit 原生 PlaceholderAPI 變數清單與傳回型態範例
---

# 🔌 PlaceholderAPI 變數大全

只要伺服器安裝了 **PlaceholderAPI** 插件，ChatConduit 將自動註冊 `chatconduit` 擴充。

---

## 📊 變數對照表 (%chatconduit_*%)

| Placeholder 變數 | 資料型態 | 範例傳回值 | 說明 |
| :--- | :--- | :--- | :--- |
| `%chatconduit_friend_count%` | Integer | `12` | 玩家目前的總好友數量 |
| `%chatconduit_online_friends%` | Integer | `5` | 玩家目前在線的好友數量（包含跨服） |
| `%chatconduit_pending_requests%` | Integer | `2` | 玩家收到的待處理好友申請數量 |
| `%chatconduit_channel%` | String | `global` | 玩家目前預設說話的頻道 Key |
| `%chatconduit_muted%` | Boolean | `false` | 玩家是否處於禁言狀態 (`true`/`false`) |

---

## 💡 使用場景範例
- **Scoreboard / TAB 記分板**: 顯示在線好友數量 `%chatconduit_online_friends%`
- **Chat Format 聊天格式**: 顯示玩家發言頻道名稱 `%chatconduit_channel%`
