package com.tinyyana.lycohism.boss

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.facility.Cost
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Wither
import org.bukkit.entity.WitherSkeleton
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable

/**
 * v0.8 BOSS「蝕影守望者」— the convergence of the 日 and 月 progression branches.
 *
 * Built lean (ponytail): the boss is a heavily-buffed vanilla [Wither], so it brings a real boss
 * AI (flight, skull barrage) AND its native boss bar for free — no custom AI or bar to maintain.
 * We only add: a summon ritual at the 日月儀 multiblock, one mid-fight phase shift, an arena-damage
 * guard (so the Wither doesn't chew the fight world), and the 蝕輝結晶 drop that unlocks Lv3 facilities.
 */
class EclipseBoss(private val plugin: Lycohism) : Listener {

    private var health = 450.0
    private var crystalDrop = 1

    init { load() }

    fun load() {
        val cfg = plugin.config.getConfigurationSection("eclipse-boss")
        health = (cfg?.getDouble("health", 450.0) ?: 450.0).coerceAtLeast(20.0)
        crystalDrop = (cfg?.getInt("crystal-drop", 1) ?: 1).coerceAtLeast(0)
    }

    // ── Summon ritual: right-click the built 日月儀 controller in 暮蝕之境 ──────────────
    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        val dial = plugin.multiblockRegistry.get(DIAL_ID) ?: return
        if (block.type != dial.controller) return
        if (dial.detectRotation(block.world, block.x, block.y, block.z) == null) return
        event.isCancelled = true
        val player = event.player

