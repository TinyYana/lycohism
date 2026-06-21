package com.tinyyana.lycohism.energy

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.gui.NexusHolder
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID

/**
 * 輝能核心（據點 Nexus）— the base-side energy store (PROJECT_PLAN §「v0.7 規格」§2). A nexus is a
 * built rigid multiblock claimed by right-clicking its controller; the placer is owner and may share
 * access with members. Towers produce 日輝/月輝 that flows — directly or hopping through 能量中繼器
 * relays — into the nearest connected nexus. Facility upgrades (and later the altar) draw from it.
 *
 * Persistence: in-memory authoritative, saved on a slow autosave and on disable (energy is low-stakes
 * enough that per-change disk writes aren't worth it). Connectivity is a small BFS over relays.
 */
class NexusManager(private val plugin: Lycohism) {

    private val nexuses = mutableListOf<Nexus>()
    private val relays = mutableListOf<Node>()
    private val file = File(plugin.dataFolder, FILE_NAME)
    private var dirty = false
    private var cyclesSinceSave = 0

    private var capacity = 5000
    private var networkRange = 48.0
    private var productionPerCycle = 10

    init {
        load()
    }

    fun load() {
        val cfg = plugin.config.getConfigurationSection("nexus")
        capacity = (cfg?.getInt("capacity", capacity) ?: capacity).coerceAtLeast(1)
        networkRange = (cfg?.getDouble("network-range", networkRange) ?: networkRange).coerceAtLeast(1.0)
        productionPerCycle = (cfg?.getInt("production-per-cycle", productionPerCycle) ?: productionPerCycle).coerceAtLeast(0)
        nexuses.clear()
        relays.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getStringList("nexuses").forEach { decodeNexus(it)?.let(nexuses::add) }
        yaml.getStringList("relays").forEach { decodeNode(it)?.let(relays::add) }
    }

    // ---- Registration (from controller right-click) ------------------------

    /** Claims a built 輝能核心 at the clicked controller, or returns the existing one. */
    fun claimNexus(player: Player, block: Block): ClaimResult {
        existingNexus(block)?.let { return ClaimResult.EXISTS }
        nexuses += Nexus(block.world.name, block.x, block.y, block.z, player.uniqueId)
        markDirty()
        return ClaimResult.CLAIMED
    }

    /** Registers a built 能量中繼器 at the clicked controller. */
    fun registerRelay(block: Block): Boolean {
        val node = Node(block.world.name, block.x, block.y, block.z)
        if (relays.any { it == node }) return false
        relays += node
        markDirty()
        return true
    }

    fun existingNexus(block: Block): Nexus? =
        nexuses.firstOrNull { it.world == block.world.name && it.x == block.x && it.y == block.y && it.z == block.z }

    fun relayExists(block: Block): Boolean =
        relays.any { it.world == block.world.name && it.x == block.x && it.y == block.y && it.z == block.z }

    // ---- Access / members --------------------------------------------------

    /** The nearest nexus in [location]'s world the player may use, within network range. */
    fun accessibleNexus(player: Player): Nexus? {
        val loc = player.location
        return nexuses
            .filter { it.world == player.world.name && it.isAllowed(player.uniqueId) }
            .filter { dist2(it, loc.x, loc.y, loc.z) <= networkRange * networkRange }
            .minByOrNull { dist2(it, loc.x, loc.y, loc.z) }
    }

    /** The nearest nexus the player owns (any distance), for share/unshare commands. */
    fun ownedNexusNear(player: Player): Nexus? {
        val loc = player.location
        return nexuses
            .filter { it.world == player.world.name && it.owner == player.uniqueId }
            .minByOrNull { dist2(it, loc.x, loc.y, loc.z) }
    }

    fun share(nexus: Nexus, member: UUID): Boolean = nexus.members.add(member).also { if (it) markDirty() }
    fun unshare(nexus: Nexus, member: UUID): Boolean = nexus.members.remove(member).also { if (it) markDirty() }

