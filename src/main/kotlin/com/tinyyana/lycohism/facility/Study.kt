package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.gui.StudyHolder
import com.tinyyana.lycohism.gui.StudyMenu
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.tool.MoonPouch
import com.tinyyana.lycohism.tool.WindVane
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * 書房 — a small record facility. Per-player repair state; opened in-world by interacting
 * with a bookshelf (see FacilityAccessListener) or via /lycohism study.
 *
 * Its contents are grouped into categories (書冊 / 器物 / 遠征) behind a small root menu, the
 * same shape as the 工房. This keeps each screen readable and means new books or tools just drop
 * into the right leaf instead of crowding one flat page as the facility grows.
 */
class Study(private val plugin: Lycohism) {

    private var repairCost: List<String> = listOf("rain_breath:2", "BOOKSHELF:4", "LECTERN:1")

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("study") ?: return
        repairCost = node.getStringList("repair-cost").ifEmpty { repairCost }
    }

    fun open(player: Player) = openMain(player)

    // ---- Menus -------------------------------------------------------------

    private fun openMain(player: Player) {
        val sealed = level(player) <= 0
        val title = Texts.line(if (sealed) "gui.study.sealed" else "gui.study.root")
        val inv = create(StudyMenu.MAIN, title)

        val headerLore = if (sealed) {
            Texts.lines("gui.study.header-sealed-lore")
        } else {
            Texts.lines("gui.study.header-main-lore")
        }
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.study.header-main"), *headerLore.toTypedArray()))

        if (sealed) {
            inv.setItem(
                SLOT_REPAIR,
                Menu.button(Material.CHISELED_BOOKSHELF, Texts.line("gui.study.repair"), buildList {
                    addAll(Texts.lines("gui.study.repair-lore"))
                    add("")
                    add(Texts.line("gui.common.requires"))
                    addAll(FacilityUi.costLines(Cost.parse(repairCost, plugin)))
                    add("")
                    add(Texts.line("gui.common.click-repair"))
                }),
            )
        } else {
            inv.setItem(SLOT_BOOKS, Menu.button(Material.BOOKSHELF, Texts.line("gui.study.books-leaf"), Texts.lines("gui.study.books-lore")))
            inv.setItem(SLOT_TOOLS, Menu.button(Material.COMPASS, Texts.line("gui.study.tools-leaf"), Texts.lines("gui.study.tools-lore")))
            inv.setItem(SLOT_PROGRESS, Menu.button(Material.NETHER_STAR, Texts.line("gui.study.progress-leaf"), Texts.lines("gui.study.progress-lore")))
            inv.setItem(SLOT_EXPEDITION, Menu.button(Material.FILLED_MAP, Texts.line("gui.study.expedition-leaf"), Texts.lines("gui.study.expedition-lore")))
            inv.setItem(SLOT_STATUS, Menu.button(Material.LECTERN, Texts.line("gui.study.status"), Texts.renderLines("gui.study.status-lore", "level" to level(player).toString())))
        }
        player.openInventory(inv)
    }

    private fun openBooks(player: Player) {
        val inv = create(StudyMenu.BOOKS, Menu.title(Texts.line("gui.study.root"), Texts.line("gui.study.books-leaf")))
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.study.books-leaf"), *Texts.lines("gui.study.header-books-lore").toTypedArray()))
        inv.setItem(SLOT_MANUAL, Menu.button(Material.BOOK, Texts.line("gui.study.manual"), Texts.lines("gui.study.manual-lore")))
        inv.setItem(SLOT_COMPENDIUM, Menu.button(Material.WRITTEN_BOOK, Texts.line("gui.study.compendium"), Texts.lines("gui.study.compendium-lore")))
        inv.setItem(SLOT_RECORD, Menu.button(Material.KNOWLEDGE_BOOK, Texts.line("gui.study.record"), Texts.lines("gui.study.record-lore")))
        inv.setItem(Menu.BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    private fun openTools(player: Player) {
        val inv = create(StudyMenu.TOOLS, Menu.title(Texts.line("gui.study.root"), Texts.line("gui.study.tools-leaf")))
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.study.tools-leaf"), *Texts.lines("gui.study.header-tools-lore").toTypedArray()))
        putCraftButton(inv, SLOT_WIND_VANE, player, plugin.windVane.createItem(), plugin.windVane.cost)
        putCraftButton(inv, SLOT_MOON_POUCH, player, plugin.moonPouch.createItem(), plugin.moonPouch.cost)
        if (level(player) >= 2) {
            putCraftButton(inv, SLOT_STAR_COMPASS, player, plugin.starCompass.createItem(), plugin.starCompass.craftCost)
        }
        inv.setItem(Menu.BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    private fun openExpedition(player: Player) {
        val inv = create(StudyMenu.EXPEDITION, Menu.title(Texts.line("gui.study.root"), Texts.line("gui.study.expedition-leaf")))
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.study.expedition-leaf"), *Texts.lines("gui.study.header-expedition-lore").toTypedArray()))
        if (plugin.expeditionManager.expeditionAt(player.world) != null) {
            // Inside any expedition world: a single button returns home.
            inv.setItem(SLOT_GATE, Menu.button(Material.OAK_DOOR, Texts.line("gui.study.expedition-return"), Texts.lines("gui.study.expedition-return-lore")))
        } else {
            plugin.expeditionManager.all().take(EXPEDITION_SLOTS.size).forEachIndexed { index, expedition ->
                inv.setItem(EXPEDITION_SLOTS[index], expeditionButton(player, expedition))
            }
        }
        inv.setItem(Menu.BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    /** One expedition entry, shown locked until the player meets its advancement gate. */
    private fun expeditionButton(player: Player, expedition: com.tinyyana.lycohism.expedition.Expedition): ItemStack =
        if (plugin.expeditionManager.isUnlocked(player, expedition)) {
            Menu.button(
                Material.FILLED_MAP,
                Texts.render("gui.study.expedition-enter-name", "name" to expedition.displayName),
                Texts.lines("gui.study.expedition-enter-lore"),
            )
        } else {
            Menu.button(
                Material.BARRIER,
                Texts.render("gui.study.expedition-locked-name", "name" to expedition.displayName),
                Texts.lines("gui.study.gate-locked-lore"),
            )
        }

    // ---- Click handling ----------------------------------------------------

    fun handleClick(player: Player, holder: StudyHolder, slot: Int) {
        when (holder.menu) {
            StudyMenu.MAIN -> handleMain(player, slot)
            StudyMenu.BOOKS -> handleBooks(player, slot)
            StudyMenu.TOOLS -> handleTools(player, slot)
            StudyMenu.EXPEDITION -> handleExpedition(player, slot)
        }
    }

    private fun handleMain(player: Player, slot: Int) {
        if (level(player) <= 0) {
            if (slot == SLOT_REPAIR) repair(player)
            return
        }
        when (slot) {
            SLOT_BOOKS -> openBooks(player)
            SLOT_TOOLS -> openTools(player)
            SLOT_PROGRESS -> plugin.progressionManager.open(player)
            SLOT_EXPEDITION -> openExpedition(player)
            SLOT_STATUS -> showStatus(player)
        }
    }

    private fun handleBooks(player: Player, slot: Int) {
        when (slot) {
            SLOT_MANUAL -> openBookNextTick(player) { plugin.tuningManual.open(player) }
            SLOT_COMPENDIUM -> openBookNextTick(player) { plugin.studyBooks.openCompendium(player) }
            SLOT_RECORD -> openBookNextTick(player) { plugin.studyBooks.openDiscoveryRecord(player) }
            Menu.BACK_SLOT -> openMain(player)
        }
    }

    private fun handleTools(player: Player, slot: Int) {
        when (slot) {
            SLOT_WIND_VANE -> craftTool(player, plugin.windVane.cost, WindVane.ID) { plugin.windVane.createItem() }
            SLOT_MOON_POUCH -> craftTool(player, plugin.moonPouch.cost, MoonPouch.ID) { plugin.moonPouch.createItem() }
            SLOT_STAR_COMPASS -> if (level(player) >= 2) {
                craftTool(player, plugin.starCompass.craftCost, com.tinyyana.lycohism.tool.StarCompass.ID) { plugin.starCompass.createItem() }
            }
            Menu.BACK_SLOT -> openMain(player)
        }
    }

    private fun handleExpedition(player: Player, slot: Int) {
        if (slot == Menu.BACK_SLOT) {
            openMain(player)
            return
        }
        // Inside an expedition world the single OAK_DOOR (SLOT_GATE) returns home.
        if (plugin.expeditionManager.expeditionAt(player.world) != null) {
            if (slot == SLOT_GATE) {
                plugin.expeditionManager.leave(player)
                player.closeInventory()
            }
            return
        }
        val index = EXPEDITION_SLOTS.indexOf(slot)
        val expedition = plugin.expeditionManager.all().getOrNull(index) ?: return
        if (plugin.expeditionManager.useGateFor(player, expedition)) {
            player.closeInventory()
        }
    }

    /** InventoryClickEvent should finish before a written-book view is opened. */
    private fun openBookNextTick(player: Player, action: () -> Unit) {
        player.closeInventory()
        plugin.server.scheduler.runTask(plugin, Runnable { action() })
    }

    private fun repair(player: Player) {
        val requirements = Cost.parse(repairCost, plugin)
        if (!Cost.hasAll(player, requirements)) {
            sendMissing(player, requirements)
            return
        }

        Cost.consume(player, requirements)
        plugin.playerDataManager.update(player.uniqueId) { data ->
            data.studyLevel = 1
            data.discoveries.add(DISCOVERY_ID)
        }
        Messages.send(player, Texts.line("messages.facility.study-repaired"))
        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.2f)
        open(player)
    }

    private inline fun craftTool(
        player: Player,
        cost: List<String>,
        discoveryId: String,
        factory: () -> ItemStack,
    ) {
        val requirements = Cost.parse(cost, plugin)
        if (!recipeUnlocked(player, requirements)) {
            Messages.send(player, Texts.line("messages.common.recipe-locked"))
            return
        }
        if (!Cost.hasAll(player, requirements)) {
            sendMissing(player, requirements)
            return
        }
        Cost.consume(player, requirements)
        Items.give(player, factory())
        com.tinyyana.lycohism.util.Audit.log(player, "craft", "$discoveryId (study)")
        plugin.playerDataManager.discover(player.uniqueId, discoveryId)
        Messages.send(player, Texts.render("messages.facility.study-crafted", "item" to Texts.line("content-names.$discoveryId")))
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f)
    }

    private fun putCraftButton(inv: Inventory, slot: Int, player: Player, item: ItemStack, cost: List<String>) {
        val requirements = Cost.parse(cost, plugin)
        val data = plugin.playerDataManager.rememberInventoryMaterials(player)
        inv.setItem(slot, FacilityUi.withCost(item, requirements, "gui.common.click-craft", data))
    }

    private fun recipeUnlocked(player: Player, requirements: List<Cost.Requirement>): Boolean =
        Cost.isRecipeUnlocked(plugin.playerDataManager.rememberInventoryMaterials(player), requirements)

    private fun showStatus(player: Player) {
        Texts.renderLines("messages.facility.study-status", "level" to level(player).toString())
            .forEach { Messages.send(player, it) }
    }

    private fun sendMissing(player: Player, requirements: List<Cost.Requirement>) {
        Messages.send(player, Texts.render("messages.common.missing-materials", "costs" to FacilityUi.describe(requirements)))
    }

    private fun level(player: Player): Int = plugin.playerDataManager.get(player.uniqueId).studyLevel

    private fun create(menu: StudyMenu, title: String, size: Int = Menu.COMPACT_SIZE): Inventory =
        Menu.create(StudyHolder(menu), title, size)

    companion object {
        private const val FILE_NAME = "facilities.yml"

        // MAIN
        private const val SLOT_REPAIR = 13
        private const val SLOT_BOOKS = 10
        private const val SLOT_TOOLS = 11
        private const val SLOT_PROGRESS = 13
        private const val SLOT_EXPEDITION = 15
        private const val SLOT_STATUS = 16

        // 書冊 leaf
        private const val SLOT_MANUAL = 11
        private const val SLOT_COMPENDIUM = 13
        private const val SLOT_RECORD = 15

        // 器物 leaf
        private const val SLOT_WIND_VANE = 12
        private const val SLOT_MOON_POUCH = 14
        private const val SLOT_STAR_COMPASS = 16

        // 遠征 leaf
        private const val SLOT_GATE = 13
        private val EXPEDITION_SLOTS = listOf(11, 13, 15)

        private const val DISCOVERY_ID = "study"
    }
}
