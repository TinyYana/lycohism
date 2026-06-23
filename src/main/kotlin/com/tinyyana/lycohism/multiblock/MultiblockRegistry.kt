package com.tinyyana.lycohism.multiblock

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Material

/**
 * Holds the defined [Multiblock] templates. v0.7 starts with the 能量中繼器 (energy relay); towers,
 * the nexus core and the altar will be added here (and later loaded from a structures file). The
 * registry is what `/lycohism build` and the future blueprint item read from.
 *
 * ponytail: templates are authored in code via [Multiblock.fromLayers] for now — readable and
 * type-checked. Move to YAML only once non-developers need to add structures.
 */
class MultiblockRegistry(private val plugin: Lycohism) {

    private val structures = LinkedHashMap<String, Multiblock>()

    init {
        load()
    }

    fun load() {
        structures.clear()
        register(energyRelay())
        register(sunTower())
        register(moonTower())
        register(energyNexus())
        register(energyAltar())
        register(workshopTier2())
        register(studyTier2())
        register(greenhouseTier2())
        register(eclipseDial())
        register(attunementEngine())
        register(seedlingCultivator())
        register(phenomenonCondenser())
        register(infernalRelay())
        register(emberForge())
        plugin.logger.info("Loaded ${structures.size} multiblock templates.")
    }

    fun get(id: String): Multiblock? = structures[id]

    fun all(): List<Multiblock> = structures.values.toList()

    fun ids(): List<String> = structures.keys.toList()

    private fun register(multiblock: Multiblock) {
        structures[multiblock.id] = multiblock
    }

    // ponytail: waxed copper throughout the energy structures — plain copper oxidizes to EXPOSED/
    // WEATHERED/OXIDIZED over time, which fails the exact-material validation and silently "breaks"
    // a built tower/relay/nexus. Waxed copper never weathers, so the fix is the material, not code.

    /** 能量中繼器: a 3×3 copper base with a central pillar topped by a lightning-rod antenna. */
    private fun energyRelay(): Multiblock = Multiblock.fromLayers(
        id = "energy_relay",
        legend = mapOf(
            'C' to Material.WAXED_CUT_COPPER,
            'P' to Material.WAXED_COPPER_BLOCK,
            'H' to Material.WAXED_CHISELED_COPPER,
            'R' to Material.LIGHTNING_ROD,
            'X' to Material.LODESTONE,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("CCC", "CXC", "CCC"),
            listOf("   ", " P ", "   "),
            listOf("   ", " P ", "   "),
            listOf("   ", " H ", "   "),
            listOf("   ", " R ", "   "),
        ),
    )

    /** 輝能核心（據點 Nexus）: a 3×3 copper base with corner pillars and a central amethyst/gold core
     * topped by a beacon controller. Right-click the beacon to claim it as a base energy store. */
    private fun energyNexus(): Multiblock = Multiblock.fromLayers(
        id = "energy_nexus",
        legend = mapOf(
            'C' to Material.WAXED_CUT_COPPER,
            'P' to Material.WAXED_COPPER_BLOCK,
            'A' to Material.AMETHYST_BLOCK,
            'G' to Material.GOLD_BLOCK,
            'X' to Material.BEACON,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("CCC", "CCC", "CCC"),
            listOf("P P", " A ", "P P"),
            listOf("P P", " G ", "P P"),
            listOf("   ", " X ", "   "),
        ),
    )

    /** 輝能祭壇: a central enchanting-table pedestal ringed by four chiseled pedestals. Hold the
     * catalyst and right-click the centre, with the ingredients dropped around it, to craft. */
    private fun energyAltar(): Multiblock = Multiblock.fromLayers(
        id = "energy_altar",
        legend = mapOf(
            'D' to Material.POLISHED_DEEPSLATE,
            'C' to Material.CHISELED_DEEPSLATE,
            'B' to Material.DEEPSLATE_BRICKS,
            'X' to Material.ENCHANTING_TABLE,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("DDD", "DDD", "DDD"),
            listOf("C C", " B ", "C C"),
            listOf("   ", " X ", "   "),
        ),
    )

    /** 日輝塔: a tall (18-block) bright sandstone tower — base 5×5, four corner pillars + central
     * shaft, widening crown lit by glowstone, gold cap and an end-rod ray. The energy economy's
     * 日輝 producer (also generated naturally). Shares its silhouette with the moon tower. */
    private fun sunTower(): Multiblock = tower(
        id = "sun_tower",
        base = Material.CUT_SANDSTONE,
        shaft = Material.SMOOTH_SANDSTONE,
        pillar = Material.SANDSTONE_WALL,
        glow = Material.GLOWSTONE,
        cap = Material.GOLD_BLOCK,
        controller = Material.CHISELED_SANDSTONE,
    )

