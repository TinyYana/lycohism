package com.tinyyana.lycohism.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Central place for player-facing text. Uses MiniMessage so message styling stays
 * data-driven (config / future content files) rather than hardcoded colour codes.
 */
object Messages {

    private val mm = MiniMessage.miniMessage()

    /** Prefix prepended to [send]. Set from config.yml on enable/reload. */
    var prefix: String = "[Lycohism] "

    fun parse(text: String): Component = mm.deserialize(text)

    /** Sends a prefixed, MiniMessage-formatted line to a player or the console. */
    fun send(sender: CommandSender, text: String) {
        sender.sendMessage(mm.deserialize(prefix + text))
    }

    /** Sends an action-bar message (no prefix) for lightweight, repeatable feedback. */
    fun actionBar(player: Player, text: String) {
        player.sendActionBar(mm.deserialize(text))
    }
}
