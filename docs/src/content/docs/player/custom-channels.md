---
title: 🎮 自建玩家群組頻道 (/playerchannel)
description: 如何建立專屬的玩家私人頻道、設置加入密碼與邀請好友加入
---

# 🎮 自建玩家群組頻道 (/playerchannel)

想和您的公會夥伴或幾位好友建立私密討論群？ChatConduit 允許玩家自建專屬群組頻道！

---

## 🛠️ 指令對照表 (Command List)

| 指令 | 說明 |
| :--- | :--- |
| `/playerchannel create <頻道ID> <頻道名稱> [密碼]` | 創建一個新的群組頻道（可設密碼） |
| `/playerchannel join <頻道ID> [密碼]` | 加入特定的群組頻道 |
| `/playerchannel leave <頻道ID>` | 離開該群組頻道 |
| `/playerchannel invite <玩家>` | 邀請線上/異服好友加入頻道（對方接收點擊 Component）|
| `/playerchannel switch <頻道ID>` | 將發言切換至該群組頻道 |
| `/playerchannel gui` | 開啟自建頻道控制台 GUI 選單 |

---

## 🔒 密碼保護與權限
- 設定密碼的頻道，其他玩家加入時必須輸入正確密碼。
- 頻道創建者可透過控制台 GUI 管理成員名單或升級頻道。
