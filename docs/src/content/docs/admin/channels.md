---
title: 👑 系統頻道與前綴配置
description: 管理員如何設定 config.yml 官方系統頻道、MiniMessage 漸變色彩與零指令發言前綴
---

管理員可以在 `plugins/ChatConduit/config.yml` 中定義任意數量的官方系統頻道。

---

## 📄 `config.yml` 頻道設定範例

```yaml
# 預設發言頻道 Key
default-channel: "global"

# 官方系統頻道定義清單
channels:
  global:
    name: "全服廣播"
    color: "<gradient:#00FFA3:#00B8FF>"
    prefix-key: "!"
    permission: ""
    description: "預設全伺服器公共聊天頻道"
    rules: "發言請遵守社群禮儀，禁止人身攻擊"

  trade:
    name: "商業交易"
    color: "<gradient:#FFE000:#799F0C>"
    prefix-key: "$"
    permission: "chatconduit.channel.trade"
    description: "玩家物品拍賣與買賣頻道"
    rules: "禁止重複刷屏洗版，違者禁言 30 分鐘"

  vip:
    name: "VIP 貴賓廳"
    color: "<gradient:#7F00FF:#E100FF>"
    prefix-key: "@"
    permission: "chatconduit.channel.vip"
    description: "VIP 贊助玩家專屬討論頻道"
    rules: "VIP 專屬交流天地"
```

---

## 🎨 格式說明與色彩 (MiniMessage Formatting)

ChatConduit 採用 Adventure **MiniMessage** 格式引擎：
- **漸變色彩**: `<gradient:#00FFA3:#00B8FF>文字</gradient>`
- **粗體/斜體**: `<bold>粗體</bold>`, `<italic>斜體</italic>`
- **點擊事件 (Hover & Click)**: 支援懸停顯示說明與點擊執行指令 Component！
