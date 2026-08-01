---
title: Redis Pub/Sub 封包協定規格
description: ChatConduit 跨服 JSON 封包結構說明與 Pub/Sub 廣播通道對接
---

ChatConduit 使用 Jedis 5.x 透過 Redis Channel 廣播 JSON 封包進行跨服資料通訊。

---

## 封包結構 (Packet Schemas)

### 1. `FriendStatusPacket` (好友連線/離線狀態)
```json
{
  "playerUuid": "e7b8a9c0-1234-5678-9abc-def012345678",
  "playerName": "Alex",
  "eventType": "JOIN",
  "serverId": "survival-1",
  "timestamp": 1772496000000
}
```

### 2. `FriendRequestNotifyPacket` (好友申請與動作通知)
```json
{
  "senderUuid": "e7b8a9c0-1234-5678-9abc-def012345678",
  "senderName": "Alex",
  "targetUuid": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "targetName": "Bob",
  "originServerId": "survival-1",
  "action": "SEND",
  "timestamp": 1772496000000
}
```

### 3. `PrivateMessagePacket` (跨服私訊)
```json
{
  "senderUuid": "e7b8a9c0-1234-5678-9abc-def012345678",
  "senderName": "Alex",
  "targetName": "Bob",
  "rawMessage": "你好，這是一條跨服私訊！",
  "originServerId": "survival-1",
  "timestamp": 1772496000000
}
```
