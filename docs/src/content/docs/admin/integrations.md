---
title: 👑 DiscordSRV 與 CMI AFK 整合
description: 雙向 Discord 聊天機器人 Webhook 連動與 CMI 掛機狀態自動過濾設置指南
---

# 👑 DiscordSRV 與 CMI AFK 整合

---

## 🤖 1. DiscordSRV 雙向連動與 Webhook

ChatConduit 原生 Hook **DiscordSRV**，無需撰寫額外代碼即可將遊戲內發言同步至 Discord 頻道：

### 遊戲 ➔ Discord 雙向推送
1. 當玩家在 `global` 或自訂頻道發言時，訊息經由 Discord Webhook 推送至對應 Discord Channel。
2. 支援動態頭像網址解析 (如 CraftHead / Crafatar API)，自動顯示玩家頭顱 Avatar。

### Discord ➔ 遊戲 訊息接收
- Discord 頻道內訊息自動轉譯為 MiniMessage Component 並送達遊戲聊天室。

---

## 💤 2. CMI AFK 掛機自動監測

ChatConduit 監聽 **CMI** 插件的 `CMIPlayerAfkStatusChangeEvent`：
- **自動狀態更新**: 當玩家進出 AFK 掛機狀態時，自動更新玩家資料卡片。
- **過濾廣播**: 管理員可於 `config.yml` 配置 AFK 玩家是否暫停接收全服播報，保持聊天欄乾淨。
