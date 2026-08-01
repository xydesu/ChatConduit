---
title: config.yml 完整參數參考
description: ChatConduit 主設定檔所有選項的詳細說明與預設值對照
---

import { Aside, Code } from '@astrojs/starlight/components';

本頁列出 `plugins/ChatConduit/config.yml` 的所有可用設定項目、預設值與說明。

---

## server-id

```yaml
server-id: "survival-1"
```

| 鍵 | 類型 | 預設值 |
| :--- | :--- | :--- |
| `server-id` | String | `"survival-1"` |

跨服模式下識別目前子伺服器的唯一名稱。此名稱會顯示於聊天訊息的 `{server}` 佔位符。若僅單服使用，保留預設或清空皆可。

---

## chat-format

```yaml
chat-format: "<white><channel_prefix> <dark_gray>[<gray>{server}<dark_gray>] <gray>[%luckperms_prefix%<gray>] <white><player>> <white><message>"
```

全服統一的聊天訊息格式，支援以下佔位符：

| 佔位符 | 說明 |
| :--- | :--- |
| `<channel_prefix>` | 頻道的前綴顯示文字 |
| `{server}` | 玩家所在子伺服器 ID（跨服時顯示） |
| `%luckperms_prefix%` | LuckPerms 群組前綴（需安裝 PlaceholderAPI） |
| `<player>` | 發言玩家名稱 |
| `<message>` | 發言內容 |

<Aside type="tip">
  若非跨服訊息或 `server-id` 為空，`{server}` 欄位會自動隱藏，不會顯示空括號。
</Aside>

---

## default-settings

```yaml
default-settings:
  death-messages-enabled: true
  join-messages-enabled: true
```

玩家第一次加入時的訊息顯示預設狀態。玩家可透過 GUI 偏好設定自行切換。

| 鍵 | 類型 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `death-messages-enabled` | Boolean | `true` | 是否預設顯示死亡廣播訊息 |
| `join-messages-enabled` | Boolean | `true` | 是否預設顯示玩家進出伺服器廣播 |

---

## private-message

```yaml
private-message:
  enabled: true
  sender-format: "<gray>[<green>我</green> -> <yellow>{target}</yellow><dark_gray>(<aqua>{target_server}</aqua>)</dark_gray>] <white>{message}"
  receiver-format: "<gray>[<yellow>{sender}</yellow><dark_gray>(<aqua>{sender_server}</aqua>)</dark_gray> -> <green>我</green>] <white>{message}"
```

私人訊息 (`/msg`, `/tell`, `/r`) 的格式設定。

| 鍵 | 說明 |
| :--- | :--- |
| `enabled` | 是否啟用私訊功能 |
| `sender-format` | 發送方看到的格式，支援 `{target}`, `{target_server}`, `{message}` |
| `receiver-format` | 接收方看到的格式，支援 `{sender}`, `{sender_server}`, `{message}` |

---

## player-channels

```yaml
player-channels:
  max-per-player: 3
  session-timeout-seconds: 45
  name-min-length: 1
  name-max-length: 20
  description-max-length: 60
  rules-max-length: 60
```

玩家自建群組頻道 (`/playerchannel`) 的安全與數量限制。

| 鍵 | 類型 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `max-per-player` | Integer | `3` | 單一玩家最多可建立的群組頻道數量 |
| `session-timeout-seconds` | Integer | `45` | GUI 輸入對話框的超時秒數 |
| `name-min-length` | Integer | `1` | 頻道名稱最少字元數 |
| `name-max-length` | Integer | `20` | 頻道名稱最多字元數 |
| `description-max-length` | Integer | `60` | 頻道簡介最多字元數 |
| `rules-max-length` | Integer | `60` | 頻道守則最多字元數 |

---

## database

```yaml
database:
  type: "sqlite"
  sqlite-file: "storage.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "chatconduit"
    username: "root"
    password: ""
    pool-size: 10
    max-lifetime: 1800000
    connection-timeout: 10000
```

| 鍵 | 類型 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `type` | String | `"sqlite"` | 資料庫類型，可選 `"sqlite"` 或 `"mysql"` |
| `sqlite-file` | String | `"storage.db"` | SQLite 資料庫檔名（位於 `plugins/ChatConduit/`） |
| `mysql.host` | String | `"localhost"` | MySQL 伺服器位址 |
| `mysql.port` | Integer | `3306` | MySQL 連接埠 |
| `mysql.database` | String | `"chatconduit"` | 資料庫名稱 |
| `mysql.username` | String | `"root"` | 登入帳號 |
| `mysql.password` | String | `""` | 登入密碼 |
| `mysql.pool-size` | Integer | `10` | HikariCP 最大連線數 |
| `mysql.max-lifetime` | Integer | `1800000` | 連線最長存活時間（毫秒，預設 30 分鐘） |
| `mysql.connection-timeout` | Integer | `10000` | 連線超時時間（毫秒，預設 10 秒） |

