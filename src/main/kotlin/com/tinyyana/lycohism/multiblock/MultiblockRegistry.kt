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
        plugin.logger.info("Loaded ${structures.size} multiblock templates.")
    }

    fun get(id: String): Multiblock? = structures[id]

    fun all(): List<Multiblock> = structures.values.toList()

    fun ids(): List<String> = structures.keys.toList()

    private fun register(multiblock: Multiblock) {
        structures[multiblock.id] = multiblock
    }

    /** 能量中繼器: a 3×3 copper base with a central pillar topped by a lightning-rod antenna. */
    private fun energyRelay(): Multiblock = Multiblock.fromLayers(
        id = "energy_relay",
        legend = mapOf(
            'C' to Material.CUT_COPPER,
            'P' to Material.COPPER_BLOCK,
            'H' to Material.CHISELED_COPPER,
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
            'C' to Material.CUT_COPPER,
            'P' to Material.COPPER_BLOCK,
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