    /** 月輝塔: the night counterpart — deepslate and amethyst. The 月輝 producer. */
    private fun moonTower(): Multiblock = tower(
        id = "moon_tower",
        base = Material.POLISHED_DEEPSLATE,
        shaft = Material.DEEPSLATE_BRICKS,
        pillar = Material.POLISHED_DEEPSLATE_WALL,
        glow = Material.AMETHYST_BLOCK,
        cap = Material.AMETHYST_BLOCK,
        controller = Material.CHISELED_DEEPSLATE,
    )

    // ── 設施升級多方塊（v0.7.4，#3）────────────────────────────────────────
    // 每個家用設施有一份「升級型」剛性結構。玩家第一次蓋出來、在旁邊開設施 → 才能升到 Lv2。
    // 控制器＝該設施的存取方塊（工作台／書架／花盆），所以同一個互動就能開啟。造型刻意做成有機的
    // 小角落（不是方塊堆）以兼顧美學。MultiblockListener 會跳過 *_tier_2 不攔截原版互動。
    val FACILITY_TIER_2 = listOf("workshop_tier_2", "study_tier_2", "greenhouse_tier_2")

    /** 升級工房：銅基鐵砧鍛造角落，工作台為核心。 */
    private fun workshopTier2(): Multiblock = Multiblock.fromLayers(
        id = "workshop_tier_2",
        legend = mapOf(
            'B' to Material.WAXED_CUT_COPPER,
            'A' to Material.ANVIL,
            'S' to Material.SMITHING_TABLE,
            'P' to Material.WAXED_CUT_COPPER,
            'L' to Material.LANTERN,
            'X' to Material.CRAFTING_TABLE,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("BBB", "BXB", "BBB"),
            listOf("A.S", "...", "P.P"),
            listOf("...", "...", "L.L"),
        ),
    )

    /** 升級書房：深板岩基座、四角雕刻書櫃與燭光，書架為核心。 */
    private fun studyTier2(): Multiblock = Multiblock.fromLayers(
        id = "study_tier_2",
        legend = mapOf(
            'D' to Material.DEEPSLATE_BRICKS,
            'H' to Material.CHISELED_BOOKSHELF,
            'C' to Material.CANDLE,
            'X' to Material.BOOKSHELF,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("DDD", "DXD", "DDD"),
            listOf("H.H", "...", "H.H"),
            listOf("C.C", "...", "C.C"),
        ),
    )

    /** 升級溫室：苔基、四角杜鵑花叢與玻璃頂棚，花盆為核心。 */
    private fun greenhouseTier2(): Multiblock = Multiblock.fromLayers(
        id = "greenhouse_tier_2",
        legend = mapOf(
            'M' to Material.MOSS_BLOCK,
            'A' to Material.FLOWERING_AZALEA,
            'G' to Material.GLASS,
            'X' to Material.FLOWER_POT,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("MMM", "MXM", "MMM"),
            listOf("A.A", "...", "A.A"),
            listOf("GGG", "GGG", "GGG"),
        ),
    )

    /**
     * 日月儀（v0.8）：召喚蝕影守望者的祭壇。深板岩基座、四角金（日）／紫水晶（月）對置，中央哭泣
     * 黑曜石為核心。在暮蝕之境蓋好、手持月輝核心右鍵核心 → 觸發蝕並召喚 BOSS（見 [EclipseBoss]）。
     */
    private fun eclipseDial(): Multiblock = Multiblock.fromLayers(
        id = "eclipse_dial",
        legend = mapOf(
            'D' to Material.POLISHED_DEEPSLATE,
            'B' to Material.DEEPSLATE_BRICKS,
            'G' to Material.GOLD_BLOCK,
            'A' to Material.AMETHYST_BLOCK,
            'X' to Material.CRYING_OBSIDIAN,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("DDD", "DDD", "DDD"),
            listOf("G A", " B ", "A G"),
            listOf("   ", " X ", "   "),
        ),
    )

    /**
     * 自動調律機（v0.9.1 自動化）：銅基座、四角燈籠、中央高爐為控制器。右鍵高爐認領；每個週期從正上方
     * 容器（玩家自放的箱子）取出原料、消耗附近核心輝能、放回加工成品。見 [com.tinyyana.lycohism.energy.AutomationManager]。
     */
    private fun attunementEngine(): Multiblock = Multiblock.fromLayers(
        id = "attunement_engine",
        legend = mapOf(
            'P' to Material.WAXED_CUT_COPPER,
            'B' to Material.WAXED_CHISELED_COPPER,
            'L' to Material.LANTERN,
            'X' to Material.BLAST_FURNACE,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("PPP", "PBP", "PPP"),
            listOf("L L", " X ", "L L"),
        ),
    )

