package com.tinyyana.lycohism.util

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/** Short note-block melodies shared by live feedback and the admin sound sampler. */
object SoundMusic {

    data class Theme(val id: String, val icon: Material, val sound: Sound)

    val CHIME = Theme("chime", Material.AMETHYST_SHARD, Sound.BLOCK_NOTE_BLOCK_CHIME)
    val THEMES = listOf(
        CHIME,
        Theme("harp", Material.NOTE_BLOCK, Sound.BLOCK_NOTE_BLOCK_HARP),
        Theme("flute", Material.BAMBOO, Sound.BLOCK_NOTE_BLOCK_FLUTE),
        Theme("bell", Material.BELL, Sound.BLOCK_NOTE_BLOCK_BELL),
    )

    fun play(plugin: Lycohism, player: Player, theme: Theme) {
        PITCHES.forEachIndexed { step, pitch ->
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                player.playSound(player.location, theme.sound, 0.65f, pitch)
            }, step * STEP_TICKS)
        }
    }

    private const val STEP_TICKS = 3L
    private val PITCHES = floatArrayOf(0.8f, 1.0f, 1.2f, 1.6f)
}
