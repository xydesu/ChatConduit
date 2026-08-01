---
title: 👑 Chest GUI Symbol Map 佈局指南
description: 解耦的箱子 GUI 符號佈局設計機制、按鈕綁定與動態分頁動態解析說明
---

# 👑 Chest GUI Symbol Map 佈局指南

ChatConduit 引進了獨創的 **Symbol Map 符號地圖解耦** 機制。管理員無需修改任何 Java 原始碼或計算 Slot 索引，即可在 YAML 配置檔中自由設計選單。

---

## 🧰 佈局範例 (`gui/friend_list.yml`)

```yaml
title: "<green><bold>好友列表 - 第 <page> / <total_pages> 頁</bold></green>"
size: 54

# 9 欄 x 6 列 (共 54 槽位) 字符圖案設計
layout:
  - "#########"
  - "#FFFFFFF#"
  - "#FFFFFFF#"
  - "#FFFFFFF#"
  - "#########"
  - "B##P#N##A"

# 符號與元件功能綁定表
symbols:
  '#':
    material: "GRAY_STAINED_GLASS_PANE"
    name: "<gray> </gray>"
    action: "NONE"

  'F':
    action: "FRIEND_ITEM"  # 好友頭顱動態填充區

  'B':
    material: "BARRIER"
    name: "<red><bold>返回主選單</bold></red>"
    action: "OPEN_MAIN_GUI"

  'P':
    material: "ARROW"
    name: "<yellow>上一頁</yellow>"
    action: "PREV_PAGE"

  'N':
    material: "ARROW"
    name: "<yellow>下一頁</yellow>"
    action: "NEXT_PAGE"

  'A':
    material: "EMERALD"
    name: "<green><bold>新增好友</bold></green>"
    action: "ADD_FRIEND"
```

---

## ⚡ Slot 槽位計算與 Symbol 解析原理

`GUIManager` 讀取 `layout` 陣列時，自動依循 `slot = row * 9 + col` 將符號對映至箱子槽位：
- 第一行 `layout[0]`（字元 0~8）對應 Slot 0~8。
- 符號 `'F'` 出現的所有槽位皆會被自動解析為 `FriendList` 動態填充 Slot。
- 支援於選單開啟狀態下，由 `GUIRefresher` 觸發無縫 UI 刷新，玩家畫面不閃爍關閉！
