package com.tinyyana.lycohism.multiblock

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.block.Block

/**
 * v0.7 building framework (PROJECT_PLAN §「Repo 補充：v0.7 規格」§1). One rigid template — a
 * controller block plus blocks at fixed relative offsets — drives everything: shape+count
 * validation, ghost preview, blueprint build and admin instant-stamp. Define a structure once and
 * get all four for free (towers / relays / nexus / altar / later puzzle & boss structures reuse it).
 *
 * Offsets are relative to the controller at (0,0,0). [Rotation] turns the template around Y so the
 * same definition validates and places in any of four facings.
 */
class Multiblock(
    val id: String,
    val controller: Material,
    /** Required blocks relative to the controller; the controller itself is implicit at (0,0,0). */
    val blocks: List<MultiblockBlock>,
) {

    private fun allBlocks(): List<MultiblockBlock> = blocks + MultiblockBlock(0, 0, 0, controller)

    private fun rotatedBlocks(rotation: Rotation): List<MultiblockBlock> =
        allBlocks().map { block ->
            val (rx, rz) = rotation.rotate(block.dx, block.dz)
            MultiblockBlock(rx, block.dy, rz, block.material)
        }

    /** True when the world already contains this structure at [origin] in [rotation]. */
    fun matchesAt(world: World, ox: Int, oy: Int, oz: Int, rotation: Rotation): Boolean =
        rotatedBlocks(rotation).all { world.getBlockAt(ox + it.dx, oy + it.dy, oz + it.dz).type == it.material }

    /** The rotation in which the structure is fully built at [origin], or null if none is. */
    fun detectRotation(world: World, ox: Int, oy: Int, oz: Int): Rotation? =
        Rotation.entries.firstOrNull { matchesAt(world, ox, oy, oz, it) }

    /** Controller block when [block] belongs to a complete copy of this structure. */
    fun anchorContaining(block: Block): Block? {
        for (rotation in Rotation.entries) {
            for (part in rotatedBlocks(rotation).filter { it.material == block.type }) {
                val ox = block.x - part.dx
                val oy = block.y - part.dy
                val oz = block.z - part.dz
                if (matchesAt(block.world, ox, oy, oz, rotation)) return block.world.getBlockAt(ox, oy, oz)
            }
        }
        return null
    }

    /** Stamps every block (admin instant-build / blueprint one-shot). */
    fun place(world: World, ox: Int, oy: Int, oz: Int, rotation: Rotation = Rotation.NONE) {
        rotatedBlocks(rotation).forEach { world.getBlockAt(ox + it.dx, oy + it.dy, oz + it.dz).type = it.material }
    }

    /**
     * Shows a five-second, player-only preview of the *actual* blocks the structure is made of (not a
     * generic glass shell) so a blueprint reads like the finished build — same idea as the 石工槌 preview.
     * Green/red dust still flags which blocks are already in place vs. still missing.
     */
    fun showGhost(plugin: Plugin, world: World, ox: Int, oy: Int, oz: Int, rotation: Rotation, player: Player) {
        val locations = rotatedBlocks(rotation).map {
            val location = world.getBlockAt(ox + it.dx, oy + it.dy, oz + it.dz).location
            val correct = location.block.type == it.material
            player.sendBlockChange(location, it.material.createBlockData())
            player.spawnParticle(Particle.DUST, location.toCenterLocation(), 3, 0.3, 0.3, 0.3, 0.0, if (correct) PRESENT_DUST else MISSING_DUST)
            location
        }
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (player.isOnline) locations.forEach { player.sendBlockChange(it, it.block.blockData) }
        }, PREVIEW_TICKS)
    }

    /** Material -> count, for blueprint material cost and the "數量正確" check. */
    fun materialCounts(): Map<Material, Int> = allBlocks().groupingBy { it.material }.eachCount()

    companion object {
        private val MISSING_DUST = Particle.DustOptions(Color.fromRGB(0xFF, 0x44, 0x44), 1.4f)
        private val PRESENT_DUST = Particle.DustOptions(Color.fromRGB(0x55, 0xFF, 0x77), 1.0f)
        private const val PREVIEW_TICKS = 100L

        /**
         * Builds a template from human-readable layers (bottom-to-top): `layers[y][z]` is a row of
         * chars, each mapped via [legend]; [controllerChar] marks the controller (anchor). Space and
         * '.' are air. Offsets are taken relative to the controller cell, so it can sit anywhere in
         * the pattern. This is how structures are authored — far easier than hand-listing offsets.
         */
        fun fromLayers(
            id: String,
            legend: Map<Char, Material>,
            controllerChar: Char,
            layers: List<List<String>>,
        ): Multiblock {
            var controller: Triple<Int, Int, Int>? = null
            val cells = mutableListOf<MultiblockBlock>()
            for ((y, rows) in layers.withIndex()) {
                for ((z, row) in rows.withIndex()) {
                    for ((x, ch) in row.withIndex()) {
                        if (ch == ' ' || ch == '.') continue
                        if (ch == controllerChar) {
                            require(controller == null) { "Multiblock '$id' has more than one controller '$controllerChar'." }
                            controller = Triple(x, y, z)
                            continue
                        }
                        val material = legend[ch] ?: error("Multiblock '$id': legend has no entry for '$ch'.")
                        cells += MultiblockBlock(x, y, z, material)
                    }
                }
            }
            val (cx, cy, cz) = controller ?: error("Multiblock '$id' has no controller char '$controllerChar'.")
            val controllerMaterial = legend[controllerChar] ?: error("Multiblock '$id': legend has no entry for controller '$controllerChar'.")
            val blocks = cells.map { MultiblockBlock(it.dx - cx, it.dy - cy, it.dz - cz, it.material) }
            return Multiblock(id, controllerMaterial, blocks)
        }
    }
}

/** One block of a [Multiblock] template, at a fixed offset from the controller. */
data class MultiblockBlock(val dx: Int, val dy: Int, val dz: Int, val material: Material)

/**
 * Quarter-turn rotations of a template around the Y axis. [rotate] is pure integer math (no Bukkit),
 * so it is unit-tested directly; CW90 applied four times returns the original offset.
 */
enum class Rotation {
    NONE, CW90, CW180, CW270;

    fun rotate(dx: Int, dz: Int): Pair<Int, Int> = when (this) {
        NONE -> dx to dz
        CW90 -> dz to -dx
        CW180 -> -dx to -dz
        CW270 -> -dz to dx
    }
}
