---
title: DiscordSRV 與 CMI AFK 整合
description: 雙向 Discord Webhook 連動、自訂頻道轉發與 CMI 掛機狀態過濾設置指南
---



---

## DiscordSRV 雙向整合

```yaml
discordsrv:
  enabled: true
  forward-custom-group-channels: false
  webhook:
    allow-custom-webhooks: true
    username-format: "%player% [%channel%]"
    avatar-url: "https://mc-heads.net/avatar/%player%/64"
  channel-mapping:
    global: "global"
    trade: "trade"
    party: "party"
    ask: "ask"
    chitchat: "chitchat"
    facility: "facility"
    lottery: "lottery"
```

### 頻道對應表 (channel-mapping)

`channel-mapping` 將遊戲內頻道 Key 對應至 Discord 文字頻道 ID 或頻道名稱：

```yaml
channel-mapping:
  global: "global"     # 遊戲「公共」頻道 -> Discord #global 頻道
  trade: "trade"       # 遊戲「交易」頻道 -> Discord #trade 頻道
```

:::tip
建議使用 Discord 頻道 **ID**（純數字）而非名稱，可避免頻道改名後失效。
:::

### Webhook 設定

| 鍵 | 預設值 | 說明 |
| :--- | :--- | :--- |
| `allow-custom-webhooks` | `true` | 是否允許頻道隊長設定自訂 Discord Webhook |
| `username-format` | `"%player% [%channel%]"` | Webhook 顯示名稱格式，`%player%` 為玩家名，`%channel%` 為頻道名 |
| `avatar-url` | `https://mc-heads.net/avatar/%player%/64` | 玩家皮膚頭像渲染 API（`%player%` 為玩家名，`%uuid%` 為 UUID） |
| `forward-custom-group-channels` | `false` | 是否將玩家自訂群組頻道訊息也轉發至 Discord（預設關閉以保護隱私） |

---

## CMI AFK 掛機監測

ChatConduit 監聽 CMI 插件的 `CMIPlayerAfkStatusChangeEvent`：

- 玩家進入 AFK 掛機時，自動更新好友選單的玩家狀態圖示
- 管理員可在 `config.yml` 的 `default-settings` 區塊，控制掛機玩家是否暫停接收系統廣播訊息

:::note
若伺服器未安裝 CMI，此功能將自動停用，不影響插件正常運作。
:::
