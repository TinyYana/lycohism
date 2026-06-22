package com.tinyyana.lycohism.energy

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.tool.EnergyCrystal
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.HeightMap
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

/**
 * Drives the 輝能 economy each second (v0.6.1 model): the player charges *themselves* passively
 * under an open sky — 日輝 by day, 月輝 by night — and when their pool is full the surplus overflows
 * into a 蓄能晶 they carry. An always-on boss bar makes the pool readable at a glance (the old
 * hold-the-crystal HUD was unintuitive). Standing near a 日晷 / 月晷 tower boosts the matching
 * charge and shows the tower's particle aura.
 */
class EnergyService(private val plugin: Lycohism) {

    private var task: BukkitTask? = null
    private val bars = HashMap<UUID, BossBar>()

    private var baseCharge = 2
    private var towerBonus = 3
    private var towerRadius = 16.0

    init {
        load()
    }

    fun load() {
        val node = plugin.config.getConfigurationSection("energy")
        baseCharge = (node?.getInt("base-charge", baseCharge) ?: baseCharge).coerceAtLeast(1)
        towerBonus = (node?.getInt("tower-bonus", towerBonus) ?: towerBonus).coerceAtLeast(1)
        towerRadius = (node?.getDouble("tower-radius", towerRadius) ?: towerRadius).coerceAtLeast(1.0)
    }

