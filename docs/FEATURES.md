# Lycohism — Features & Mechanics Overview (v1.0.0-BETA)

*[繁體中文版](FEATURES.zh-TW.md)*

> This is a thorough list and explanation of "everything built so far". For reference, see also:
> - the lighter, player-facing tutorial focused on "where people get stuck" → [`TUTORIAL.md`](TUTORIAL.md)
>
> Most numbers live in `src/main/resources/*.yml` and can be `/lyco reload`ed live. Anything marked "tunable" here is in a config file.
> This document reflects the current working tree's **v0.9.34-ALPHA WIP code and default YAML**. Each release is tested in-game during development, but passing compile/unit tests and solo testing is not the same as a long-term multiplayer Paper acceptance run.

---

## 0. One-line positioning

Lycohism is a **server-side** survival expansion: no client mod, just a layer of "nature magic / living tools / base growth / exploration" laid over vanilla survival. The core rhythm is:

> **Observe nature → harvest phenomena → turn them into tools and facilities → store radiance, build a base → expedition for exclusive materials → challenge a BOSS → push the base to its peak.**

Every session can finish one small step forward, with the "Path of Attunement" pointing you to the next.

---

## 1. Natural phenomena (the harvesting system)

The world gains a layer of drops that "respond to time / weather / moon phase / place". Break the matching block while the environment condition holds, and there's a chance to get it. All data lives in `phenomena.yml`; adding one is just a YAML block.

| Phenomenon | Main acquisition condition | Use direction |
|---|---|---|
| **Morning Dew** morning_dew | Dawn, vegetation (grass, flowers) | The earliest all-purpose material; repair the workshop, make early tools |
| **Rain Breath** rain_breath | Rain, near water | Rain Bandage, crop tending, etc. |
| **Flower Vein** flower_vein | Flower clusters, cherry groves | Greenhouse cultivation, Flower Vein Shears |
| **Moon Dew** moon_dew | Night, full moon, high up | Moon-phase tools, crafting |
| **Wind Trace** wind_trace | Mountains, high up | Wind Vane, Star Compass |
| **Leyline Sand** leyline_sand | Caves, deep | Leyline Probe |
| **Moss Bloom** moss_bloom | Rainfall Forest (expedition) exclusive | Greenhouse-exclusive products, restorative tools |
| **Ember Bloom** ember_bloom | Nether (harvest / mob drops) | Right-click to shatter = 30s Fire Resistance |
| **Infernal Shard** infernal_shard | Nether mob drops, or refined at the altar | Nether material line; currently used to open follow-up content |
| **Radiant Ore** radiant_ore | Nether, gilded blackstone | Core mineral for tier-2 tools and altar creations |
| **Moon Core** moon_core | Moonless Waste (expedition) exclusive | Radiant Focus (moon power), one half of the BOSS summon |
| **Sun Core** sun_core | Tidal Depths (expedition) exclusive | The other half of the BOSS summon (sun line mirrors moon line) |
| **Eclipse Crystal** eclipse_crystal | Eclipse Warden BOSS drop | The only key to tier-3 facilities |

Mechanic highlights:
- **No memorising**: while holding certain tools (Wind Vane, Leyline Probe), the screen tells you in real time "what's harvestable here and now".
- **Multiplayer-fair**: expedition-exclusive cores can also be obtained by **killing mobs** (`mob-drops`), not just harvesting/chests, so you don't have to fight over loot.
- **First-time hint**: the first time you harvest each phenomenon, a one-line first-hint pops up.
- **Materials are usable themselves**: hold Rain Breath and right-click a crop to push 2 growth stages; Ember Bloom right-click consumes 1 for 30s Fire Resistance.

### Default harvest conditions and rates

Rates are rolled independently each time you break a matching source block; servers can tune them in `phenomena.yml`.

