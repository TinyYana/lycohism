# Lycohism

*[繁體中文版 README](README.zh-TW.md)*

A server-side survival expansion for Minecraft (Spigot / Paper). No client mod required — it grows a layer of "nature magic / living tools / base growth / exploration" on top of vanilla survival.

Lycohism is currently **BETA** at `1.0.0-BETA`; the full loop is playable, with tuning still expected from real servers.

## What it does

You play vanilla survival as usual — chop trees, mine, build a home — and gradually notice the world has gained a layer of **natural phenomena**: morning grass, riversides in the rain, the full-moon night sky, high mountains, caves. Each responds with a different material. Collect them to repair base facilities, craft living tools that smooth out survival, and open the next path along the way.

The whole design guards against one thing: becoming "tech gadgets with a magic name". So everything is tied to some vanilla behaviour — you have to actually do the right thing, at the right time, in the right place, for a phenomenon to appear. Observation itself is part of the play.

For the full feature set, exact costs and current limits, see [`docs/FEATURES.md`](docs/FEATURES.md); for a first-time walkthrough, see [`docs/TUTORIAL.md`](docs/TUTORIAL.md).

## What's in it now

- **Natural phenomena**: Morning Dew, Rain Breath, Moon Dew, Wind Trace, Leyline Sand, Flower Vein — plus the expedition / Nether / BOSS lines' Moss Bloom, Ember Bloom, Radiant Ore, Sun/Moon Cores and Eclipse Crystal. All data-driven (`phenomena.yml`).
- **Living tools**: Dewlight Vial, Flower Bookmark, Stonework Hammer, Wind Vane, Moon Pouch... a dozen-plus, all crafted in the Workshop / Study / Greenhouse menus. Each has a one-sentence purpose; if it can't be explained that cleanly, it doesn't go in.
- **Base facilities**: Workshop, Study, Greenhouse. Repairing one gets it to Lv1; Lv2 needs an upgrade structure and Nexus radiance; Lv3 needs an Eclipse Crystal from defeating a BOSS.
- **Radiance system (sun & moon)**: stand under the open sky to gather Sunlight by day and Moonlight by night; when your pool fills, the overflow spills into Energy Crystals in your inventory. Sun/Moon Towers produce → Relays carry hop by hop → the Radiance Nexus stores → you spend it upgrading facilities.
- **Base automation**: the Attunement Engine, Seedling Cultivator and Phenomenon Condenser hook into a Nexus, pull inputs from the container directly above, and process them by spending radiance. The three currently share one recipe pool — playable first, split finer later if testing calls for it.
- **Expedition worlds**: Rainfall Forest, Moonless Waste, Tidal Depths and Eclipse Realm — running from plant returns and Sun/Moon Cores all the way to a mini-boss and the Eclipse Warden.
- **Exploration side-paths**: the Overworld has Sealed Shrines unlocked by progression + an Energy Crystal (key blocks are protected from being broken before the shrine is solved); the Nether has ember-line mob drops and claimable Infernal Ruins.
- **Path of Attunement**: a progression line mapped onto vanilla advancements that points you roughly toward the next step — without laying everything ahead bare, leaving room to explore. Type `/lyco progress` any time to see your current objective.
- **First-join onboarding**: a short hint tells new players about Morning Dew, the Attunement Manual and how to open facilities — shown once, then never again.

## How to run it

Requires Spigot or Paper matching the `api-version` in `plugin.yml`.

Build:

```bash
./gradlew.bat build   # Windows
./gradlew build       # other platforms
```

The JAR lands at `build/libs/lycohism-<version>.jar`; drop it into your server's `plugins/`.

First thing in-game that trips people up: **sneak + empty hand + right-click a crafting table** opens the Workshop (empty hand is deliberate, otherwise it would steal vanilla's place-block action). The Study maps to bookshelves, the Greenhouse to flower pots — same gesture. For everything else, `/lyco help`.

To walk the full playthrough, see [`docs/TUTORIAL.md`](docs/TUTORIAL.md).

## Language

Player-facing text ships in **Traditional Chinese and English**. The active language follows config.yml `language` (`auto` | `zh` | `en`). On `auto`, it follows the server's system locale — a Chinese-region host defaults to Traditional Chinese, everyone else to English. Edit `lang_zh.yml` / `lang_en.yml` and `/lyco reload` to customise.

## Status

`v1.0.0-BETA`. The main play loop is in place, Spigot is supported, and **each release is tested in-game during development**. It still needs more long-term multiplayer mileage, and most content is data-driven so tuning can keep moving. There will be bugs — saying so up front.

## License

Released under the **TinyYana Universal Software License (TYUSL) v1.0** — see [`LICENSE`](LICENSE). In short: free to use and modify, but you may not claim the plugin as your own work, nor make it the core of a monetized product/service, without TinyYana's prior written permission.
