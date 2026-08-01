---
title: 🤝 好友與社交系統
description: 好友關係管理、Chest GUI Symbol Map 與黑名單機制
---

# 🤝 好友與社交系統 (Friend & Social System)

ChatConduit 擁有完善的好友社交管理系統，包含箱子 GUI 控制台、動態分頁與黑名單保護。

---

## 📜 1. 好友指令清單 (Command List)

| 指令 | 說明 |
| :--- | :--- |
| `/friend add <玩家>` | 發送好友申請（包含 5 秒防刷冷卻限制） |
| `/friend accept <玩家>` | 接受來自對方的好友申請 |
| `/friend deny <玩家>` | 拒絕來自對方的好友申請 |
| `/friend revoke <玩家>` | 撤回已發出的好友申請 |
| `/friend remove <玩家>` | 刪除好友關係（雙向自動解綁） |
| `/friend list [頁碼]` | 檢視好友清單（在線優先與伺服器識別展示） |
| `/friend block <玩家>` | 將目標加入黑名單（自動解綁好友並刪除申請） |
| `/friend unblock <玩家>` | 解除黑名單封鎖 |
| `/friend gui` (或 `/f`) | 開啟 Chest GUI 好友控制台 |

---

## 🧰 2. 箱子 GUI Symbol 佈局與配置 (`gui/friend_list.yml`)

ChatConduit 採用基於 **Symbol Map 符號地圖** 的 GUI 佈局設計，管理員可在 YAML 中任意排列按鈕位置：

```yaml
title: "<green><bold>好友列表 - 第 <page> / <total_pages> 頁</bold></green>"
size: 54

layout:
  - "#########"
  - "#FFFFFFF#"
  - "#FFFFFFF#"
  - "#FFFFFFF#"
  - "#########"
  - "B##P#N##A"

symbols:
  '#': "filler-glass"   # 邊框灰玻璃
  'F': "player-heads"   # 好友頭顱展示 Slot
  'B': "back-button"    # 返回主選單按鈕
  'P': "prev-page"      # 上一頁按鈕
  'N': "next-page"      # 下一頁按鈕
  'A': "add-friend"     # 新增好友按鈕
```

### ⚡ 動態 GUI 刷新
當好友連線或離線時，開著 GUI 視窗的玩家視窗將由 `GUIRefresher` 自動即時更新，無須手動關閉重新打開！

---

## 🛑 3. 黑名單與安全保護 (Blacklist Protection)

- **自動解綁**: 當玩家 A 將玩家 B 加入黑名單時，系統會自動強制刪除雙方的好友關係與未處理申請。
- **私訊與傳送屏蔽**: 被封鎖的玩家無法發送私訊，亦無法發起頻道邀請。
