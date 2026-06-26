package com.tinyyana.lycohism.util

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta

object Messages {

    var prefix: String = "[Lycohism] "

    /** Parses MiniMessage tags and legacy `&` codes to `§` sequences. */
    fun format(text: String): String = MiniText.parse(text)

    fun send(sender: CommandSender, text: String) {
        sender.sendMessage(format(prefix + text))
    }

    fun actionBar(player: Player, text: String) {
        @Suppress("DEPRECATION")
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent(format(text)),
        )
    }

    fun applyDisplayName(meta: ItemMeta, text: String) {
        @Suppress("DEPRECATION")
        meta.setDisplayName(format(text))
    }

    fun applyLore(meta: ItemMeta, lines: List<String>) {
        @Suppress("DEPRECATION")
        meta.setLore(lines.map(::format))
    }

    fun loreLine(text: String): String = format(text)

    @Suppress("DEPRECATION")
    fun getLore(meta: ItemMeta): MutableList<String> = meta.lore?.toMutableList() ?: mutableListOf()
}
