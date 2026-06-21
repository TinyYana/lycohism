package com.tinyyana.lycohism.gui

import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/** An inventory holder that exposes its backing inventory; shared by all Lycohism menus. */
interface BackedHolder : InventoryHolder {
    var backing: Inventory
    override fun getInventory(): Inventory = backing
}

/**
 * Shared building blocks for Lycohism GUIs so every facility menu looks and behaves the
 * same: same filler, same header/breadcrumb, same back button in the same place. This is
 * what gives the menus a consistent "where am I / what can I do here" feel.
 */
object Menu {

    const val ROOT_SIZE = 18
    const val COMPACT_SIZE = 27
    const val EXTENDED_SIZE = 36
    const val LARGE_SIZE = 54
    const val HEADER_SLOT = 4
    val BACK_SLOT = backSlotAfter(listOf(10))

    private val SEPARATOR get() = Texts.line("gui.breadcrumb-separator")

    /** Creates a filled inventory bound to [holder] with a breadcrumb [title]. */
    fun create(holder: BackedHolder, title: String, size: Int = COMPACT_SIZE): Inventory {
        require(size == ROOT_SIZE || size == COMPACT_SIZE || size == EXTENDED_SIZE || size == LARGE_SIZE) { "Menu size must be 18, 27, 36, or 54." }
        val inv = Bukkit.createInventory(holder, size, Messages.parse(title))
        holder.backing = inv
        fill(inv)
        return inv
    }

    fun fill(inv: Inventory) {
        val material = Material.matchMaterial(Texts.line("gui.filler-material"))
            ?: Material.GRAY_STAINED_GLASS_PANE
        val pane = button(material, " ", emptyList())
        for (i in 0 until inv.size) inv.setItem(i, pane)
    }

    /** Centers the back button one row below the last interactive content item. */
    fun backSlotAfter(contentSlots: Iterable<Int>): Int {
        val slot = ((contentSlots.maxOrNull() ?: HEADER_SLOT) / 9 + 1) * 9 + 4
        require(slot < LARGE_SIZE) { "Menu content leaves no row for a back button." }
        return slot
    }

    /** A breadcrumb-style title, e.g. "工房 ▸ 工具製作". */
    fun title(vararg crumbs: String): String = crumbs.joinToString(SEPARATOR)

    /** The header item placed at the top of a menu: names the view and says what it's for. */
    fun header(name: String, vararg description: String): ItemStack {
        val material = Material.matchMaterial(Texts.line("gui.header-material"))
            ?: Material.PINK_PETALS
        return button(material, name, description.toList())
    }

    /** Standard back button. Use [backSlotAfter] when content spans more than one row. */
    fun back(): ItemStack = button(
        Material.matchMaterial(Texts.line("gui.back-material")) ?: Material.ARROW,
        Texts.line("gui.back"),
        Texts.lines("gui.back-lore"),
    )

    fun button(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(Messages.parse(name).decoration(TextDecoration.ITALIC, false))
            if (lore.isNotEmpty()) {
                meta.lore(lore.map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
            }
        }
        return item
    }
}
