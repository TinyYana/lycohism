package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Advancements
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import kotlin.random.Random

/**
 * Drops Lycohism phenomenon materials from mob kills. Data-driven: any phenomenon that declares
 * a `mob-drops` map can fall from the listed mobs. Requires a player kill (so the drop can be
 * advancement-gated to the killer) — e.g. 燼華 only drops once the player has entered the Nether.
 */
class MobDropListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val type = event.entityType
        val worldName = event.entity.world.name
        for (phenomenon in plugin.phenomenonManager.all()) {
            val chance = phenomenon.mobDrops[type] ?: continue
            // Honour the world allow-list so expedition-only drops (e.g. 月輝核心) stay in their world.
            if (phenomenon.worlds.isNotEmpty() && worldName !in phenomenon.worlds) continue
            if (!Advancements.hasAll(killer, phenomenon.requiresAdvancements)) continue
            if (Random.nextDouble() > chance) continue
            event.drops.add(plugin.phenomenonManager.createItem(phenomenon))
            com.tinyyana.lycohism.util.Audit.log(killer, "mob-drop", "${phenomenon.id} from ${type.name}")
            plugin.playerDataManager.discover(killer.uniqueId, phenomenon.id)
        }
    }
}
