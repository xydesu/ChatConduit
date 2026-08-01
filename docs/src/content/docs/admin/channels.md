---
title: 系統頻道與前綴配置
description: 管理員如何設定 config.yml 官方系統頻道、MiniMessage 色彩與零指令發言前綴
---

管理員可以在 `plugins/ChatConduit/config.yml` 中的 `channels:` 區塊定義任意數量的官方系統頻道。

---

## 預設頻道一覽

ChatConduit 開箱即內建 7 個系統頻道：

| 頻道 Key | 顯示名稱 | 前綴符號 | 說明 |
| :--- | :--- | :--- | :--- |
| `global` | 公共 | `!` | 全服公共對話，預設所有玩家可見 |
| `trade` | 交易 | `$` | 物品、資源與裝備買賣交流 |
| `party` | 組隊 | `+` | 副本招募與冒險團隊溝通 |
| `ask` | 詢問 | `?` | 新手發問與遊戲疑難解答 |
| `chitchat` | 閒聊 | `~` | 輕鬆日常閒聊 |
| `facility` | 設施 | `@` | 公共建設與紅石討論 |
| `lottery` | 抽獎 | `*` | 伺服器活動抽獎廣播 |

---

## 頻道設定欄位說明

```yaml
channels:
  global:
    name: "公共"
    color: "<gradient:#00d2ff:#3a7bd5>"
    prefix-key: "!"
    permission: ""
    description: "全服公共玩家對話頻道，交流與共享伺服器資訊。"
    rules: "聊天請遵循禮貌，禁止刷屏洗版、人身攻擊或散布廣告。"
```

| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| `name` | String | 頻道顯示名稱（用於 GUI 圖示與訊息前綴） |
| `color` | String | MiniMessage 格式色彩（支援 `<gradient:#HEX:#HEX>`） |
| `prefix-key` | String | 聊天欄前綴符號，玩家輸入此符號即切換到該頻道發言 |
| `permission` | String | 進入頻道所需 LuckPerms 權限節點（留空則所有人可用） |
| `description` | String | 頻道描述，顯示於 GUI 選單的說明文字 |
| `rules` | String | 頻道守則，顯示於 GUI 頻道資訊頁面 |

---

## 新增自訂頻道

在 `channels:` 下新增任意 Key 即可建立新頻道：

```yaml
channels:
  vip:
    name: "VIP"
    color: "<gold>"
    prefix-key: "#"
    permission: "chatconduit.channel.vip"
    description: "VIP 贊助玩家專屬頻道"
    rules: "請尊重彼此，保持頻道品質。"
```

修改完成後執行 `/chatconduit reload` 即可立即套用，無需重啟。

---

## 聊天格式模板 (chat-format)

頻道訊息的整體排版由 `chat-format` 控制：

```yaml
chat-format: "<white><channel_prefix> <dark_gray>[<gray>{server}<dark_gray>] <gray>[%luckperms_prefix%<gray>] <white><player>> <white><message>"
```

若為單服環境，`{server}` 欄位將自動省略，不會顯示空括號。
