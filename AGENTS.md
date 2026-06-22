# Lycohism — Agent 指南

> 這份是給 AI agent 在此 repo 工作的專案級規範。它**延伸並覆寫**全域 agent 規則；衝突時以「最新且最具體的指示」為準（使用者當下要求 > 本檔 > 全域規則）。

## 這是什麼

Lycohism 是**純伺服器端**（Paper）的 Minecraft 生存擴充插件，Kotlin 撰寫。核心：在原版生存上疊一層「觀察自然 → 採集自然現象 → 轉化成工具與設施 → 蓄輝能、蓋據點 → 遠征取素材 → 挑戰 BOSS」。不要求客戶端模組。

定位是 **TinyYana 個人玩法實驗**，不是大型商業服；口味判斷由 TinyYana 拍板，agent 負責實作與整理。

## 建置與驗證

```bash
./gradlew.bat test build --offline          # 編譯 + 單元測試 + 打包（離線吃快取）
./gradlew.bat clean build --offline          # 發布前用 clean，確保 plugin.yml 版號更新
```

- 可部署的 JAR：`build/libs/lycohism-<version>.jar`（shadowJar，已 bundle Kotlin stdlib；`*-plain.jar` 不要用）。
- **改完任何東西都要能編譯通過再交付**；非平凡邏輯留一個可跑的單元測試。
- **測試不能引用 Bukkit 類別**：`paper-api` 是 `compileOnly`，不在測試 runtime classpath。只測純函式（不碰 `Material`/`World`/… 的程式路徑）。需要可測時，把純邏輯抽成 Bukkit-free 的 seam（例：`parseRecipeSpec` 之於 `parseAutomationRecipe`）。

## 版本與發布

- 版號在 `gradle.properties` 的 `version`；`plugin.yml` 由 `processResources` 展開（增量建置可能吃舊快取，發布前 `clean build` 確認 jar 內版號）。
- 每個版本要更新 `HANDOFF.md`（最新版記在最上面）與 `docs/FEATURES.md`。
- **`HANDOFF.md` 與 `docs/PROJECT_PLAN.md` 被 `.gitignore` 刻意忽略**（本地開發筆記，不推 git）——可以照常編輯，但不要 `git add -f` 它們。

## 資料驅動設計

行為寫在程式、數值/文案放 YAML（`src/main/resources/`），改完 `/lyco reload` 即生效：

`config.yml`（輝能網路/設施升級/BOSS/自動化…）、`lang.yml`（所有玩家文本，MiniMessage 格式）、`phenomena.yml`、`tools.yml`、`facilities.yml`、`expeditions.yml`、`progression.yml`、`structures.yml`、`altars.yml`。

新增內容優先「加一段 YAML」而非改核心程式。

## 架構地圖

- 進入點 `Lycohism.kt`：所有 manager 在 `onEnable` 實例化、`reload()` 重載、`onDisable` 存檔。新 manager 照同一模式接上三處。
- 關鍵套件：`energy/`（輝能池/服務/塔/核心 Nexus/自動化）、`expedition/`、`facility/`（工房/書房/溫室）、`multiblock/`（剛性多方塊框架）、`phenomenon/`、`progression/`（調律之路）、`tool/`、`listener/`、`boss/`、`world/`、`util/`、`gui/`。
- 共用慣例：
  - **GUI**：用 `gui/Menu`（filler/header/back/`centeredRow` 置中）統一外觀；玩家文本一律走 `util/Messages`（MiniMessage）＋ `util/Texts`（lang.yml）。
  - **多方塊**：一份 `Multiblock` 模板同時供驗證/幽靈預覽/藍圖建造/秒放；`StructureActivation` 負責認領+名牌+破壞失效移除。
  - **登記表持久化**：記憶體權威 ＋ 定時 autosave ＋ 關服存（`nexuses.yml`/`towers.yml`/`automations.yml` 等），不存方塊 PDC。
  - **稽核**：所有產出/輝能變動/認領/掉落單點呼叫 `util/Audit`。

## 工作守則

- **一版一主題**：每個版本聚焦一個主題；不在同一版塞多個未驗證的大系統。大型新系統（能量網、多方塊、世界 BOSS、解謎…）**先在 `PROJECT_PLAN` 提案/細化成可動工規格，TinyYana 拍板後再建**，並「每完成一個可玩功能就停下回報」。
- **改動要外科手術式**：最小但正確的改動，放在程式結構該在的位置，別堆進 entrypoint。只移除自己造成的未使用 import/變數。
- **偷懶但不偷工**（ponytail）：能用 stdlib/原生/既有依賴就別自造；能一行別五十行；不做臆測性抽象與「之後可能用到」的彈性。但**輸入驗證、錯誤處理、安全、無障礙、明確要求的東西不可省**。刻意的簡化用 `// ponytail:` 註記，寫明上限與升級路徑。
- **不確定就講清楚**：列出假設與多種解讀，不要假裝確定；不要宣稱測過/建置過/部署過而其實沒有。
- **文風**：玩家文本（`lang.yml`）與對外文件用**繁體台式中文**；避免中國用語（數據/服務器/信息/默認/文件/界面…）與「不是 X 而是 Y」「賦能/升維」這類 AI 套路。UI 標籤保持精簡，不硬塞語氣。

## Git

- 僅在被要求時 commit/push。沿用既有 `vX.Y.Z update` 風格的 commit。
- 不要 commit 祕密、token、本機路徑；不要 commit 建置暫存（`build/`、`.tmp/`、`run/` 已忽略）。
- 不重寫歷史、不 force push，除非明確要求。

## 文件導覽

- `docs/FEATURES.md` — 目前做出來的完整玩法清單（對外、隨版本更新）。
- `docs/TUTORIAL.md` — 玩家向、講卡關處的輕量教學。
- `docs/PROJECT_PLAN.md` —（本地）長期企劃與各版規格、未來主題的可動工規格。
- `HANDOFF.md` —（本地）開發交接，最新版方向記在最上面。
