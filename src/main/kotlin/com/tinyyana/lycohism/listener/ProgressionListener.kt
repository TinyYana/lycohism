package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.ProgressionHolder
import com.tinyyana.lycohism.util.SoundMusic
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent

/** Keeps the 調律之路 screen read-only: every click and drag is cancelled. */
class ProgressionListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is ProgressionHolder) return
        event.isCancelled = true
        if (event.rawSlot == plugin.progressionManager.backSlot()) {
            (event.whoClicked as? org.bukkit.entity.Player)?.let(plugin.study::open)
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is ProgressionHolder) event.isCancelled = true
    }

    @EventHandler
    fun onAdvancement(event: PlayerAdvancementDoneEvent) {
        // When one of our own progression advancements completes, that's a Lycohism chapter
        // unlocking — give it a short melodic flourish for atmosphere (the toast is silent and
        // its backdrop texture isn't ours to ship). The motif varies by chapter type so a base
        // facility, an expedition and the boss kill each sound distinct. Don't re-sync here:
        // this fired from our own award.
        if (event.advancement.key.namespace == LYCOHISM_NAMESPACE) {
            SoundMusic.play(plugin, event.player, themeFor(event.advancement.key.key.substringAfterLast('/')))
            return
        }
        // A vanilla advancement (e.g. entering the Nether) may unlock chapters — mirror them.
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.progressionManager.syncAdvancements(event.player)
        })
    }

    /** Picks a motif by chapter so milestones don't all sound the same (P6 音效變化). */
    private fun themeFor(stageId: String): SoundMusic.Theme = when (stageId) {
        // 收束終章：莊嚴鐘聲。
        "eclipse_warden", "facility_growth_3" -> SoundMusic.BELL
        // 家用設施落成：溫暖的豎琴收束。
        "workshop", "study", "greenhouse", "facility_growth" -> SoundMusic.HARP
        // 探索／遠征／世界：輕盈的長笛呼喚。
        "expedition", "radiant_vein", "tidal_depths", "lunar_waste", "eclipse_realm" -> SoundMusic.FLUTE
        else -> SoundMusic.CHIME
    }

    private companion object {
        const val LYCOHISM_NAMESPACE = "lycohism"
    }
}
