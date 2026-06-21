package com.tinyyana.lycohism.util

import com.tinyyana.lycohism.Lycohism
import org.bukkit.Location
import org.bukkit.entity.Player
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Audit trail for everything this plugin produces or moves (TinyYana: 「插件產出的東西都至少要有
 * LOG 紀錄去向」). One structured line per event — item grants, mob/chest drops, crafts, energy
 * spends, structure claims — to a dedicated file, separate from the console, for tracking dupes and
 * economy on a multiplayer server.
 *
 * ponytail: buffered append with a flush per write. Event volume is low (user-initiated actions, not
 * per-tick), so this is plenty; move writing to an async queue only if a busy server ever shows it
 * costing main-thread time.
 */
object Audit {

    private var enabled = true
    private var writer: BufferedWriter? = null
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun init(plugin: Lycohism) {
        close()
        enabled = plugin.config.getBoolean("audit.enabled", true)
        if (!enabled) return
        runCatching {
            val dir = File(plugin.dataFolder, "logs").apply { mkdirs() }
            writer = BufferedWriter(FileWriter(File(dir, "lycohism-audit.log"), true))
        }.onFailure {
            plugin.logger.warning("Could not open audit log: ${it.message}")
            enabled = false
        }
    }

    /** Logs an event tied to a player, including their location. */
    fun log(player: Player, action: String, detail: String) =
        write(player.name, action, detail, where(player.location))

    /** Logs an event with an explicit actor (e.g. "console", "structure"). */
    fun log(who: String, action: String, detail: String, location: String = "") =
        write(who, action, detail, location)

    private fun write(who: String, action: String, detail: String, location: String) {
        val out = writer ?: return
        val line = buildString {
            append(LocalDateTime.now().format(stamp)).append(" | ")
            append(who).append(" | ").append(action).append(" | ").append(detail)
            if (location.isNotEmpty()) append(" | ").append(location)
            append('\n')
        }
        synchronized(this) {
            runCatching { out.write(line); out.flush() }
        }
    }

    private fun where(loc: Location): String =
        "${loc.world?.name ?: "?"}@${loc.blockX},${loc.blockY},${loc.blockZ}"

    fun close() {
        synchronized(this) {
            runCatching { writer?.flush(); writer?.close() }
            writer = null
        }
    }
}
