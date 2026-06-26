# Lycohism Gameplay Tutorial

*[繁體中文版](TUTORIAL.zh-TW.md)*

This one is for players. It won't list every number and recipe — some things are more fun to bump into yourself — but it spells out "where people get stuck" so you don't keep coming back to look things up.

Just play vanilla survival; Lycohism's stuff surfaces on its own. Below roughly follows the order you'll meet it.

## First, the thing that trips everyone up

Facilities (Workshop, Study, Greenhouse) aren't opened by command — they're opened with blocks in the world. The gesture is:

**Sneak + empty hand + right-click** the matching block.

- Workshop → crafting table
- Study → bookshelf
- Greenhouse → flower pot

Empty hand is deliberate, otherwise every time you wanted to place a block or use a crafting table you'd open a menu instead. Remember this gesture and the rest flows.

## Natural phenomena: your first material

The world has gained a layer that responds to you. The earliest you'll meet is **Morning Dew**: at dawn, break grass and flowers in a grassland for a chance to drop it.

The key is "the right time, the right place". Morning Dew is only at dawn, only on vegetation. The later ones work the same way —

- Rain Breath: rain, near water
- Moon Dew: night, full moon, high up
- Wind Trace: mountains, high up
- Leyline Sand: caves, deep
- Flower Vein: flower clusters, cherry groves

No need to memorise. While holding certain tools (like the Wind Vane or Leyline Probe), the screen hints at what's harvestable in your current environment. Go wander outside first and bump into things.

## Workshop: turning materials into useful things

After a few drops of Morning Dew, find a crafting table and sneak+empty-hand right-click to open the Workshop. It starts broken; the menu tells you what repair needs (Morning Dew + some planks and stone). Repaired = Lv1, and you can start making tools.

The first thing worth making is probably the **Dewlight Vial** — a stretch of night vision for mining, comfier than constantly placing torches. Others like the Stonework Hammer (stone processing) and Flower Bookmark (remember a place, point back later) are here too. Each button's description spells out its cost and use.

## Study and Greenhouse

Same logic, different blocks.

- **Study** (bookshelf): holds your records. The Attunement Manual tells you "the next small step", the Nature Compendium records phenomena you've met, and there's the entrance to the Path of Attunement. It's also the departure point for expeditions.
- **Greenhouse** (flower pot): plant-related. Cultivate various farming materials from Flower Vein, make tools like the Flower Vein Shears.

## Path of Attunement: look here when you don't know what to do

The study can open the "Path of Attunement". It's a chain of milestones; the first uncompleted chapter is your current objective and gives a hint, while later chapters stay hidden to keep a sense of pushing forward.

**Shortcut: type `/lyco progress` any time.** You don't need the study open — the command works from anywhere and always shows your current objective first.

If you log in one day and forget where you were stuck, check this first. It also maps onto the vanilla advancement screen, so you can see it there too.

## Radiance: sun and moon

Mid-game you'll run into energy. The model is simple: **stand under the open sky and you gather on your own** — Sunlight by day, Moonlight by night. The boss bar at the top of the screen is your current amount.

Once the pool is full, the surplus auto-overflows into the **Energy Crystal** in your inventory (made at the workshop). So the Energy Crystal is more like a self-charging battery — you don't have to hold it.

Next comes "concentrating" the energy, which is where most people get confused, so let's break it down:

1. **Sun Tower / Moon Tower** (called Sundial / Moondial in-game): tall towers that produce. The sun tower produces by day, the moon tower by night, and the top must see open sky. **Note: in this version, only naturally generated towers — or ones placed by an admin / blueprint — produce.** One you stack by hand won't run (player-produced towers are a later version). Wander to one in the world and approach it, and it gets recorded, advancing your progress.
2. **Radiance Nexus**: the base's energy store. Place it within 48 horizontal blocks of a tower and the tower's energy flows in. When you're near, you'll see a flowing beam — that means energy is actually moving.
3. **Energy Relay**: when a tower is too far from the Nexus, place a relay in between to carry it, 48 blocks per hop, segment by segment.

Remember the order as one sentence: **towers produce → relays carry → the Nexus stores → spend it upgrading facilities**. The Nexus page and the facility upgrade buttons both spell out this flow; click in if you forget.

## Automation machines: let the base take over processing

