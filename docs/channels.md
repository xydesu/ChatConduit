# 💬 頻道與聊天系統 (Channels & Private Messaging)

ChatConduit 提供了零指令前綴發言、官方系統頻道、玩家自建頻道與跨服私訊系統。

---

## 📢 1. 系統頻道配置 (`config.yml`)

管理員可以在 `config.yml` 的 `channels` 區塊自訂官方系統頻道：

```yaml
default-channel: "global"

channels:
  global:
    name: "全服廣播"
    color: "<gradient:#00FFA3:#00B8FF>"
    prefix-key: "!"
    permission: ""
    description: "預設全伺服器公共聊天頻道"
    rules: "發言請遵守社群禮儀"

  trade:
    name: "商業交易"
    color: "<gradient:#FFE000:#799F0C>"
    prefix-key: "$"
    permission: "chatconduit.channel.trade"
    description: "玩家物品拍賣與買賣頻道"
    rules: "禁止重複刷屏洗版"
```

### 發言前綴機制 (Zero-Command Switching)
- 當玩家輸入 `!大家好` 時，系統會自動辨識前綴 `!` 並將訊息發送至 `global` 頻道。
- 當玩家輸入 `$出售神劍` 時，訊息自動發送至 `trade` 頻道。
- 玩家亦可透過 `/channel switch <頻道 Key>` 切換預設發言頻道。

---

## 👥 2. 玩家自建群組頻道 (`/playerchannel`)

玩家可透過指令自建私人或密碼保護的專屬頻道：

| 指令 | 說明 |
| :--- | :--- |
| `/playerchannel create <ID> <名稱> [密碼]` | 建立新群組頻道 |
| `/playerchannel join <ID> [密碼]` | 加入指定的群組頻道 |
| `/playerchannel leave <ID>` | 離開群組頻道 |
| `/playerchannel invite <玩家>` | 邀請玩家加入目前群組（支援跨服即時推播）|
| `/playerchannel switch <ID>` | 切換目前發言至該群組頻道 |
| `/playerchannel gui` | 開啟群組頻道控制台 GUI |

---

## ✉️ 3. 私訊與回覆系統 (`/msg`, `/reply`)

ChatConduit 支援本地與 Redis 跨服私訊：

### 常用指令

* `/msg <玩家> <訊息>`（或 `/tell`, `/w`）：發送私訊給指定玩家。
* `/reply <訊息>`（或 `/r`）：快速回覆最後一位私訊對象。

### 格式範例
- **寄件者視角**: `[我 -> Alex(survival-1)] 你好！`
- **收件者視角**: `[Alex(survival-1) -> 我] 你好！`

### 🔒 隱私與保護
- 若收件者已將寄件者加入黑名單，系統將阻止私訊傳送並給予提示。
- 當發送者處於禁言狀態 (`/mute`) 時，私訊將被強制攔截。
