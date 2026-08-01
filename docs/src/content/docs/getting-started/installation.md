---
title: 📦 快速安裝指南 (Installation)
description: ChatConduit 系統需求、安裝步驟與 initial 設置說明
---

# 📦 快速安裝指南 (Installation)

---

## ⚙️ 系統與執行環境需求

| 組件名稱 | 最低需求 | 推薦版本 | 說明 |
| :--- | :--- | :--- | :--- |
| **Minecraft 核心** | Paper / Purpur 1.20.x | Paper 1.20.4+ | 支援 Mojang Mapping 核心 |
| **Java 執行環境** | Java 21 | **GraalVM JDK 25** | `C:\Users\xy\...\graalvm-jdk-25` |
| **資料庫** | SQLite (內建) | MySQL 8.0+ / MariaDB | 採用 HikariCP 連線池 |
| **跨服快取 (選填)** | Jedis 5.x | **Redis 6.x+** | 多服跨服必須啟用 Redis |

---

## 🚀 步驟 1: 下載與部署 Jar 檔

1. 下載最新版本的 `chatconduit-paper-1.0.jar`。
2. 將 Jar 檔放入目標伺服器的 `plugins/` 資料夾內。

---

## ⚙️ 步驟 2: 初次啟動與配置檔產生

1. 啟動伺服器，插件會自動創建 `plugins/ChatConduit/` 目錄並生成預設配置：
   - `config.yml` (核心頻道與 Redis / MySQL 設定)
   - `language.yml` (MiniMessage 語言文本)
   - `gui/` (箱子 GUI 佈局檔)

---

## 🔄 步驟 3: 驗證與重載指令

在遊戲內或 Server Console 執行：
```bash
/chatconduit reload
```
當看到 `[ChatConduit] 成功重載所有配置檔案與 GUI 介面！` 提示時即表示安裝順利完成！