    fun start() {
        stop()
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, INTERVAL, INTERVAL)
    }

    fun stop() {
        task?.cancel()
        task = null
        bars.keys.mapNotNull(plugin.server::getPlayer).forEach { p -> bars[p.uniqueId]?.let(p::hideBossBar) }
        bars.clear()
    }

    /** Drops a player's boss bar on quit so it isn't leaked. */
    fun clear(player: Player) {
        bars.remove(player.uniqueId)?.let(player::hideBossBar)
    }

    private fun tick() {
        for (player in plugin.server.onlinePlayers) {
            accrue(player)
            updateBar(player)
        }
        plugin.energyTowers.all().forEach(::towerAura)
        plugin.nexusManager.tick()
        plugin.automationManager.tick()
    }

    private fun accrue(player: Player) {
        if (player.world.environment != World.Environment.NORMAL) return
        if (!underOpenSky(player)) return
        val type = if (isDay(player.world)) EnergyType.SUN else EnergyType.MOON
        var rate = if (plugin.energyTowers.nearActive(player.location, type, towerRadius)) baseCharge * towerBonus else baseCharge
        // 永夜荒原 (and any world with an energy-multiplier) charges faster, so the trip pays off.
        val worldMultiplier = plugin.expeditionManager.expeditionAt(player.world)?.energyMultiplier ?: 1.0
        rate = (rate * worldMultiplier).toInt().coerceAtLeast(if (worldMultiplier > 0) 1 else 0)
        val intoPool = plugin.energyManager.add(player, type, rate, persist = false)
        val overflow = rate - intoPool
        val intoCrystal = if (overflow > 0) fillCrystal(player, type, overflow) else 0
        if (intoPool > 0 || intoCrystal > 0) {
            player.world.spawnParticle(
                if (type == EnergyType.SUN) Particle.END_ROD else Particle.WITCH,
                player.location.add(0.0, 1.1, 0.0), 2, 0.25, 0.3, 0.25, 0.0,
            )
        }
    }

    /** Fills carried 蓄能晶 with overflow; returns the total charge absorbed. */
    private fun fillCrystal(player: Player, type: EnergyType, amount: Int): Int {
        var remaining = amount
        val contents = player.inventory.contents
        for (i in contents.indices) {
            if (remaining <= 0) break
            val stack = contents[i] ?: continue
            if (Items.idOf(stack) != EnergyCrystal.ID) continue
            val added = plugin.energyCrystal.addCharge(stack, type, remaining)
            if (added > 0) {
                player.inventory.setItem(i, stack)
                remaining -= added
            }
        }
        return amount - remaining
    }

    private fun updateBar(player: Player) {
        if (!engaged(player)) {
            clear(player)
            return
        }
        val sun = plugin.energyManager.get(player, EnergyType.SUN)
        val moon = plugin.energyManager.get(player, EnergyType.MOON)
        val sunCap = plugin.energyManager.cap(EnergyType.SUN)
        val moonCap = plugin.energyManager.cap(EnergyType.MOON)
        val name = Messages.parse(
            Texts.render(
                "messages.energy.bar",
                "sun" to sun.toString(), "sun_cap" to sunCap.toString(),
                "moon" to moon.toString(), "moon_cap" to moonCap.toString(),
            ),
        )
        val progress = ((sun + moon).toFloat() / (sunCap + moonCap)).coerceIn(0f, 1f)
        val color = if (isDay(player.world)) BossBar.Color.YELLOW else BossBar.Color.BLUE
        val bar = bars[player.uniqueId]
        if (bar == null) {
            bars[player.uniqueId] = BossBar.bossBar(name, progress, color, BossBar.Overlay.PROGRESS)
                .also(player::showBossBar)
        } else {
            bar.name(name)
            bar.progress(progress)
            bar.color(color)
        }
    }

    private fun engaged(player: Player): Boolean {
        return Items.idOf(player.inventory.itemInMainHand) != null ||
            Items.idOf(player.inventory.itemInOffHand) != null
    }

    private fun towerAura(tower: EnergyTowers.Tower) {
        val world = plugin.server.getWorld(tower.world) ?: return
        if (!world.isChunkLoaded(tower.x shr 4, tower.z shr 4)) return
        val centre = centre(world, tower)
        val nearby = world.players.filter { it.location.distanceSquared(centre) <= AURA_RANGE_SQ }
        if (nearby.isEmpty()) return
        // Finding a tower (natural or built) reveals it, so the 調律之路 progresses by exploration now
        // that towers can't be hand-activated (v0.7.5 #1). discover() is a no-op once already known.
        val towerId = if (tower.type == EnergyType.SUN) "sun_tower" else "moon_tower"
        nearby.forEach { plugin.playerDataManager.discover(it.uniqueId, towerId) }

        // Thicker aura (v0.7.5 #7): a dense crown burst, a rising light column up the spire, and a
        // slow swirl ring so the tower reads as actively channelling energy.
        val crown = if (tower.type == EnergyType.SUN) Particle.END_ROD else Particle.WITCH
        val accent = if (tower.type == EnergyType.SUN) Particle.WAX_ON else Particle.WAX_OFF
        val cx = tower.x + 0.5; val cz = tower.z + 0.5
        world.spawnParticle(crown, cx, tower.y + 0.6, cz, 40, 0.5, 0.9, 0.5, 0.02)
        // Light column up the 15-block spire.
        for (dy in -TOWER_CORE_HEIGHT..1 step 2) {
            world.spawnParticle(crown, cx, tower.y + dy + 0.5, cz, 2, 0.12, 0.25, 0.12, 0.0)
        }
        // Swirl ring around the crown.
        val phase = (world.fullTime % 40L) / 40.0 * Math.PI * 2
        for (i in 0 until 6) {
            val a = phase + i * Math.PI / 3
            world.spawnParticle(accent, cx + Math.cos(a) * 1.4, tower.y + 0.8, cz + Math.sin(a) * 1.4, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun centre(world: World, tower: EnergyTowers.Tower) =
        org.bukkit.Location(world, tower.x + 0.5, tower.y + 0.5, tower.z + 0.5)

    private fun isDay(world: World): Boolean {
        val time = world.time
        return time < 12300 || time > 23850
    }

    private fun underOpenSky(player: Player): Boolean {
        val loc = player.location
        return loc.blockY >= player.world.getHighestBlockYAt(loc.blockX, loc.blockZ, HeightMap.MOTION_BLOCKING)
    }

    private companion object {
        const val INTERVAL = 20L
        const val AURA_RANGE_SQ = 48.0 * 48.0
        const val TOWER_CORE_HEIGHT = 15 // crown sits this far above the tower base
    }
}
