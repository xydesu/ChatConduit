# ChatConduit 單元測試模組報告 (Test Report)

本報告詳細記錄 `ChatConduit` 插件於 JUnit 5 測試框架下之**單元測試模組設計**、**測試案例範疇**與**執行驗證結果**。

---

## 📊 測試執行結果統計 (Test Execution Metrics)

| 測試類別 (Test Class) | 測試案例數 (Cases) | 成功 (Passed) | 失敗 (Failed) | 錯誤 (Errors) | 執行耗時 (Duration) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `PlayerChannelManagerTest` | 2 | 2 | 0 | 0 | 0.084s |
| `RedisPacketTest` | 2 | 2 | 0 | 0 | 0.047s |
| `WebhookManagerTest` | 2 | 2 | 0 | 0 | 0.160s |
| `ChatUtilsTest` | 2 | 2 | 0 | 0 | 0.122s |
| **總計 (Total)** | **8** | **8** | **0** | **0** | **0.413s** |

> [!NOTE]
> 測試結果：**100% 通過 (BUILD SUCCESS)**

---

## 🧪 測試模組與案例細節 (Test Cases Detail)

### 1. `PlayerChannelManagerTest`
> 測試類別路徑：`src/test/java/me/xydesu/chatconduit/channel/PlayerChannelManagerTest.java`

- **`testReservedKeywords`**
  - **測試目的**：驗證系統保留字名稱黑名單 (`isReservedKeyword`) 檢測。
  - **測試輸入**：`create`, `CREATE`, `global`, `admin`, `reload` 以及合法名稱 `myteam`, `pvp_squad`。
  - **驗證條件**：保留字回傳 `true`，合法名稱回傳 `false`。
- **`testCustomChannelMembers`**
  - **測試目的**：驗證玩家自訂群組頻道之成員 Set 操作與初始化隊長狀態。
  - **驗證條件**：建立頻道後自動新增隊長，成員新增/刪除後數量統計與 `contains` 檢測正確。

---

### 2. `RedisPacketTest`
> 測試類別路徑：`src/test/java/me/xydesu/chatconduit/redis/RedisPacketTest.java`

- **`testChatMessagePacketJsonSerialization`**
  - **測試目的**：驗證跨服聊天訊息封包 (`ChatMessagePacket`) 之 Gson JSON 序列化與反序列化。
  - **驗證條件**：序列化後欄位包含 `senderUuid`, `senderName`, `channelName`, `rawMessage`, `serverId`, `timestamp`，反序列化後物件內容與原封包完全一致。
- **`testChannelInvitePacketJsonSerialization`**
  - **測試目的**：驗證跨服頻道邀請封包 (`ChannelInvitePacket`) 之序列化與 Action (`INVITE`, `ACCEPT`, `REJECT`) 反序列化。
  - **驗證條件**：JSON 轉換過程無遺失，Action 列舉與字串欄位正確還原。

---

### 3. `WebhookManagerTest`
> 測試類別路徑：`src/test/java/me/xydesu/chatconduit/integration/WebhookManagerTest.java`

- **`testCleanInteractiveChatPlaceholders`**
  - **測試目的**：驗證 InteractiveChat 插件產生之內部未解析標籤（例如 `<chat=UUID:[item]:>` 或 `<chat=UUID:[ping]:>`）淨化正則表達式。
  - **驗證條件**：成功將未解析標籤轉為乾淨的 `[item]` / `[ping]` 文字，防止 Discord Webhook 內容混亂。
- **`testRemoveCooldown`**
  - **測試目的**：驗證頻道解散時呼叫 `removeCooldown` 移除速率限制 Map 快取，防範 `null` 值拋出例外。

---

### 4. `ChatUtilsTest`
> 測試類別路徑：`src/test/java/me/xydesu/chatconduit/util/ChatUtilsTest.java`

- **`testParseLegacy`**
  - **測試目的**：驗證舊版 Spigot/Legacy 顏色碼 (`&a`, `&c`, `&f`) 轉換為 Adventure Component 之 PlainText 序列化結果。
  - **驗證條件**：去除顏色碼後純文字與原文字比對正確。
- **`testParseNoItalic`**
  - **測試目的**：驗證 MiniMessage 格式化字串並移除 Minecraft 物品預設斜體 (`TextDecoration.ITALIC`) 之解析效果。
  - **驗證條件**：成功解析 `<green>Test Message` 且 Component 元件結構無誤。

---

## 💻 執行測試命令 (Run Tests Command)

若要在本機開發環境重新執行單元測試，請使用以下 Maven 命令：

```powershell
$env:JAVA_HOME="C:\Users\xy\AppData\Roaming\PrismLauncher\java\graalvm-jdk-25+37.1"; mvn test
```
