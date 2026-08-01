---
title: 資料庫與 Redis 跨服同步
description: MySQL (HikariCP 連線池)、SQLite 與 Redis Pub/Sub 跨服架構詳細配置說明
---

import { Aside } from '@astrojs/starlight/components';

ChatConduit 支援 **SQLite**（預設）與 **MySQL** 兩種資料庫，以及可選的 **Redis Pub/Sub** 跨服即時廣播。

---

## 資料庫設定 (database)

### SQLite（預設，單服推薦）

```yaml
database:
  type: "sqlite"
  sqlite-file: "storage.db"
```

無需額外配置，資料庫檔案自動建立於 `plugins/ChatConduit/storage.db`。

### MySQL（多服共用推薦）

```yaml
database:
  type: "mysql"
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

| 鍵 | 預設值 | 說明 |
| :--- | :--- | :--- |
| `host` | `"localhost"` | MySQL 伺服器位址 |
| `port` | `3306` | MySQL 連接埠 |
| `database` | `"chatconduit"` | 資料庫名稱（需預先建立） |
| `username` | `"root"` | 登入帳號 |
| `password` | `""` | 登入密碼 |
| `pool-size` | `10` | HikariCP 最大連線數 |
| `max-lifetime` | `1800000` | 連線最長存活時間（毫秒，= 30 分鐘） |
| `connection-timeout` | `10000` | 連線超時時間（毫秒，= 10 秒） |

<Aside type="note">
  所有 SQL 存取均透過 **HikariCP** 連線池以非同步方式執行，確保 0 阻塞主執行緒。
</Aside>

### 自動建表

啟動後插件會自動建立以下資料表：
- `friends` — 好友關係（UUID 雙向關聯）
- `friend_requests` — 待處理好友申請
- `friend_blocks` — 黑名單封鎖紀錄
- `player_settings` — 玩家個人偏好設定

---

## Redis 跨服設定 (redis)

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

<Aside type="caution">
  **多伺服器必讀**：啟用 Redis 後，每台子伺服器的 `config.yml` 必須設定**相同的 `redis.channel` 名稱**，否則跨服廣播將失效。
</Aside>

| 鍵 | 預設值 | 說明 |
| :--- | :--- | :--- |
| `enabled` | `false` | 是否啟用 Redis 跨服功能 |
| `host` | `"localhost"` | Redis 伺服器位址 |
| `port` | `6379` | Redis 連接埠 |
| `password` | `""` | Redis 驗證密碼（無密碼則留空） |
| `ssl` | `false` | 是否啟用 SSL/TLS 連線加密 |
| `channel` | `"chatconduit:global_chat"` | Pub/Sub 廣播管道名稱 |
| `max-connections` | `8` | Jedis 連線池最大連線數 |
| `timeout` | `2000` | 連線超時（毫秒） |

### Redis 同步內容

啟用後，以下事件將自動跨服同步：
- 玩家上線 / 離線狀態（好友通知）
- 跨服私訊 (`/msg`)
- 跨服頻道訊息廣播
- 跨服好友申請點擊 Component（接受 / 拒絕）