| id | Exact condition | Default rate | Source blocks |
|---|---|---:|---|
| `morning_dew` | Overworld, 0–3000 ticks | 20% | Short/tall grass, fern, large fern, dandelion, poppy, cornflower, oxeye daisy, daisy |
| `rain_breath` | Overworld, raining | 18% | Sugar cane, lily pad, vines, moss, seagrass |
| `flower_vein` | Overworld, any time | 18% | Pink petals, cherry leaves, flowering azalea leaves, orchids, allium, lily of the valley, tulips, wither rose |
| `moon_dew` | Overworld, 13000–23000 ticks, clear, full moon (phase 0) | 16% | Pink petals, cherry leaves, flowering azalea leaves, daisy, lily of the valley, orchids |
| `wind_trace` | Overworld, Y=96–320 | 12% | Grass block, stone, andesite, gravel, snow, snow block |
| `leyline_sand` | Overworld, Y=-64–32 | 8% | Stone, deepslate, tuff, calcite, dripstone block |
| `moss_bloom` | Rainfall Forest only | 35% | Moss, mangrove leaves/roots, azalea leaves, big/small dripleaf |
| `ember_bloom` | Nether, completed `enter_the_nether` | 30% | Nether wart block, warped wart block, shroomlight, both fungi and vines; also Nether mob drops |
| `infernal_shard` | Nether, completed `enter_the_nether` | no block source | Blaze 25%, ghast 20%, wither skeleton 10%; also refined at the altar |
| `radiant_ore` | Nether, completed `enter_the_nether` | 100% | Gilded blackstone veins the plugin generates in new chunks |
| `moon_core` | Moonless Waste only | 25% | Dark oak leaves, glow lichen, sculk, mushrooms; also mob drops |
| `sun_core` | Tidal Depths only | 25% | Prismarine, sea lantern, kelp, coral; also mob drops and shrine chests |
| `eclipse_crystal` | no natural source | — | Obtained by defeating the Eclipse Warden |

---

## 2. Tools (living tools)

Tool behaviour is in code, balance numbers in `tools.yml`, names and lore in `lang_<lang>.yml`. Most are crafted from materials in a facility GUI; right-click to activate, main hand only (to avoid off-hand misfires).

