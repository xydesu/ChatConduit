---
title: 👑 資料庫與 Redis 跨服同步
description: MySQL (HikariCP 連線池)、SQLite 與 Redis Pub/Sub 跨服架構詳細配置說明
---

ChatConduit 專為高併發跨服與大型伺服器設計，支援 **MySQL / SQLite** 非同步持久化存取與 **Redis Pub/Sub** 即時跨服訊息廣播。

---

## 🗄️ 1. 資料庫配置 (`config.yml`)

```yaml
database:
  # 儲存類型：SQLITE 或 MYSQL
  type: "MYSQL"
  
  host: "127.0.0.1"
  port: 3306
  database: "chatconduit_db"
  username: "root"
  password: "secure_password"

  # HikariCP 連線池優化參數
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

### 📊 自動建表 Schema (Auto DDL)
- `friends`: 好友關係表 (UUID 雙向關聯與建立時間)
- `friend_requests`: 待處理好友申請表
- `friend_blocks`: 黑名單封鎖紀錄表
- `player_settings`: 玩家頻道選擇與發言習慣設定表

---

## 📡 2. Redis 跨服 Pub/Sub 配置

在 `config.yml` 啟用 Redis 跨服設定後，所有群服子伺服器將進行狀態同步：

```yaml
redis:
  enabled: true
  host: "127.0.0.1"
  port: 6379
  password: ""
  channel: "chatconduit:pubsub"
  server-id: "survival-1"  # 唯一的子伺服器標示 Key
```

### ⚡ 跨服同步內容
* **在線玩家名冊註冊**: 全服跨服在線名冊 (`online_players`)
* **跨服好友狀態事件**: 玩家切換伺服器或上下線時即時廣播通知
* **跨服私訊與回覆**: 異服私訊與對接 (`/msg`)
* **跨服好友申請點擊 Component**: 跨服畫面直覺點擊 `[接受]` / `[拒絕]`