Once the Radiance Nexus is built and energy is flowing, you can make an **Attunement Engine**. Its blueprint is crafted at **Workshop Lv2** (the Attunement Manual also introduces it once you've touched a Nexus). It's a small multiblock with a central blast furnace; build it per the blueprint and **right-click the furnace** to claim it.

Use: place a chest **directly above it** and drop in inputs (like cobblestone). As long as it's within 48 blocks of a Nexus that has the matching radiance, it takes one input every few seconds, spends a little radiance, and returns the product to the chest. This is the first "continuous radiance sink" beyond upgrading facilities and summoning the BOSS.

Once the Greenhouse and Study reach Lv2, their tools pages each gain a blueprint too: the **Seedling Cultivator** uses a composter as its controller and is flavoured toward plant processing; the **Phenomenon Condenser** uses a lectern and is flavoured toward material condensing. All three currently share one recipe pool, so in practice don't fuss over "which category does this input belong to" — drop it into any of the three and it may run. This is an ALPHA simplification; whether to split is a post-playtest decision.

## Upgrading facilities: build the "upgrade structure" first

A repaired facility is Lv1. To reach Lv2, materials and energy alone aren't enough — you first have to build the facility into a matching **upgrade structure** (a fixed-shape multiblock).

Not knowing what it looks like is normal; I didn't either at first. So the way it works: click "Upgrade" on the facility page, and if you haven't built the structure yet, it hands you a **blueprint** directly. Hold the blueprint and right-click for the translucent outline, then build it; or sneak+right-click to build it instantly using materials. Build it, stand beside it, and click upgrade once more.

The post-upgrade content (enhanced processing, enchant with extra durability, new cultivation products, the tower-marking map, etc.) is only accessible beside that upgrade structure, or via command. In other words, that structure *is* the body of your "upgraded facility".

After the Study reaches Lv3, on top of the regional forecast, your personal Sunlight/Moonlight caps also rise from the default 200 to 300.

## Sealed Shrine

New chunks in the Overworld occasionally grow a small shrine of a deepslate platform, amethyst pillars and soul lanterns. The chest won't open directly — first touch the Radiance Nexus line, then **right-click the central chiseled deepslate while holding an Energy Crystal**. The controller gives different directional hints depending on which condition is missing.

Unseal progress is tracked per player; one player solving a shrine doesn't mean you have. One ALPHA boundary to flag: currently only the chest is locked, the whole structure isn't teardown-protected yet — don't pickaxe the puzzle apart physically.

## Expeditions

The study's expedition screen travels to places beyond the Overworld. Early on you'll first meet:

- **Rainfall Forest**: day, wet, overgrown with moss. The exclusive material is Moss Bloom — bring it back to cultivate in the greenhouse, feeding the farming and restoration lines.
- **Moonless Waste**: always night, more dangerous. Its reason to exist is the mechanic itself — this is the only place to stably gather Moonlight all day, and where the Moon Core is produced.

After entering the Nether you'll also meet two material lines: mobs may drop **Infernal Shards**; new chunks may also grow **Infernal Ruins** with a central burning soul fire. Right-click that soul fire to record the discovery and get a hint on how to refine ember-radiance at the altar. It doesn't transmit radiance currently — the name looks like a relay but don't read too much into it, it's still just an exploration point.

Expeditions don't wipe your Overworld progress; they're for bringing things back to use.

Two more come mid-to-late game, both extensions of the "sun / moon" lines:

- **Tidal Depths** (sun line): a warm sea of endless day, Sunlight gathered at double rate; exclusive material the Sun Core, plus the Tidal Shrine and a mini-boss.
- **Eclipse Realm** (convergence): sun and moon eclipse here, the battlefield of the BOSS **Eclipse Warden**. It unlocks only after walking both the Tidal Depths (sun) and Moonless Waste (moon) lines — before that, it shows as locked on the study's expedition page, with the button stating which line is still missing. After entering, an **Eclipse Dial** generates naturally at spawn; hold a **Sun Core + Moon Core** at once and right-click it to summon the BOSS. Win and it drops the **Eclipse Crystal**, the key to raising facilities to Lv3.

(The Eclipse Realm was built back in v0.8, but only in v0.9 was it formally hooked up to the normal player entrance — there weren't enough slots before; now all four expeditions are visible and enterable.)

## Radiance Altar

Higher-tier crafting (like the Radiant Focus) moves to the altar. Its use differs from the workshop, and is a bit confusing the first time:

1. **Drop** the materials around the altar (dropped items within about 2.5 blocks).
2. **Hold a catalyst** and right-click the central enchanting table.
3. Spend your radiance to refine the product.

If you're unsure which item is the catalyst, empty-hand right-click the altar and it lists the three steps and all current recipes. Right-click while holding the wrong thing and it'll tell you which items count as catalysts.

## A few commands and small reminders

- `/lyco help`: lists the usable commands.
- `/lyco progress`: opens the Path of Attunement directly (when you don't want to run to the study).
- Facilities can all be opened with `/lyco workshop`, `/lyco study`, `/lyco greenhouse`, but for normal play, opening with blocks feels more like "using your base".

Finally: this is still ALPHA, the numbers will be tuned, and you may hit bugs. If you're stuck or something feels off, it's usually not you — it's just not dialled in yet.
