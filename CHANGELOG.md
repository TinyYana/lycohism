# Changelog

All notable changes to Lycohism are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.0.0-BETA] — 2026-06-26

### Breaking / Infrastructure
- **Spigot compatibility**: compile target switched from Paper API to Spigot API.
  The plugin now runs on Spigot 1.21+ as well as Paper. Paper-only APIs replaced with Spigot-compatible equivalents throughout.
- **Adventure removed**: `net.kyori.adventure` / `MiniMessage` library dependency dropped.
  Replaced by a zero-dependency custom parser (see below).

### Added
- **`util/MiniText`** — custom MiniMessage→§ parser, fully Bukkit-free and unit-tested:
  - Named colour tags: `<aqua>`, `<gold>`, `<dark_gray>`, … (all 16 Minecraft colours)
  - Format tags: `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`
  - `<reset>` / any `</closing-tag>` → `§r`
  - **Gradient**: `<gradient:stop1:stop2[:stop3…]>text</gradient>` — multi-stop linear interpolation, emits Spigot 1.16+ hex `§x§R§R§G§G§B§B` per character; skips embedded `§`-codes when stepping
  - Backward-compatible with legacy `&x` codes (existing lang values continue to work unchanged)
- **Spigot shims** in `util/Items`:
  - `ItemStack.modifyMeta {}` / `ItemStack.modifyMeta<T>(class) {}`
  - `Location.toCenterLocation()`
  - `World.getNearbyPlayers(location, radius)`

### Changed
- `util/Messages`: replaced `MiniMessage.miniMessage().deserialize()` with `MiniText.parse()`; action bar uses BungeeCord `TextComponent` instead of `Component`
- `lang_zh.yml` / `lang_en.yml`: all `&x` legacy codes converted to MiniMessage tag syntax; format comment updated to reflect the custom parser
- `docs/FEATURES.md` / `docs/FEATURES.zh-TW.md`: version header updated to v1.0.0-BETA

---

## [0.9.34-ALPHA] — prior

See commit `49f27a8` — onboarding pass (initial public alpha).