    /** Opens the read-only nexus view (stored energy + owner/members). */
    fun open(player: Player, nexus: Nexus) {
        val inv = Menu.create(NexusHolder(), Texts.line("gui.nexus.title"), Menu.ROOT_SIZE)
        val ownerName = Bukkit.getOfflinePlayer(nexus.owner).name ?: "?"
        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(
                Texts.line("gui.nexus.header"),
                *Texts.renderLines("gui.nexus.header-lore", "owner" to ownerName, "members" to nexus.members.size.toString()).toTypedArray(),
            ),
        )
        inv.setItem(11, Menu.button(Material.GOLD_BLOCK, Texts.line("gui.nexus.sun"), Texts.renderLines("gui.nexus.sun-lore", "value" to nexus.sun.toString(), "cap" to capacity.toString())))
        inv.setItem(15, Menu.button(Material.AMETHYST_BLOCK, Texts.line("gui.nexus.moon"), Texts.renderLines("gui.nexus.moon-lore", "value" to nexus.moon.toString(), "cap" to capacity.toString())))
        player.openInventory(inv)
    }

    // ---- Storage -----------------------------------------------------------

    fun get(nexus: Nexus, type: EnergyType): Int = if (type == EnergyType.SUN) nexus.sun else nexus.moon
    fun capacity(): Int = capacity

    fun add(nexus: Nexus, type: EnergyType, amount: Int): Int {
        if (amount <= 0) return 0
        val current = get(nexus, type)
        val stored = (current + amount).coerceAtMost(capacity)
        val added = stored - current
        if (added > 0) {
            if (type == EnergyType.SUN) nexus.sun = stored else nexus.moon = stored
            markDirty()
        }
        return added
    }

    fun spend(nexus: Nexus, type: EnergyType, amount: Int): Boolean {
        if (amount <= 0) return true
        if (get(nexus, type) < amount) return false
        if (type == EnergyType.SUN) nexus.sun -= amount else nexus.moon -= amount
        markDirty()
        return true
    }

    // ---- Production tick (towers -> network -> nexus) -----------------------

    /** Called each EnergyService cycle: connected towers feed their nexus; periodic autosave. */
    fun tick() {
        for (nexus in nexuses) {
            val world = plugin.server.getWorld(nexus.world) ?: continue
            if (!world.isChunkLoaded(nexus.x shr 4, nexus.z shr 4)) continue
            val reachable = reachableNodes(nexus)
            for (tower in plugin.energyTowers.all()) {
                if (tower.world != nexus.world) continue
                if (!producesNow(world, tower)) continue
                if (networkReaches(reachable, Node(tower.world, tower.x, tower.y, tower.z), networkRange)) {
                    add(nexus, tower.type, productionPerCycle)
                }
            }
            drawBeams(world, nexus, reachable)
        }
        if (++cyclesSinceSave >= SAVE_EVERY_CYCLES) {
            cyclesSinceSave = 0
            if (dirty) save()
        }
    }

    /** Nexus plus every relay reachable from it by hops within [networkRange]. */
    private fun reachableNodes(nexus: Nexus): List<Node> {
        val start = Node(nexus.world, nexus.x, nexus.y, nexus.z)
        return reachableNetworkNodes(start, relays.filter { it.world == nexus.world }, networkRange)
    }

    /**
     * Draws flowing dust beams along the live network (nexus → relays → towers) so the transport is
     * visible. Each relay links to its nearest already-reached node; producing towers link to their
     * nearest reached node. Only runs when a player is close, to keep it cheap.
     */
    private fun drawBeams(world: World, nexus: Nexus, reached: List<Node>) {
        val centre = org.bukkit.Location(world, nexus.x + 0.5, nexus.y + 0.5, nexus.z + 0.5)
        if (world.players.none { it.location.distanceSquared(centre) <= BEAM_VIEW_SQ }) return
        // Relay edges: link each reached relay to the nearest other reached node.
        reached.forEach { node ->
            if (node.x == nexus.x && node.y == nexus.y && node.z == nexus.z) return@forEach
            val parent = reached.filter { it != node }.minByOrNull { horizontal2(it, node) } ?: return@forEach
            beam(world, parent, node, BEAM_DUST)
        }
        // Tower edges.
        plugin.energyTowers.all().forEach { tower ->
            if (tower.world != nexus.world) return@forEach
            if (!producesNow(world, tower)) return@forEach
            val node = reached.minByOrNull { horizontal2(it, Node(tower.world, tower.x, tower.y, tower.z)) } ?: return@forEach
            if (!networkReaches(listOf(node), Node(tower.world, tower.x, tower.y, tower.z), networkRange)) return@forEach
            val dust = if (tower.type == EnergyType.SUN) SUN_BEAM else MOON_BEAM
            beamPoint(world, node.x + 0.5, node.y + 1.0, node.z + 0.5, tower.x + 0.5, tower.y + 0.5, tower.z + 0.5, dust)
        }
    }

    private fun beam(world: World, a: Node, b: Node, dust: org.bukkit.Particle.DustOptions) =
        beamPoint(world, a.x + 0.5, a.y + 1.0, a.z + 0.5, b.x + 0.5, b.y + 1.0, b.z + 0.5, dust)

    private fun beamPoint(world: World, ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double, dust: org.bukkit.Particle.DustOptions) {
        val dx = bx - ax; val dy = by - ay; val dz = bz - az
        val steps = maxOf(1, Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0).toInt())
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            world.spawnParticle(org.bukkit.Particle.DUST, ax + dx * t, ay + dy * t, az + dz * t, 1, 0.0, 0.0, 0.0, 0.0, dust)
        }
    }

    private fun horizontal2(a: Node, b: Node): Double {
        val dx = (a.x - b.x).toDouble(); val dz = (a.z - b.z).toDouble()
        return dx * dx + dz * dz
    }

    private fun producesNow(world: World, tower: EnergyTowers.Tower): Boolean {
        val daytime = world.time < 12300 || world.time > 23850
        val matchesTime = if (tower.type == EnergyType.SUN) daytime else !daytime
        if (!matchesTime) return false
        return towerCanSeeSky(
            tower.y,
            world.getHighestBlockYAt(tower.x, tower.z, org.bukkit.HeightMap.MOTION_BLOCKING),
        )
    }

    private fun dist2(nexus: Nexus, x: Double, y: Double, z: Double): Double {
        val dx = nexus.x + 0.5 - x; val dy = nexus.y + 0.5 - y; val dz = nexus.z + 0.5 - z
        return dx * dx + dy * dy + dz * dz
    }

    // ---- Persistence -------------------------------------------------------

    private fun markDirty() { dirty = true }

    fun save() {
        val yaml = YamlConfiguration()
        yaml.set("nexuses", nexuses.map(::encodeNexus))
        yaml.set("relays", relays.map { "${it.world};${it.x};${it.y};${it.z}" })
        runCatching { plugin.dataFolder.mkdirs(); yaml.save(file) }
            .onFailure { plugin.logger.warning("Could not save $FILE_NAME: ${it.message}") }
        dirty = false
    }

    private fun encodeNexus(n: Nexus): String =
        "${n.world};${n.x};${n.y};${n.z};${n.owner};${n.members.joinToString(",")};${n.sun};${n.moon}"

    private fun decodeNexus(encoded: String): Nexus? {
        val p = encoded.split(";")
        if (p.size != 8) return null
        val x = p[1].toIntOrNull() ?: return null
        val y = p[2].toIntOrNull() ?: return null
        val z = p[3].toIntOrNull() ?: return null
        val owner = runCatching { UUID.fromString(p[4]) }.getOrNull() ?: return null
        val members = p[5].split(",").mapNotNull { m -> runCatching { UUID.fromString(m) }.getOrNull() }.toMutableSet()
        return Nexus(p[0], x, y, z, owner, members, p[6].toIntOrNull() ?: 0, p[7].toIntOrNull() ?: 0)
    }

    private fun decodeNode(encoded: String): Node? {
        val p = encoded.split(";")
        if (p.size != 4) return null
        return Node(p[0], p[1].toIntOrNull() ?: return null, p[2].toIntOrNull() ?: return null, p[3].toIntOrNull() ?: return null)
    }

    data class Node(val world: String, val x: Int, val y: Int, val z: Int)

    enum class ClaimResult { CLAIMED, EXISTS }

    private companion object {
        const val FILE_NAME = "nexuses.yml"
        const val SAVE_EVERY_CYCLES = 30 // ~30s at the 20-tick EnergyService cycle
        const val BEAM_VIEW_SQ = 64.0 * 64.0
        val BEAM_DUST = org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0x9a, 0xe0, 0xff), 1.6f)
        val SUN_BEAM = org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0xff, 0xdf, 0x80), 1.6f)
        val MOON_BEAM = org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0xc0, 0xa8, 0xff), 1.6f)
    }
}

