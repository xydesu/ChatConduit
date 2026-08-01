---
title: 👑 權限節點與 LuckPerms 授權
description: ChatConduit 完整的 LuckPerms 權限節點、VIP 限制與管理員專屬權限說明表
---

# 👑 權限節點與 LuckPerms 授權

ChatConduit 完全支援 **LuckPerms** 權限管理系統，管理員可為不同玩家群組 (Default, VIP, Admin) 授予細粒度的控制權限。

---

## 📜 完整權限對照表 (Permissions Node Table)

| 權限節點 (Permission Node) | 說明 (Description) | 預設擁有者 |
| :--- | :--- | :--- |
| `chatconduit.friend` | 允許使用 `/friend` 指令與開啟社交 GUI | 所有玩家 (OP / Non-OP) |
| `chatconduit.friend.limit.20` | 指定最大好友數量上限為 20 人 | 預設玩家群組 |
| `chatconduit.friend.limit.50` | 指定最大好友數量上限為 50 人 | VIP 1 贊助群組 |
| `chatconduit.friend.limit.100` | 指定最大好友數量上限為 100 人 | VIP 2 贊助群組 |
| `chatconduit.friend.limit.unlimited` | 無限制好友數量容量上限 | VIP 3 / 特權玩家 |
| `chatconduit.admin.bypasslimit` | 無視好友數量上限與申請 5 秒防刷 CD | 管理員 / OP |
| `chatconduit.chat.color` | 允許在聊天與私訊中使用 MiniMessage 彩色文字 | VIP / 管理員 |
| `chatconduit.vip.notify` | 啟用好友上線專屬提示音 (`Sound.ENTITY_PLAYER_LEVELUP`) | VIP 玩家 |
| `chatconduit.admin.reload` | 執行 `/chatconduit reload` 重載配置檔 | 管理員 / OP |
| `chatconduit.channel.trade` | 允許進入與發言至商業頻道 | 授權玩家 |
| `chatconduit.channel.vip` | 允許進入與發言至 VIP 貴賓頻道 | VIP 玩家 |
