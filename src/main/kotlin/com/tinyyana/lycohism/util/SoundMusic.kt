package com.tinyyana.lycohism.util

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

/**
 * Short note-block motifs shared by live feedback and the admin sound sampler.
 *
 * Each theme is a hand-shaped little phrase (notes with their own instrument, pitch and timing)
 * rather than one fixed rising scale — so chapter unlocks, repairs and the boss kill each have
 * their own character instead of the same four ascending beeps. Pitch follows note-block tuning
 * (0.5 = F#3 … 1.0 = F#4 … 2.0 = F#5).
 */
object SoundMusic {

    /** One note in a motif: [sound] at [pitch], started [delay] ticks after the motif begins. */
    data class Note(val sound: Sound, val pitch: Float, val delay: Long)

    data class Theme(val id: String, val icon: Material, val volume: Float, val notes: List<Note>)

    private fun chime(pitch: Float, delay: Long) = Note(Sound.BLOCK_NOTE_BLOCK_CHIME, pitch, delay)
    private fun harp(pitch: Float, delay: Long) = Note(Sound.BLOCK_NOTE_BLOCK_HARP, pitch, delay)
    private fun flute(pitch: Float, delay: Long) = Note(Sound.BLOCK_NOTE_BLOCK_FLUTE, pitch, delay)
    private fun bell(pitch: Float, delay: Long) = Note(Sound.BLOCK_NOTE_BLOCK_BELL, pitch, delay)

    /** Twinkling open-then-sparkle phrase — the default chapter-unlock flourish. */
    val CHIME = Theme(
        "chime", Material.AMETHYST_SHARD, 0.65f,
        listOf(chime(1.0f, 0), chime(1.5f, 3), chime(1.33f, 6), chime(2.0f, 9)),
    )

    /** Warm, downward-resolving phrase — settling a facility/upgrade into place. */
    val HARP = Theme(
        "harp", Material.NOTE_BLOCK, 0.7f,
        listOf(harp(1.5f, 0), harp(1.33f, 4), harp(1.0f, 8), harp(0.75f, 13)),
    )

    /** Airy call-and-answer — light, exploratory (kept for the sampler / future hooks). */
    val FLUTE = Theme(
        "flute", Material.BAMBOO, 0.7f,
        listOf(flute(1.2f, 0), flute(1.8f, 5), flute(1.5f, 9)),
    )

    /** Slow, heavy toll resolving up — reserved for climactic moments (a boss falling). */
    val BELL = Theme(
        "bell", Material.BELL, 0.9f,
        listOf(bell(0.8f, 0), bell(0.8f, 7), chime(1.5f, 14), bell(1.2f, 16)),
    )

    val THEMES = listOf(CHIME, HARP, FLUTE, BELL)

    /** Plays [theme] to a single [player]. A shared boss kill reaches everyone because each
     *  nearby player's chapter advancement completes individually (see EclipseBoss.onDeath). */
    fun play(plugin: Lycohism, player: Player, theme: Theme) {
        theme.notes.forEach { note ->
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                player.playSound(player.location, note.sound, theme.volume, note.pitch)
            }, note.delay)
        }
    }
}