/** Towers are stored at their glow-ring crown; the end rod occupies the two blocks above it. */
internal fun towerCanSeeSky(crownY: Int, highestBlockingY: Int): Boolean = crownY + 2 >= highestBlockingY

/** Pure horizontal BFS used by both direct and relay-assisted tower connections. */
internal fun reachableNetworkNodes(
    start: NexusManager.Node,
    relays: List<NexusManager.Node>,
    range: Double,
): List<NexusManager.Node> {
    val pool = relays.toMutableList()
    val reached = mutableListOf(start)
    val frontier = ArrayDeque<NexusManager.Node>().apply { add(start) }
    while (frontier.isNotEmpty()) {
        val node = frontier.removeFirst()
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val relay = iterator.next()
            if (networkReaches(listOf(node), relay, range)) {
                iterator.remove()
                reached += relay
                frontier.add(relay)
            }
        }
    }
    return reached
}

internal fun networkReaches(
    reached: Iterable<NexusManager.Node>,
    target: NexusManager.Node,
    range: Double,
): Boolean = reached.any { node ->
    val dx = (node.x - target.x).toDouble()
    val dz = (node.z - target.z).toDouble()
    dx * dx + dz * dz <= range * range
}

/** One 輝能核心: a claimed location with an owner, optional members, and stored 日輝/月輝. */
class Nexus(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    var owner: UUID,
    val members: MutableSet<UUID> = mutableSetOf(),
    var sun: Int = 0,
    var moon: Int = 0,
) {
    fun isAllowed(uuid: UUID): Boolean = uuid == owner || uuid in members
}