    /**
     * 育苗機（v0.9.3 溫室自動化）：苔蘚基座＋四角杜鵑花叢＋中央堆肥桶為控制器。右鍵堆肥桶認領；
     * 邏輯複用 [com.tinyyana.lycohism.energy.AutomationManager]，從正上方容器取植物素材、耗月輝加工。
     */
    private fun seedlingCultivator(): Multiblock = Multiblock.fromLayers(
        id = "seedling_cultivator",
        legend = mapOf(
            'M' to Material.MOSS_BLOCK,
            'A' to Material.FLOWERING_AZALEA,
            'X' to Material.COMPOSTER,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("MMM", "MMM", "MMM"),
            listOf("A.A", ".X.", "A.A"),
        ),
    )

    /**
     * 現象凝縮台（v0.9.3 書房自動化）：深板岩基座＋四角書架＋中央講台為控制器。右鍵講台認領；
     * 邏輯複用 [com.tinyyana.lycohism.energy.AutomationManager]，從正上方容器取學術素材、耗月輝加工。
     */
    private fun phenomenonCondenser(): Multiblock = Multiblock.fromLayers(
        id = "phenomenon_condenser",
        legend = mapOf(
            'D' to Material.DEEPSLATE_BRICKS,
            'H' to Material.BOOKSHELF,
            'X' to Material.LECTERN,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("DDD", "DDD", "DDD"),
            listOf("H.H", ".X.", "H.H"),
        ),
    )

    /**
     * 地獄腐壞中繼廢址（v0.9.31）：鑿裂地獄磚基座、四角地獄磚牆柱、中央魂靈焰火為控制器。
     * 右鍵魂靈焰火認領廢址（discover + label），解鎖輝能祭壇的焰輝碎片提煉配方。
     * 自然生成於地獄底層新區塊（StructureGenerator），可用 /lycohism build 管理員放置。
     */
    private fun infernalRelay(): Multiblock = Multiblock.fromLayers(
        id = "infernal_relay",
        legend = mapOf(
            'N' to Material.CRACKED_NETHER_BRICKS,
            'W' to Material.NETHER_BRICK_WALL,
            'X' to Material.SOUL_CAMPFIRE,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("NNN", "NNN", "NNN"),
            listOf("W.W", ".X.", "W.W"),
        ),
    )

    /**
     * 燼華鍛爐（v0.9.32）：地獄磚基座＋四角磨製黑石磚柱＋中央熔岩塊為控制器。玩家自建；
     * 空手右鍵熔岩塊查看配方，手持燼華右鍵煉製焰輝碎片（耗日輝）。見 [AltarListener]。
     */
    private fun emberForge(): Multiblock = Multiblock.fromLayers(
        id = "ember_forge",
        legend = mapOf(
            'N' to Material.NETHER_BRICKS,
            'P' to Material.POLISHED_BLACKSTONE_BRICKS,
            'X' to Material.MAGMA_BLOCK,
        ),
        controllerChar = 'X',
        layers = listOf(
            listOf("NNN", "NNN", "NNN"),
            listOf("P.P", ".X.", "P.P"),
        ),
    )

    /** Builds the shared 18-tall tower silhouette from a material palette. */
    private fun tower(
        id: String,
        base: Material,
        shaft: Material,
        pillar: Material,
        glow: Material,
        cap: Material,
        controller: Material,
    ): Multiblock {
        val layers = buildList {
            add(listOf("BBBBB", "BBBBB", "BBXBB", "BBBBB", "BBBBB")) // y0 base, X = controller (centre)
            repeat(13) { add(listOf("     ", " W W ", "  S  ", " W W ", "     ")) } // y1..13 corner pillars + shaft
            add(listOf("     ", " SSS ", " SSS ", " SSS ", "     ")) // y14 crown platform
            add(listOf("     ", "  G  ", " GGG ", "  G  ", "     ")) // y15 glow ring
            add(listOf("     ", "     ", "  O  ", "     ", "     ")) // y16 cap
            add(listOf("     ", "     ", "  R  ", "     ", "     ")) // y17 ray
        }
        return Multiblock.fromLayers(
            id = id,
            legend = mapOf(
                'B' to base, 'S' to shaft, 'W' to pillar, 'G' to glow,
                'O' to cap, 'R' to Material.END_ROD, 'X' to controller,
            ),
            controllerChar = 'X',
            layers = layers,
        )
    }
}