### Harvest / observation
- **Dewlight Vial** dew_light: consumable, right-click for a stretch of night vision, leaves an empty bottle. Nicer than placing torches when mining.
- **Wind Vane** wind_vane: right-click to read the current time / weather / moon phase, hinting at what's harvestable now.
- **Leyline Probe** leyline_probe: short-range scan of nearby vanilla ore veins, with a persistent action-bar readout (doesn't replace normal prospecting).
- **Flower Bookmark** flower_bookmark: sneak+right-click to remember a place, right-click to be guided to its direction and distance.
- **Star Compass** star_compass: right-click to read the direction and distance of the nearest energy tower (Study Lv1).
- **Attunement Manual** tuning_manual: right-click to open "the next small step".
  - One is auto-granted the first time you harvest a phenomenon; once the study is repaired you can read it there directly.

### Building
- **Stonework Hammer** stonework_hammer: right-click a stone block to cycle its variant in place (cobble→stone→stone bricks→chiseled), spending durability. **Preview on first right-click, confirm on the second**, with a short cooldown (a damped feel, anti-spam). Tier II shapes a connected 3×3 face at once.
- **Building Wand** building_wand: extends placement in a line along the face you click; consumes the same vanilla blocks from your inventory. Tier II has **modes** (line / wall / floor / column), switched with **sneak+right-click (even into the air)**; same preview→confirm + cooldown.
- **Blueprint** blueprint: right-click for a translucent preview of a multiblock structure; sneak+right-click to build it instantly using materials.

### Plants / restoration
- **Flower Vein Shears** flower_vein_shears: right-click an immature crop = ripen, a mature crop = harvest and replant; ripening costs more durability than harvesting.
- **Moss Fertilizer** moss_fertile: right-click a plant to bone-meal a small area (vanilla growth rules).
- **Moss Balm** moss_balm: consumable, right-click for a stretch of gentle Regeneration (distinct from the bandage's instant small heal).
- **Rain Bandage** rain_bandage: consumable, right-click to restore a little health; doesn't replace food and potions.

### Radiance / late-game
- **Energy Crystal** energy_crystal: a portable radiance battery; when your pool overflows it auto-fills the crystal in your inventory, and right-click pours the crystal's energy back into your pool.
- **Radiant Focus** radiant_focus: right-click spends Sunlight (Fire Resistance + Strength), sneak+right-click spends Moonlight (Night Vision + Slow Falling). Needs a Moon Core, forged at the Radiance Altar.
- **Solar Pickaxe** solar_pick: right-click spends Sunlight for a stretch of Haste (mining speed). Workshop Lv2.
- **Lunar Spore** lunar_spore: right-click a plant to spend Moonlight and bone-meal an area. Greenhouse Lv2.

### Storage / movement
- **Moon Pouch** moon_pouch: auto-stores natural phenomena you pick up, right-click to release all at once.
- **Rainmark Gate** rain_gate: right-click in the Overworld to travel to the first expedition, right-click in the expedition world to return; reusable. **Currently admin-given only; the normal player route is in and out via the study's expedition page.**

> Tools generally have a "craft = unlock" discovery record that advances the Path of Attunement.

### Default operation numbers

- Dewlight Vial: night vision 120s; Rain Bandage: instantly restores 4 health (2 hearts); Moss Balm: Regeneration II 10s.
- Moss Fertilizer: 3×3 area; Lunar Spore: spends 25 Moonlight, 5×5 area; Flower Vein Shears ripening costs 2 durability.
- Leyline Probe: 8 blocks horizontal, 12 vertical, rescans every 3s; Moon Pouch capacity 256 phenomenon materials.
- Solar Pickaxe: spends 25 Sunlight, Haste II 60s; Radiant Focus sun/moon modes each spend 30, main effect 8 minutes, moon mode Slow Falling 1 minute.
- Building Wand Lv1 places up to 5 blocks; Lv2 up to 8 with line, 3×3 wall, 5×5 floor and column modes.

---

## 3. Facilities: Workshop / Study / Greenhouse

Three home facilities, **each with a per-player level** (stored in PlayerData). Data-driven: `facilities.yml`.

### Opening gesture (the easiest snag)
**Sneak + empty hand + right-click** the matching block:
- Workshop → crafting table
- Study → bookshelf
- Greenhouse → flower pot

Empty hand is deliberate, otherwise placing blocks / opening the table would misfire the menu. You can also use `/lyco workshop|study|greenhouse` (but blocks feel more like "using the base").

### Levels and content
Each must first be **repaired / reclaimed / set in order** to Lv1 with materials before it has any function. Upgrading costs materials + radiance from a nearby Nexus. **The GUI adapts as it levels up** (auto-resizing and repositioning the back button).

**Workshop** (crafting post)
- Lv1: tool crafting, batch material conversion, Rain-Breath enchant (adds Mending to a main-hand tool).
- Lv2 (needs an upgrade structure): Stonework Hammer II, Solar Pickaxe, double processing yield + more recipes, enchant also adds **Unbreaking III**.
- Lv3 (needs an Eclipse Crystal): enchant also adds a **combat enchant** (Sharpness / Power / Protection / Efficiency by type).

**Study** (records)
- Lv1: Attunement Manual, Nature Compendium, Discovery Record, Wind Vane, Moon Pouch, Star Compass, expedition entrance.
- Lv2: mark the nearest energy tower onto a locked map; can craft the **Phenomenon Condenser Blueprint**.
- Lv3: **regional forecast** — lists every natural phenomenon harvestable here and now; personal Sun/Moon caps ×1.5 (default 200→300).

**Greenhouse** (plants)
- Lv1: Flower Vein & Moss Bloom cultivation (incl. glow lichen), Flower Vein Shears, Moss Balm, Moss Fertilizer.
- Lv2: Lunar Spore, more advanced cultivation products; can craft the **Seedling Cultivator Blueprint**.
- Lv3: cultivation **output doubled**.

### Upgrade flow
1. **Lv1→Lv2**: click "Upgrade" on the facility page. If you haven't built the upgrade structure, it hands you a **blueprint**; build that multiblock per the outline (an organic little corner centred on the crafting table / bookshelf / flower pot), stand beside it and click upgrade again. Costs Nexus radiance + materials.
2. **Lv2→Lv3**: no upgrade structure; uses an **Eclipse Crystal** + more Nexus radiance + materials.
- Upgraded content is only accessible beside the upgrade structure (or via command) — that structure *is* the "upgraded facility".

### Default repair and upgrade costs

| Action | Default cost |
|---|---|
| Repair Workshop to Lv1 | Morning Dew ×4, oak planks ×8, cobblestone ×8 |
| Set Study in order to Lv1 | Rain Breath ×2, bookshelf ×4, lectern ×1 |
| Reclaim Greenhouse to Lv1 | Flower Vein ×4, glass ×8, moss block ×4 |
| Any facility Lv1→Lv2 | Matching tier-2 structure, nearby usable Nexus Sunlight 500 + Moonlight 500, gold blocks ×2, amethyst blocks ×2 |
| Any facility Lv2→Lv3 | Nearby usable Nexus Sunlight 1200 + Moonlight 1200, Eclipse Crystal ×1, netherite ingot ×1, Radiant Ore ×6; no second upgrade structure needed |

---

## 4. Multiblocks and the building framework

One `Multiblock` framework powers all of: **shape + count validation, ghost preview, one-click blueprint build, admin instant-place**. Templates are defined once in code as "layered character maps" and reused everywhere. Current templates:

- Energy: `energy_relay` (Relay), `sun_tower`/`moon_tower` (Sun/Moon Tower, i.e. Sundial/Moondial), `energy_nexus` (Radiance Nexus), `energy_altar` (Radiance Altar).
- Facility upgrades: `workshop_tier_2`, `study_tier_2`, `greenhouse_tier_2`.
- BOSS: `eclipse_dial` (Eclipse Dial, the altar that summons the Eclipse Warden).
- Automation: `attunement_engine`, `seedling_cultivator`, `phenomenon_condenser`.
- Exploration: `infernal_relay` (player-facing name "Infernal Ruins"; currently a discovery/recipe entry, no actual energy transfer).

Once placed, **right-click the core block** to "activate": it registers (tower / nexus / relay) and floats a nameplate above. When a structure is broken, nearby registrations re-validate and stop working if invalidated (with a "break confirm" to prevent accidental teardown).

---

## 5. The radiance system (sun & moon)

The model is deliberately simple:

- **Passive gathering**: stand somewhere under open sky and you gather on your own — **Sunlight** by day, **Moonlight** by night. The boss bar at the top of the screen is your current amount.
- **Overflow**: once the pool is full, the surplus auto-fills the **Energy Crystal** in your inventory (like a self-charging battery).
- **Concentration network**: **towers produce → relays carry → the Nexus stores → spend it upgrading facilities / summoning a BOSS**.
  - Towers: sun by day, moon by night, with an open-sky top. **This version's towers only produce if generated naturally / by admin / from a blueprint** (player-stacked ones don't produce yet, left for a later version).
  - Nexus: the base's energy store, placed within ~48 horizontal blocks of a tower (tunable); a flowing beam appears when near. Has an owner and access sharing (`/lyco nexus share/unshare`).
  - Relay: carries when a tower is too far from the Nexus, ~48 blocks per hop.
- **Energy outlets**: Radiant Focus, Solar Pickaxe, Lunar Spore, facility upgrades, summoning a BOSS, and the three **automation machines** (a continuous outlet).

**Attunement Engine** (`attunement_engine`, the first automation device, v0.9.1): a rigid multiblock (central blast furnace as controller); right-click the furnace to claim it and link a nearby Radiance Nexus. Place a chest/barrel **directly above** as the input bin, and every few seconds it takes one input, spends Nexus radiance, and returns the product to the container — letting the base take over the block-by-block material processing you'd otherwise click in the workshop. Recipes are data-driven in `config.yml > automation`: the **Sunlight line** runs bright stone (cobble→stone→stone bricks, granite/diorite/andesite polishing, etc.), the **Moonlight line** runs dark rock (deepslate / blackstone / tuff bricks, etc.). **Acquisition** (v0.9.2): craft the "Attunement Engine Blueprint" at Workshop Lv2; the manual and Path of Attunement both introduce it once you've touched a Nexus.

v0.9.3 added two more looks and routes: the Greenhouse Lv2 **Seedling Cultivator** (central composter, plant-processing flavour) and the Study Lv2 **Phenomenon Condenser** (central lectern, material-condensing flavour). All three reuse the chest-above, 48-block Nexus and the same recipe config. This also means the code **doesn't yet split recipes by machine**: any of the three can handle every recipe in the config file; whether to split is a post-playtest decision.

**Radiance Altar** (high-tier crafting) works differently from the workshop: **drop** materials around the altar → **hold a catalyst and right-click** the central enchanting table → spend your radiance to refine. Empty-hand right-click the altar to list the three steps and all recipes.

Default numbers: player Sun/Moon pools 200 each (300 at Study Lv3); +2 per second under open sky, ×3 within 16 blocks of the matching tower; Energy Crystal stores 100 each of sun/moon. Nexus stores 5000 each; max 48 horizontal blocks per hop between Nexus, relay and tower; each connected, sky-open tower in the right period feeds 10/s into the Nexus. The boss bar only shows while holding a Lycohism item in the main or off hand; gathering itself is unaffected.

---

## 6. Expeditions (separate worlds)

The study's expedition screen travels to separate, persistent worlds beyond the Overworld (created on first entry). Expeditions don't wipe Overworld progress; they let you bring materials back. Each world is data-driven in `expeditions.yml`: terrain algorithm, forced biome, eternal rain/night, passive-gathering multiplier, themed hazard mob, unlock gate.

| World | Theme | Terrain algorithm | Exclusive material | Notes |
|---|---|---|---|---|
| **Rainfall Forest** rainfall_forest | Day, wet, mossy (mangrove/swamp/jungle) | RAINFALL | Moss Bloom | Eternal rain; feeds back into greenhouse farming/restoration |
| **Moonless Waste** moonless_waste | Night, barren (dark/dark-forest/wind-eroded) | MOONLESS | Moon Core | Eternal night = the only place to gather Moonlight all day; moon line |
| **Tidal Depths** tidal_depths | Warm sea, normal day/night cycle | OCEAN | Sun Core | Passive-gathering ×2; sun line (mirrors Moonless Waste); exclusive **Tidal Shrine** and mini-boss |
| **Eclipse Realm** eclipse_realm | Eternally dim eclipse (ridged peaks + ravines) | ECLIPSE | (battlefield) Eclipse Crystal | BOSS battlefield; spawn naturally generates a large battlefield + Eclipse Dial |

Two mechanics cut across all expeditions / late-game dimensions:
- **Surface and underground both reskinned**: the terrain algorithm changes more than the surface — themed ore pockets in each style scatter below too (Eclipse's amethyst/blackstone, Ocean's prismarine/sea lanterns, Moonless's sculk). You can tell where you are even while digging.
- **High-difficulty dimension mobs buffed overall**: in the Moonless Waste / Eclipse Realm / Tidal Depths, naturally spawned creatures get more health, speed, size and Strength (`expedition-mob-boost`, tunable). The plugin's own summons (hazard mobs, BOSS, mini-boss) don't stack the boost.

The first three expeditions (Rainfall Forest / Moonless Waste / Tidal Depths) only require completing vanilla `minecraft:story/enter_the_nether`; **the Eclipse Realm is where the sun and moon lines converge, and unlocks only after walking both the Tidal Depths (sun) and Moonless Waste (moon) lines** (gate = `lycohism:progression/tidal_depths` + `lycohism:progression/lunar_waste`, consistent with the Path of Attunement). Normal players enter from the study's expedition page and return to their departure point; worlds are server-shared, created on first entry and persisted afterwards.

> **v0.9 fix**: the study's expedition page expanded from 3 slots to 4, and the Eclipse Realm is now formally on the normal player route — all four expeditions are visible and enterable. The Eclipse shows as locked until you've walked both sun and moon lines, with a dedicated hint explaining the solution (walk the Tidal Depths and Moonless Waste first). After entering, the spawn naturally generates an Eclipse Dial; hold a Sun Core + Moon Core and right-click to summon the Eclipse Warden. (The pre-v0.8.2 problem of being unable to reach it for lack of slots is resolved.)

---

## 7. BOSS and mini-boss

**Eclipse Warden** (Eclipse Realm, main BOSS)
- The sun and moon side-lines converge here: **hold a Sun Core + Moon Core** and right-click the Eclipse Dial core in the Eclipse Realm to summon.
- The body is a heavily buffed Wither (reusing its native boss bar and flight/skull AI), with a **skill set**: dark-eclipse barrage (rapid charged skulls), eclipse burst (Blindness + Wither + knockback), eclipse pull (drags players in); at half health it enters a **moon phase**, more frenzied and summoning minions.
- The battlefield and Eclipse Dial are **generated naturally** at the Eclipse Realm spawn when a player enters (a circular bastion at a fixed height).
- Defeating it drops an **Eclipse Crystal** — the key to tier-3 facilities.
- Default 450 health; casts a random skill roughly every 3s, gains Speed II and Strength II in the moon phase and summons two minions. Within the battlefield, wither explosions and block changes are blocked.

**Tidal Warden** (Tidal Depths, mini-boss)
- Guardian of the **Tidal Shrine** (a prismarine temple) on the seabed, a buffed Elder Guardian.
- The shrine chest gives a Sun Core; the Tidal Depths don't generate other worlds' shared towers/altars, only this exclusive structure, giving the ocean its own character.
- The body is a 200-health Elder Guardian; the shrine chest defaults to Sun Core ×2, prismarine crystals ×6, heart of the sea ×1.

---

## 8. Path of Attunement (progression)

The study opens the "Path of Attunement": a chain of milestones where the first uncompleted chapter is your current objective and gives a hint, with later ones hidden. It also maps onto the vanilla advancement tree (a visible fork). Completion conditions can mix: discovery records, vanilla advancements, facility levels. Data is in `progression.yml`, text in `lang_<lang>.yml`.

The backbone (two symmetric lines since v0.8.2):

```
Awakening → Workshop → First Tools → Rain → Study → Greenhouse → Night → Nether Gate → Radiant Vein → Expedition → Expedition Yield → Radiance
                                                                                                          │ (sun/moon fork)
        ┌── Sun line: Sun Tower → Tidal Depths (Sun Core) → Sun Attunement (Solar Pickaxe)
Radiance ┼── Moon line: Moon Tower → Moonless Waste (Moon Core) → Moon Attunement (Radiant Focus)
        └── Neutral: Radiance Nexus → Radiance Altar → Relay network → Facility Upgrade (Lv2)
                                                    │
                          ┌─────────────────────────┘
   Eclipse convergence: Eclipse Realm (needs both sun & moon expedition lines) → Eclipse Warden (beat the BOSS) → Tier-3 Facilities (Lv3)
```

Design highlights: **the sun and moon lines are treated equally** — fully symmetric shapes (tower → exclusive expedition (core) → attunement (power)); the radiance network/base is split out as a neutral side-line favouring neither. The eclipse chapter opens only after **walking both sun and moon lines**, and the BOSS summon needs both cores, truly converging the two.

v0.9.3 / v0.9.31 added four more visible chapters: Sealed Shrine, Seedling Cultivator, Phenomenon Condenser and Infernal Ruins. These are side-lines and don't change the backbone above.

---

## 9. Worldgen reward structures

Newly generated chunks (Overworld / expeditions) occasionally grow Lycohism landmarks (`structures.yml` tunes rate and height):

- **Moss Attunement Altar**, **Dewspeak Well** (Overworld), **Wind-Trace Stone Circle**, **Rain-Soaked Cellar** (Rainfall Forest only).
- **Moondial / Sundial** (Moon Tower / Sun Tower; naturally generated ones produce directly).
- **Tidal Shrine** (Tidal Depths only, containing a Sun Core chest + Tidal Warden).
- Overworld: **Sealed Shrine** (deepslate platform + amethyst pillars; chest unsealed per individual player).
- Nether: **Radiant Ore vein** (gilded blackstone, breaks for Radiant Ore), **Infernal Ruins** (soul-fire controller; hints at ember refining once discovered).

Each structure comes with chest rewards and registers with the structure locator (`/lyco locate` can guide a compass).

| Structure | Default attempt rate per suitable new chunk | Fixed chest contents / effect |
|---|---:|---|
| Moss Attunement Altar | 2% | Overworld: Flower Vein ×3; expedition: Moss Bloom ×3; plus bone meal ×6 |
| Dewspeak Well | 0.4% | Morning Dew ×4, glass bottles ×3 |
| Wind-Trace Stone Circle | 1.2% | Wind Trace ×3, feathers ×6 |
| Rain-Soaked Cellar | 4%, Rainfall Forest only | Moss Bloom ×5, Moss Balm ×1, Rain Bandage ×1; contains a slow drowned spawner |
| Moondial / Moon Tower | 1% | Overworld gives amethyst; expedition gives Moon Core ×2 + amethyst shards ×4; also registers as a producing tower |
| Sundial / Sun Tower | 0.4%, Overworld | Energy Crystal ×1, glowstone dust ×4; also registers as a producing tower |
| Tidal Shrine | 3%, Tidal Depths only | Sun Core ×2, prismarine crystals ×6, heart of the sea ×1, and spawns a Tidal Warden |
| Radiant Ore vein | 35%, Nether Y=16–96 | Generates two small clusters of gilded blackstone; eligible players always get Radiant Ore from mining |
| Sealed Shrine | 0.6%, Overworld Y=60–130 | Radiant Ore ×2, Morning Dew ×3, Energy Crystal ×2, amethyst shards ×8; needs the Radiance Nexus discovered and an Energy Crystal in hand to unseal |
| Infernal Ruins | 0.6%, Nether ~Y=30–75 | Right-click the central soul fire to record the discovery and unlock the ember-refining hint; no energy transfer currently |

Attempt rate isn't a guaranteed generation rate: when terrain height, slope, water bodies or landing validation fail, that chunk may place no structure.

---

## 10. Commands

`/lycohism` (alias `/lyco`). Normal players:

- `/lyco help`, `/lyco version`
- `/lyco workshop|study|greenhouse`: open a facility
- `/lyco progress`: open the Path of Attunement
- `/lyco upgrade <facility>`: upgrade a facility (also a GUI button)
- `/lyco nexus share|unshare <player>`: share / unshare your Nexus

Admin (`lycohism.admin`, default OP):

- `/lyco reload`, `/lyco debug`
- `/lyco admin`: admin panel (give items, set stage, facility levels, reload, sound test)
- `/lyco give <id> [amount]`: give a phenomenon / tool
- `/lyco stage <id>`: set a progression chapter
- `/lyco build <structure> [ghost]`: instant-place / preview a multiblock
- `/lyco blueprint <structure>`: get a blueprint
- `/lyco locate <structure>`: point a compass at the nearest such structure
- `/lyco expedition`: travel directly, skipping unlock (testing)

---

## 11. Crafting, refining and facility-product tables

### General tool crafting costs

A recipe button is only revealed after the player has touched the required Lycohism material; vanilla materials are never hidden.

| Location | Item | Default cost |
|---|---|---|
| Workshop Lv1 | Dewlight Vial | Morning Dew ×1, glass bottle ×1 |
| Workshop Lv1 | Flower Bookmark | Morning Dew ×2, paper ×1 |
| Workshop Lv1 | Stonework Hammer | Morning Dew ×2, cobblestone ×3, sticks ×2, iron ingots ×2 |
| Workshop Lv1 | Rain Bandage | Rain Breath ×1, paper ×2, string ×1 |
| Workshop Lv1 | Leyline Probe | Leyline Sand ×3, copper ingots ×2, redstone ×1 |
| Workshop Lv1 | Energy Crystal | Moon Dew ×3, amethyst shards ×4, glass ×1 |
| Workshop Lv2 | Stonework Hammer II | Stonework Hammer ×1, Radiant Ore ×4, iron ingots ×4 |
| Workshop Lv2 | Solar Pickaxe | Energy Crystal ×1, diamond pickaxe ×1, gold block ×1 |
| Study Lv1 | Wind Vane | Moon Dew ×1, Wind Trace ×2, clock ×1, copper ingots ×2 |
| Study Lv1 | Moon Pouch | Moon Dew ×4, bundle ×1, string ×2 |
| Study Lv1 | Star Compass | Wind Trace ×4, Moon Dew ×2, compass ×1 |
| Greenhouse Lv1 | Flower Vein Shears | Flower Vein ×2, iron ingot ×1 |
| Greenhouse Lv1 | Moss Balm | Moss Bloom ×2, glistering melon slice ×1 |
| Greenhouse Lv1 | Moss Fertilizer | Moss Bloom ×1, bone meal ×3 |
| Greenhouse Lv2 | Lunar Spore | Moss Bloom ×2, Energy Crystal ×1, glow berries ×8 |
| Greenhouse Lv2 | Seedling Cultivator Blueprint | Morning Dew ×2, bone meal ×8, paper ×1 |
| Study Lv2 | Phenomenon Condenser Blueprint | Rain Breath ×2, amethyst shards ×4, paper ×1 |

The workshop's Rain-Breath enchant additionally needs Rain Breath ×8, Moon Dew ×4, book ×1 and 15 player experience levels; Lv2 also adds Unbreaking III, Lv3 adds Sharpness/Power/Protection/Efficiency by item type.

### Radiance Altar recipes

| Product | Catalyst | Materials dropped by the altar | Player radiance |
|---|---|---|---|
| Radiant Focus ×1 | Gold ingot | Moon Core ×1, Energy Crystal ×1 | Sun 50, Moon 50 |
| Moon Core ×1 | Amethyst block | Amethyst shards ×4, glowstone ×1 | Moon 120 |
| Ember Bloom ×2 | Blaze powder | Magma block ×1, glowstone dust ×2 | Sun 80 |
| Infernal Shard ×3 | Ember Bloom | Blaze rods ×2, nether wart ×4 | Sun 60 |
| Building Wand ×1 | Stick | Radiant Ore ×4, copper ingots ×4, scaffolding ×8 | Sun 60, Moon 20 |
| Building Wand II ×1 | `BREEZE_ROD` | Building Wand ×1, Radiant Ore ×8, diamonds ×2 | Sun 120, Moon 80 |

### Workshop processing and greenhouse cultivation

- Workshop Lv1: cobble→stone, stone→stone bricks, sand→sandstone; one batch at most at a time.
- Workshop Lv2: the above ×2 yield, plus deepslate→deepslate bricks and granite/diorite/andesite→matching polished stone.
- Greenhouse Lv1 Flower Vein route: bone meal, seeds, sugar cane, moss, cherry sapling, spore blossom, torchflower seeds.
- Greenhouse Lv1 Moss Bloom route: clay, mangrove propagule, glow berries, slimeball, pitcher pod, glow lichen.
- Greenhouse Lv2: big dripleaf, cactus, azalea, flowering azalea, sweet berries; Lv3 all cultivation yields ×2 again.

---

## 12. Cross-cutting systems and config

- **Audit log** (`Audit`): every production and radiance change (natural / mob drops, facility / altar crafting, chests, admin gives, radiance gain/loss, facility upgrades, BOSS drops) is recorded to a separate file, for multiplayer servers to trace dupes / economy / bugs.
- **Vanilla crafting guard**: warns before you use a Lycohism material in a vanilla recipe (to avoid wasting precious materials).
- **Break confirm**: breaking a block that's part of a complete multiblock asks for confirmation first; sneaking disables the warning.
- **Config files**: `config.yml` (radiance network, facility upgrades, BOSS, high-difficulty mob buffs...), `tools.yml`, `facilities.yml`, `phenomena.yml`, `structures.yml`, `expeditions.yml`, `progression.yml`, `altars.yml`, `lang_<lang>.yml`. `/lyco reload` applies changes live.

Both the vanilla crafting guard and structure break-confirm use a "do it again within 10 seconds to confirm" pattern; sneaking during the prompt permanently disables that warning and saves it to player data.

---

## 13. Current WIP / next steps

- **v0.9.34**: Beta onboarding pass — first-join welcome hint, `/lyco progress` as the main stuck-recovery entry point, Sealed Shrine block protection, and doc refresh.
- **Sealed Shrine protection (v0.9.34)**: the controller block and chest are now protected from being broken before the player has solved the shrine. Teardown after solving remains unrestricted.
- **First-join onboarding (v0.9.34)**: a 5-line hint is shown once per player on first join, covering Morning Dew, the Attunement Manual, `/lyco progress` and the facility gesture.
- **Loose ends (Beta TODO)**: the three automation machines still share one recipe pool; the Infernal Ruins are exploration/recipe only, not yet a real derelict energy network; `/lyco progress` stage ordering and discoverability have been checked but a full paging/filtering UI is deferred post-playtest.
- **Spigot compatibility**: not handled for now. The code leans heavily on Paper's Adventure/MiniMessage.

> This system is still ALPHA; content will keep being tuned from actual play.