        if (plugin.expeditionManager.expeditionAt(player.world)?.id != ECLIPSE_WORLD_ID) {
            Messages.send(player, Texts.line("messages.boss.wrong-world"))
            return
        }
        if (activeBossNear(block.location)) {
            Messages.send(player, Texts.line("messages.boss.already"))
            return
        }
        // Summon needs BOTH cores — the 日 line's 日輝核心 (from 潮汐深淵) and the 月 line's 月輝核心
        // (from 永夜荒原). This is where the two branches actually converge (item v0.8.1 #5).
        val cost = Cost.parse(listOf("$SUN_CORE_ID:1", "$MOON_CORE_ID:1"), plugin)
        if (!Cost.hasAll(player, cost)) {
            Messages.send(player, Texts.line("messages.boss.need-core"))
            return
        }
        Cost.consume(player, cost)
        summon(block.location.add(0.5, 2.0, 0.5))
        plugin.server.broadcast(Messages.parse(Texts.render("messages.boss.summoned", "player" to player.name)))
    }

    /** Spawns the buffed Wither and starts its single phase-shift watcher. */
    fun summon(loc: Location) {
        val world = loc.world ?: return
        world.strikeLightningEffect(loc)
        world.spawn(loc, Wither::class.java) { wither ->
            wither.customName(Messages.parse(Texts.line("content-names.$ENTITY_NAME_ID")))
            wither.isCustomNameVisible = true
            wither.persistentDataContainer.set(Keys.bossTag, PersistentDataType.INTEGER, PHASE_ONE)
            wither.getAttribute(Attribute.MAX_HEALTH)?.let {
                it.baseValue = health
                wither.health = health
            }
            phaseWatcher(wither).runTaskTimer(plugin, 20L, 20L)
            skillTicker(wither).runTaskTimer(plugin, SKILL_PERIOD, SKILL_PERIOD)
        }
    }

    /** Every few seconds the守望者 uses one of its eclipse skills on nearby players; faster in phase 2. */
    private fun skillTicker(wither: Wither) = object : BukkitRunnable() {
        override fun run() {
            if (!wither.isValid || wither.isDead) { cancel(); return }
            val players = wither.world.getNearbyPlayers(wither.location, 28.0).toList()
            if (players.isEmpty()) return
            val phaseTwo = wither.persistentDataContainer.get(Keys.bossTag, PersistentDataType.INTEGER) == PHASE_TWO
            useSkill(wither, players)
            // 月相階段：更狂暴——再追加一輪暗蝕彈幕。
            if (phaseTwo) skullBarrage(wither, players)
        }
    }

    private fun useSkill(wither: Wither, players: List<org.bukkit.entity.Player>) {
        when ((0..2).random()) {
            0 -> skullBarrage(wither, players)
            1 -> eclipseNova(wither, players)
            else -> voidPull(wither, players)
        }
    }

    /** 暗蝕彈幕：朝最近的幾位玩家連射蓄能凋零頭骨。 */
    private fun skullBarrage(wither: Wither, players: List<org.bukkit.entity.Player>) {
        players.sortedBy { it.location.distanceSquared(wither.location) }.take(3).forEach { target ->
            val dir = target.eyeLocation.toVector().subtract(wither.eyeLocation.toVector())
            if (dir.lengthSquared() < 1.0e-4) return@forEach
            runCatching {
                val skull = wither.launchProjectile(org.bukkit.entity.WitherSkull::class.java, dir.normalize())
                skull.isCharged = true
            }
        }
        wither.world.playSound(wither.location, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.7f)
    }

    /** 蝕之爆發：對周圍玩家施加失明＋凋零並擊退，伴隨一圈蝕色粒子。 */
    private fun eclipseNova(wither: Wither, players: List<org.bukkit.entity.Player>) {
        players.filter { it.location.distanceSquared(wither.location) <= 12.0 * 12.0 }.forEach { p ->
            p.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true))
            p.addPotionEffect(PotionEffect(PotionEffectType.WITHER, 80, 0, false, true))
            val away = p.location.toVector().subtract(wither.location.toVector())
            if (away.lengthSquared() > 1.0e-4) p.velocity = away.normalize().multiply(1.1).setY(0.55)
        }
        wither.world.spawnParticle(Particle.DUST, wither.location.clone().add(0.0, 2.0, 0.0), 140, 7.0, 2.5, 7.0, 0.0, ECLIPSE_DUST)
        wither.world.playSound(wither.location, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f)
    }

    /** 蝕引：把周圍玩家朝守望者拉近，逼近戰。 */
    private fun voidPull(wither: Wither, players: List<org.bukkit.entity.Player>) {
        players.forEach { p ->
            val pull = wither.location.toVector().subtract(p.location.toVector()).multiply(0.16)
            p.velocity = p.velocity.add(pull)
        }
        wither.world.spawnParticle(Particle.DUST, wither.location.clone().add(0.0, 2.0, 0.0), 60, 1.0, 1.5, 1.0, 0.0, ECLIPSE_DUST)
        wither.world.playSound(wither.location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f)
    }

    /** Every second: at half health, shift to the 月相 phase once — haste/strength + two skeleton adds. */
    private fun phaseWatcher(wither: Wither) = object : BukkitRunnable() {
        override fun run() {
            if (!wither.isValid || wither.isDead) { cancel(); return }
            val phase = wither.persistentDataContainer.get(Keys.bossTag, PersistentDataType.INTEGER) ?: PHASE_ONE
            val max = wither.getAttribute(Attribute.MAX_HEALTH)?.value ?: health
            if (phase == PHASE_ONE && wither.health <= max / 2.0) {
                wither.persistentDataContainer.set(Keys.bossTag, PersistentDataType.INTEGER, PHASE_TWO)
                wither.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false))
                wither.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, false, false))
                wither.world.spawnParticle(Particle.DUST, wither.location, 60, 1.5, 1.5, 1.5, 0.0, ECLIPSE_DUST)
                repeat(2) {
                    wither.world.spawn(wither.location.add(0.0, -1.0, 0.0), WitherSkeleton::class.java) { add ->
                        add.persistentDataContainer.set(Keys.bossTag, PersistentDataType.INTEGER, MINION)
                    }
                }
                wither.world.getNearbyPlayers(wither.location, 40.0).forEach {
                    Messages.send(it, Texts.line("messages.boss.phase-2"))
                    it.playSound(it.location, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f)
                }
            }
        }
    }

    // ── Death: drop the 蝕輝結晶 and announce the kill ──────────────────────────────
    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        val tag = event.entity.persistentDataContainer.get(Keys.bossTag, PersistentDataType.INTEGER) ?: return
        if (tag == MINION) return // adds: nothing special
        if (crystalDrop > 0) {
            plugin.phenomenonManager.get(com.tinyyana.lycohism.facility.FacilityUpgrade.ECLIPSE_CRYSTAL_ID)?.let {
                event.drops.add(plugin.phenomenonManager.createItem(it, crystalDrop))
            }
        }
        event.entity.world.getNearbyPlayers(event.entity.location, 48.0).forEach {
            Messages.send(it, Texts.line("messages.boss.defeated"))
            plugin.playerDataManager.discover(it.uniqueId, ENTITY_NAME_ID)
        }
    }

    // ── Arena-damage guard: a Wither otherwise wrecks the fight world ──────────────
    @EventHandler
    fun onExplode(event: EntityExplodeEvent) {
        if (isBoss(event.entity)) event.blockList().clear()
    }

    @EventHandler
    fun onChangeBlock(event: EntityChangeBlockEvent) {
        if (isBoss(event.entity)) event.isCancelled = true
    }

    private fun isBoss(entity: org.bukkit.entity.Entity): Boolean =
        (entity.persistentDataContainer.get(Keys.bossTag, PersistentDataType.INTEGER) ?: MINION) != MINION

    private fun activeBossNear(loc: Location): Boolean =
        loc.world?.getNearbyEntities(loc, 64.0, 64.0, 64.0)?.any {
            it is LivingEntity && (it.persistentDataContainer.get(Keys.bossTag, PersistentDataType.INTEGER) == PHASE_ONE ||
                it.persistentDataContainer.get(Keys.bossTag, PersistentDataType.INTEGER) == PHASE_TWO)
        } ?: false

    companion object {
        const val DIAL_ID = "eclipse_dial"
        const val ENTITY_NAME_ID = "eclipse_warden"
        const val ECLIPSE_WORLD_ID = "eclipse_realm"
        private const val SUN_CORE_ID = "sun_core"
        private const val MOON_CORE_ID = "moon_core"
        private const val PHASE_ONE = 1
        private const val PHASE_TWO = 2
        private const val MINION = 0
        private const val SKILL_PERIOD = 60L // ~3s between skills
        private val ECLIPSE_DUST = Particle.DustOptions(org.bukkit.Color.fromRGB(0xB0, 0x7C, 0xFF), 1.6f)
    }
}
