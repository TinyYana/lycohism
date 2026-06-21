package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

/**
 * Generates the 書房's natural compendium and per-player discovery record. All text and
 * colours come from lang.yml (books.compendium / books.record) so they stay readable on
 * the parchment background and editable without recompiling.
 */
class StudyBooks(private val plugin: Lycohism) {

    fun openCompendium(player: Player) {
        val discoveries = plugin.playerDataManager.get(player.uniqueId).discoveries
        val pages = mutableListOf(page(Texts.lines("books.compendium.intro")))

        for (entry in Texts.entries("books.compendium.entries")) {
            val requires = stringList(entry["requires"])
            val found = requires.isNotEmpty() && requires.all { it in discoveries }
            val lines = stringList(entry[if (found) "found" else "hidden"])
            if (lines.isNotEmpty()) pages.add(page(lines))
        }

        openBook(player, Texts.line("books.compendium.title"), pages)
    }

    fun openDiscoveryRecord(player: Player) {
        val discoveries = plugin.playerDataManager.get(player.uniqueId).discoveries
        val entries = Texts.entries("books.record.entries")
        val knownCount = entries.count { (it["id"]?.toString() ?: "") in discoveries }

        val header = Texts.render(
            "books.record.header",
            "count" to knownCount.toString(),
            "total" to entries.size.toString(),
        )
        val listLines = entries.map { entry ->
            val id = entry["id"]?.toString() ?: ""
            val path = if (id in discoveries) "books.record.entry-line" else "books.record.hidden-line"
            Texts.render(path, "item" to (entry["label"]?.toString() ?: ""))
        }
        val pages = mutableListOf(page(listOf(header) + Texts.lines("books.record.legend")))
        pages += listLines.chunked(RECORD_LINES_PER_PAGE).map(::page)

        openBook(
            player,
            Texts.line("books.record.title"),
            pages,
        )
    }

    private fun openBook(player: Player, title: String, pages: List<Component>) {
        val book = ItemStack(Material.WRITTEN_BOOK)
        book.editMeta { meta ->
            val bookMeta = meta as BookMeta
            bookMeta.title(Messages.parse(title))
            bookMeta.author(Component.text("Lycohism"))
            bookMeta.pages(pages)
        }
        player.openBook(book)
    }

    private fun page(lines: List<String>): Component = Messages.parse(lines.joinToString("\n"))

    private fun stringList(value: Any?): List<String> =
        (value as? List<*>)?.map { it.toString() } ?: emptyList()

    companion object {
        private const val RECORD_LINES_PER_PAGE = 10
    }
}
