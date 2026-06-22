package com.tinyyana.lycohism.boss

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Material
import org.bukkit.World

/**
 * v0.8.1 #4 — builds the蝕影守望者's battle arena (and its 日月儀 summon altar) once near the
 * 暮蝕之境 spawn, so the boss has a proper stage that "naturally" exists when you arrive instead of
 * needing to hand-build the dial. Idempotent: keyed off the dial controller already standing at
 * centre, so re-entry is just a cheap block check.
 */
object EclipseArena {

    private const val RADIUS = 20
    private const val WALL_HEIGHT = 6
    // Fixed build altitude so the idempotency check is stable — using getHighestBlockYAt would drift
    // up to the dial's own top on the second visit and rebuild the arena ever higher.
    private const val FLOOR_Y = 100
    // Fixed centre (world origin). The arena MUST pin to a constant: the old code read world.spawnLocation
    // as the centre but then moved spawn into the arena (south edge), so every re-entry shifted the centre,
    // failed the dial check, and rebuilt a fresh arena while orphaning the old one — the battlefield seemed
    // to vanish. Players always arrive via the spawn we set below, so the origin is fine.
    private const val CENTER_X = 0
    private const val CENTER_Z = 0

    /** Ensures the arena exists at [world]'s origin; builds it the first time and parks spawn inside. */
    fun ensure(plugin: Lycohism, world: World) {
        val cx = CENTER_X
        val cz = CENTER_Z
        val spawn = intArrayOf(cx, FLOOR_Y + 1, cz + (RADIUS - 3))

        // Already built? The dial controller sits at centre, two above the floor.
        if (world.getBlockAt(cx, FLOOR_Y + 2, cz).type == Material.CRYING_OBSIDIAN) {
            // Re-park spawn inside the arena in case an older (drifted) world left it elsewhere.
            if (world.spawnLocation.blockX != spawn[0] || world.spawnLocation.blockZ != spawn[2]) {
                world.setSpawnLocation(spawn[0], spawn[1], spawn[2])
            }
            return
        }

        buildFloorAndWalls(world, cx, FLOOR_Y, cz)
        plugin.multiblockRegistry.get(EclipseBoss.DIAL_ID)?.place(world, cx, FLOOR_Y + 2, cz)
        com.tinyyana.lycohism.multiblock.StructureActivation.label(world.getBlockAt(cx, FLOOR_Y + 2, cz), EclipseBoss.DIAL_ID)

        // Park spawn on the floor near the south edge so players arrive facing the altar.
        world.setSpawnLocation(spawn[0], spawn[1], spawn[2])
    }

    private fun buildFloorAndWalls(world: World, cx: Int, y: Int, cz: Int) {
        val r2 = RADIUS * RADIUS
        for (dx in -RADIUS..RADIUS) for (dz in -RADIUS..RADIUS) {
            val d2 = dx * dx + dz * dz
            if (d2 > r2) continue
            // Floor (chequered day-gold / moon-dark accents) + a solid base course below it.
            world.getBlockAt(cx + dx, y, cz + dz).type =
                if ((dx + dz) and 1 == 0) Material.POLISHED_BLACKSTONE_BRICKS else Material.POLISHED_DEEPSLATE
            for (yy in 1..4) world.getBlockAt(cx + dx, y - yy, cz + dz).type = Material.DEEPSLATE_BRICKS
            // Clear headroom so the boss can fly and skulls have line of sight.
            for (yy in 1..WALL_HEIGHT + 6) world.getBlockAt(cx + dx, y + yy, cz + dz).type = Material.AIR
            // Perimeter wall (the outer ring of the disc).
            if (d2 >= (RADIUS - 1) * (RADIUS - 1)) {
                for (yy in 1..WALL_HEIGHT) world.getBlockAt(cx + dx, y + yy, cz + dz).type = Material.POLISHED_BLACKSTONE_BRICKS
            }
        }
        // Eight pillars on the compass points: alternating day-gold / moon-amethyst caps + light.
        val ring = RADIUS - 2
        val points = listOf(
            ring to 0, -ring to 0, 0 to ring, 0 to -ring,
            ring to ring, ring to -ring, -ring to ring, -ring to -ring,
        )
        points.forEachIndexed { index, (dx, dz) ->
            for (yy in 1..WALL_HEIGHT + 2) world.getBlockAt(cx + dx, y + yy, cz + dz).type = Material.POLISHED_DEEPSLATE
            world.getBlockAt(cx + dx, y + WALL_HEIGHT + 3, cz + dz).type =
                if (index % 2 == 0) Material.GOLD_BLOCK else Material.AMETHYST_BLOCK
            world.getBlockAt(cx + dx, y + WALL_HEIGHT + 4, cz + dz).type = Material.SEA_LANTERN
        }
    }
}