<Aside type="note">
  單服環境建議使用 `sqlite`，無需額外配置。多子服跨服環境才需要切換至 `mysql` 共用資料庫。
</Aside>

---

## redis

```yaml
redis:
  enabled: false
  host: "localhost"
  port: 6379
  password: ""
  ssl: false
  channel: "chatconduit:global_chat"
  max-connections: 8
  timeout: 2000
```

| 鍵 | 類型 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `enabled` | Boolean | `false` | 是否啟用 Redis 跨服廣播 |
| `host` | String | `"localhost"` | Redis 伺服器位址 |
| `port` | Integer | `6379` | Redis 連接埠 |
| `password` | String | `""` | Redis 驗證密碼（無密碼則留空） |
| `ssl` | Boolean | `false` | 是否啟用 SSL/TLS 加密連線 |
| `channel` | String | `"chatconduit:global_chat"` | Redis Pub/Sub 廣播管道名稱 |
| `max-connections` | Integer | `8` | Jedis 連線池最大連線數 |
| `timeout` | Integer | `2000` | 連線超時時間（毫秒） |

<Aside type="caution">
  啟用 Redis 後，**所有子伺服器的 `config.yml` 必須設定相同的 `redis.channel` 名稱**，才能正確廣播跨服訊息。
</Aside>

---

## interactivechat

```yaml
interactivechat:
  enabled: true
  placeholders:
    ping: "&f%player_colored_ping% &bms"
    inv: "&b[&f%player_name%'s Inventory&b]"
    inventory: "&b[&f%player_name%'s Inventory&b]"
    ender: "&d[&f%player_name%'s Ender Chest&d]"
    ec: "&d[&f%player_name%'s Ender Chest&d]"
    money: "&e[&f%player_name%'s Balance&e]"
    balance: "&e[&f%player_name%'s Balance&e]"
```

InteractiveChat 插件整合設定，自訂聊天欄內可點擊的佔位符格式。

| 鍵 | 觸發詞 | 說明 |
| :--- | :--- | :--- |
| `ping` | `[ping]` | 顯示玩家延遲數值 |
| `inv` / `inventory` | `[inv]` | 顯示玩家背包內容（可點擊查看） |
| `ender` / `ec` | `[ender]` | 顯示玩家末影箱內容 |
| `money` / `balance` | `[money]` | 顯示玩家目前餘額 |

---

## discordsrv

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

| 鍵 | 類型 | 預設值 | 說明 |
| :--- | :--- | :--- | :--- |
| `enabled` | Boolean | `true` | 是否啟用 DiscordSRV 雙向整合 |
| `forward-custom-group-channels` | Boolean | `false` | 是否將玩家自訂群組頻道訊息轉發至 Discord |
| `webhook.allow-custom-webhooks` | Boolean | `true` | 是否允許頻道隊長設定自訂 Webhook |
| `webhook.username-format` | String | `"%player% [%channel%]"` | Webhook 機器人名稱格式 |
| `webhook.avatar-url` | String | mc-heads.net | 玩家皮膚頭像渲染服務 URL |
| `channel-mapping` | Map | 見上 | 遊戲頻道 Key 對應 Discord 頻道 ID 或名稱 |

---

## channels

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

每個頻道區塊的可用設定欄位：

| 欄位 | 類型 | 說明 |
| :--- | :--- | :--- |
| `name` | String | 頻道顯示名稱（用於 GUI 與聊天格式） |
| `color` | String | MiniMessage 色彩格式（支援漸變 `<gradient:#HEX:#HEX>`） |
| `prefix-key` | String | 聊天欄發言前綴符號（輸入此符號 + 訊息即發言至該頻道） |
| `permission` | String | 進入頻道所需的 LuckPerms 權限節點（留空表示所有人可用） |
| `description` | String | 頻道描述文字（顯示於 GUI 選單說明） |
| `rules` | String | 頻道守則文字（顯示於 GUI 頻道資訊頁） |

### 預設頻道清單

| 頻道 Key | 名稱 | 前綴符號 | 說明 |
| :--- | :--- | :--- | :--- |
| `global` | 公共 | `!` | 全服公共對話 |
| `trade` | 交易 | `$` | 物品買賣交流 |
| `party` | 組隊 | `+` | 副本招募與冒險 |
| `ask` | 詢問 | `?` | 新手發問求助 |
| `chitchat` | 閒聊 | `~` | 日常輕鬆閒聊 |
| `facility` | 設施 | `@` | 公共建設討論 |
| `lottery` | 抽獎 | `*` | 活動抽獎廣播 |
