package com.tinyyana.lycohism.tool

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.persistence.PersistentDataType

/**
 * 調律手冊 — staged guidance. A plain book item opens a generated written book whose
 * pages reflect the player's discoveries. All text (and colours) come from lang.yml so
 * it stays readable on the parchment background and is editable without recompiling.
 */
class TuningManual(private val plugin: Lycohism) {

    fun isManual(item: ItemStack?): Boolean = Items.idOf(item) == ID

    fun createItem(): ItemStack {
        val item = ItemStack(Material.BOOK)
        item.editMeta { meta ->
            meta.displayName(noItalic(Texts.line("books.manual.item-name")))
            meta.lore(Texts.lines("books.manual.item-lore").map { noItalic(it) })
            meta.persistentDataContainer.set(Keys.itemId, PersistentDataType.STRING, ID)
            meta.setEnchantmentGlintOverride(true)
        }
        return item
    }

    /** Gives the manual once, the first time a player would receive it. Returns true if given. */
    fun grantIfAbsent(player: Player): Boolean {
        if (!plugin.playerDataManager.discover(player.uniqueId, GRANTED_FLAG)) return false
        Items.give(player, createItem())
        return true
    }

    /** Opens a freshly generated written book reflecting current progress. */
    fun open(player: Player) {
        val discoveries = plugin.playerDataManager.get(player.uniqueId).discoveries
        val book = ItemStack(Material.WRITTEN_BOOK)
        book.editMeta { meta ->
            val bookMeta = meta as BookMeta
            bookMeta.title(Messages.parse(Texts.line("books.manual.title")))
            bookMeta.author(Component.text("Lycohism"))
            bookMeta.pages(buildPages(discoveries))
        }
        player.openBook(book)
    }

    private fun buildPages(discoveries: Set<String>): List<Component> {
        val pages = mutableListOf<Component>()
        pages.add(page(Texts.lines("books.manual.cover")))

        for (entry in Texts.entries("books.manual.entries")) {
            val requires = stringList(entry["requires"])
            val any = entry["requires-any"] == true
            val done = if (any) requires.any { it in discoveries }
            else requires.isNotEmpty() && requires.all { it in discoveries }

            val lines = stringList(entry[if (done) "done" else "hint"])
            if (lines.isNotEmpty()) pages.add(page(lines))
        }
        return pages
    }

    private fun page(lines: List<String>): Component = Messages.parse(lines.joinToString("\n"))

    private fun noItalic(text: String): Component =
        Messages.parse(text).decoration(TextDecoration.ITALIC, false)

    private fun stringList(value: Any?): List<String> =
        (value as? List<*>)?.map { it.toString() } ?: emptyList()

    companion object {
        const val ID = "tuning_manual"
        private const val GRANTED_FLAG = "tuning_manual"
    }
}
