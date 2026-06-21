package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.gui.WorkshopHolder
import com.tinyyana.lycohism.gui.WorkshopMenu
import com.tinyyana.lycohism.tool.EnergyCrystal
import com.tinyyana.lycohism.tool.FlowerBookmark
import com.tinyyana.lycohism.tool.LeylineProbe
import com.tinyyana.lycohism.tool.RadiantFocus
import com.tinyyana.lycohism.tool.RainBandage
import com.tinyyana.lycohism.tool.StoneworkHammer
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.VanillaItems
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * 工房 — the first base facility. Repaired by submitting materials; once at level 1 its
 * GUI offers tool crafting (花籤、石工槌), basic material processing, and a status view.
 * Per-player level lives in PlayerData. Data-driven via facilities.yml.
 */
class Workshop(private val plugin: Lycohism) {

    private var repairCost: List<String> = listOf("morning_dew:4", "OAK_PLANKS:8", "COBBLESTONE:8")
    private var conversions: List<Pair<Material, Material>> = listOf(
        Material.COBBLESTONE to Material.STONE,
        Material.STONE to Material.STONE_BRICKS,
        Material.SAND to Material.SANDSTONE,
    )
    private var toolMendingCost: List<String> = listOf("rain_breath:8", "moon_dew:4", "BOOK:1")
    private var toolMendingLevelCost = 15

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("workshop") ?: return

        repairCost = node.getStringList("repair-cost").ifEmpty { repairCost }
        val parsed = node.getStringList("conversions").mapNotNull { token ->
            val parts = token.split(">")
            val input = parts.getOrNull(0)?.let { Material.matchMaterial(it.trim()) }
            val output = parts.getOrNull(1)?.let { Material.matchMaterial(it.trim()) }
            if (input != null && output != null) input to output else null
        }
        if (parsed.isNotEmpty()) conversions = parsed
        toolMendingCost = node.getStringList("tool-mending-cost").ifEmpty { toolMendingCost }
        toolMendingLevelCost = node.getInt("tool-mending-level-cost", toolMendingLevelCost).coerceAtLeast(1)
    }

    fun open(player: Player) = openMain(player)

    // ---- Menus -------------------------------------------------------------

    private fun openMain(player: Player) {
        val level = level(player)
        val broken = level < 1
        val title = Texts.line(if (broken) "gui.workshop.broken" else "gui.workshop.root")
        val inv = create(WorkshopMenu.MAIN, title)

        val headerLore = if (broken) {
            Texts.lines("gui.workshop.header-broken-lore")
        } else {
            Texts.lines("gui.workshop.header-main-lore")
        }
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.workshop.header-main"), *headerLore.toTypedArray()))

        if (level < 1) {
            inv.setItem(
                SLOT_REPAIR,
                button(Material.CRACKED_STONE_BRICKS, Texts.line("gui.workshop.repair"), buildList {
                    addAll(Texts.lines("gui.workshop.repair-lore"))
                    add("")
                    add(Texts.line("gui.common.requires"))
                    addAll(FacilityUi.costLines(Cost.parse(repairCost, plugin)))
                    add("")
                    add(Texts.line("gui.common.click-repair"))
                }),
            )
        } else {
            inv.setItem(SLOT_TOOLS, button(Material.GLASS_BOTTLE, Texts.line("gui.workshop.tools-leaf"), Texts.lines("gui.workshop.tools-lore")))
            inv.setItem(SLOT_MATERIALS, button(Material.STONECUTTER, Texts.line("gui.workshop.materials-leaf"), Texts.lines("gui.workshop.materials-lore")))
            inv.setItem(SLOT_STATUS, button(Material.LECTERN, Texts.line("gui.workshop.status"), Texts.renderLines("gui.workshop.status-lore", "level" to level.toString())))
        }
        player.openInventory(inv)
    }

    private fun openTools(player: Player) {
        val inv = create(
            WorkshopMenu.TOOLS,
            Menu.title(Texts.line("gui.workshop.root"), Texts.line("gui.workshop.tools-leaf")),
        )
        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(Texts.line("gui.workshop.tools-leaf"), *Texts.lines("gui.workshop.header-tools-lore").toTypedArray()),
        )
        putCraftButton(inv, SLOT_TOOL_BOOKMARK, player, plugin.flowerBookmark.createItem(), plugin.flowerBookmark.cost)
        putCraftButton(inv, SLOT_TOOL_HAMMER, player, plugin.stoneworkHammer.createItem(), plugin.stoneworkHammer.cost)
        putCraftButton(inv, SLOT_TOOL_BANDAGE, player, plugin.rainBandage.createItem(), plugin.rainBandage.cost)
        putCraftButton(inv, SLOT_TOOL_DEWLIGHT, player, plugin.dewLight.createItem(), plugin.dewLight.cost)
        putCraftButton(inv, SLOT_TOOL_LEYLINE_PROBE, player, plugin.leylineProbe.createItem(), plugin.leylineProbe.cost)
        putCraftButton(inv, SLOT_TOOL_CRYSTAL, player, plugin.energyCrystal.createItem(), plugin.energyCrystal.cost)
        putCraftButton(inv, SLOT_TOOL_FOCUS, player, plugin.radiantFocus.createItem(), plugin.radiantFocus.cost)
        if (level(player) >= 2) {
            putCraftButton(inv, SLOT_TOOL_SOLAR, player, plugin.solarPick.createItem(), plugin.solarPick.craftCost)
        }
        val mendingRequirements = Cost.parse(toolMendingCost, plugin)
        inv.setItem(
            SLOT_TOOL_REPAIR, FacilityUi.withCost(
                button(Material.ENCHANTED_BOOK, Texts.line("gui.workshop.mending"), Texts.renderLines("gui.workshop.mending-lore", "levels" to toolMendingLevelCost.toString())),
                mendingRequirements,
                "gui.common.click-enchant",
                plugin.playerDataManager.rememberInventoryMaterials(player),
            ),
        )
        inv.setItem(SLOT_BACK, backButton())
        player.openInventory(inv)
    }

    private fun openMaterials(player: Player) {
        val inv = create(
            WorkshopMenu.MATERIALS,
            Menu.title(Texts.line("gui.workshop.root"), Texts.line("gui.workshop.materials-leaf")),
        )
        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(Texts.line("gui.workshop.materials-leaf"), *Texts.lines("gui.workshop.header-materials-lore").toTypedArray()),
        )
        conversions.forEachIndexed { index, (input, output) ->
            if (index >= MATERIAL_SLOTS.size) return@forEachIndexed
            inv.setItem(
                MATERIAL_SLOTS[index],
                button(output, Texts.render("gui.workshop.output-name", "item" to prettyName(output)), listOf(
                    Texts.render("gui.workshop.conversion", "input" to prettyName(input), "output" to prettyName(output)),
                    Texts.line("gui.workshop.conversion-action"),
                )),
            )
        }
        inv.setItem(SLOT_BACK, backButton())
        player.openInventory(inv)
    }

    // ---- Click handling ----------------------------------------------------

    fun handleClick(player: Player, holder: WorkshopHolder, rawSlot: Int) {
        when (holder.menu) {
            WorkshopMenu.MAIN -> handleMain(player, rawSlot)
            WorkshopMenu.TOOLS -> handleTools(player, rawSlot)
            WorkshopMenu.MATERIALS -> handleMaterials(player, rawSlot)
        }
    }

    private fun handleMain(player: Player, rawSlot: Int) {
        if (level(player) < 1) {
            if (rawSlot == SLOT_REPAIR) repair(player)
            return
        }
        when (rawSlot) {
            SLOT_TOOLS -> openTools(player)
            SLOT_MATERIALS -> openMaterials(player)
            SLOT_STATUS -> showStatus(player)
        }
    }

    private fun handleTools(player: Player, rawSlot: Int) {
        when (rawSlot) {
            SLOT_TOOL_BOOKMARK -> craftTool(player, plugin.flowerBookmark.cost, FlowerBookmark.ID) {
                plugin.flowerBookmark.createItem()
            }
            SLOT_TOOL_HAMMER -> craftTool(player, plugin.stoneworkHammer.cost, StoneworkHammer.ID) {
                plugin.stoneworkHammer.createItem()
            }
            SLOT_TOOL_BANDAGE -> craftTool(player, plugin.rainBandage.cost, RainBandage.ID) {
                plugin.rainBandage.createItem()
            }
            SLOT_TOOL_DEWLIGHT -> craftTool(player, plugin.dewLight.cost, com.tinyyana.lycohism.tool.DewLight.ID) {
                plugin.dewLight.createItem()
            }
            SLOT_TOOL_LEYLINE_PROBE -> craftTool(player, plugin.leylineProbe.cost, LeylineProbe.ID) {
                plugin.leylineProbe.createItem()
            }
            SLOT_TOOL_CRYSTAL -> craftTool(player, plugin.energyCrystal.cost, EnergyCrystal.ID) {
                plugin.energyCrystal.createItem()
            }
            SLOT_TOOL_FOCUS -> craftTool(player, plugin.radiantFocus.cost, RadiantFocus.ID) {
                plugin.radiantFocus.createItem()
            }
            SLOT_TOOL_SOLAR -> if (level(player) >= 2) {
                craftTool(player, plugin.solarPick.craftCost, com.tinyyana.lycohism.tool.SolarPick.ID) { plugin.solarPick.createItem() }
            }
            SLOT_TOOL_REPAIR -> repairHeldTool(player)
            SLOT_BACK -> openMain(player)
        }
    }

    private fun handleMaterials(player: Player, rawSlot: Int) {
        if (rawSlot == SLOT_BACK) {
            openMain(player)
            return
        }
        val index = MATERIAL_SLOTS.indexOf(rawSlot)
        if (index in conversions.indices) convert(player, conversions[index])
    }

    // ---- Actions -----------------------------------------------------------

    private fun repair(player: Player) {
        val reqs = Cost.parse(repairCost, plugin)
        if (!Cost.hasAll(player, reqs)) {
            sendMissing(player, reqs)
            return
        }
        Cost.consume(player, reqs)
        plugin.playerDataManager.update(player.uniqueId) { data ->
            data.workshopLevel = 1
            data.discoveries.add("workshop")
        }
        Messages.send(player, Texts.line("messages.facility.workshop-repaired"))
        player.playSound(player.location, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.5f)
        openMain(player)
    }

    private inline fun craftTool(
        player: Player,
        cost: List<String>,
        discoveryId: String,
        factory: () -> ItemStack,
    ) {
        if (level(player) < 1) {
            Messages.send(player, Texts.line("messages.facility.workshop-required"))
            return
        }
        val reqs = Cost.parse(cost, plugin)
        if (!recipeUnlocked(player, reqs)) {
            Messages.send(player, Texts.line("messages.common.recipe-locked"))
            return
        }
        if (!Cost.hasAll(player, reqs)) {
            sendMissing(player, reqs)
            return
        }
        Cost.consume(player, reqs)
        Items.give(player, factory())
        com.tinyyana.lycohism.util.Audit.log(player, "craft", "$discoveryId (workshop)")
        plugin.playerDataManager.discover(player.uniqueId, discoveryId)
        Messages.send(player, Texts.render("messages.facility.workshop-crafted", "item" to Texts.line("content-names.$discoveryId")))
        player.playSound(player.location, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f)
    }

    private fun convert(player: Player, conversion: Pair<Material, Material>) {
        val (input, output) = conversion
        val removed = removePlain(player, input, MAX_CONVERT)
        if (removed <= 0) {
            Messages.send(player, Texts.render("messages.facility.conversion-empty", "item" to prettyName(input)))
            return
        }
        Items.give(player, ItemStack(output, removed))
        Messages.send(player, Texts.render("messages.facility.conversion-done", "amount" to removed.toString(), "item" to prettyName(output)))
    }

    private fun repairHeldTool(player: Player) {
        val reqs = Cost.parse(toolMendingCost, plugin)
        if (!recipeUnlocked(player, reqs)) {
            Messages.send(player, Texts.line("messages.common.recipe-locked"))
            return
        }
        val item = player.inventory.itemInMainHand
        val meta = item.itemMeta
        if (item.type == Material.AIR || !Enchantment.MENDING.canEnchantItem(item)) {
            Messages.send(player, Texts.line("messages.facility.mending-invalid"))
            return
        }
        if (meta.hasEnchant(Enchantment.MENDING)) {
            Messages.send(player, Texts.line("messages.facility.mending-present"))
            return
        }
        if (player.level < toolMendingLevelCost) {
            Messages.send(player, Texts.render("messages.facility.mending-levels-missing", "levels" to toolMendingLevelCost.toString()))
            return
        }
        if (!Cost.hasAll(player, reqs)) {
            sendMissing(player, reqs)
            return
        }
        Cost.consume(player, reqs)
        player.level -= toolMendingLevelCost
        meta.addEnchant(Enchantment.MENDING, 1, false)
        item.itemMeta = meta
        player.inventory.setItemInMainHand(item)
        plugin.playerDataManager.discover(player.uniqueId, DISCOVERY_TOOL_REPAIR)
        Messages.send(player, Texts.line("messages.facility.mending-applied"))
        player.playSound(player.location, org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f)
    }

    private fun showStatus(player: Player) {
        Texts.renderLines("messages.facility.workshop-status", "level" to level(player).toString())
            .forEach { Messages.send(player, it) }
    }

    // ---- Helpers -----------------------------------------------------------

    /** Removes up to [max] plain (non-Lycohism) items of [material]; returns the count removed. */
    private fun removePlain(player: Player, material: Material, max: Int): Int {
        var removed = 0
        val contents = player.inventory.contents
        for (i in contents.indices) {
            if (removed >= max) break
            val stack = contents[i] ?: continue
            if (stack.type != material || Items.idOf(stack) != null) continue

            val take = minOf(max - removed, stack.amount)
            stack.amount -= take
            removed += take
            if (stack.amount <= 0) player.inventory.setItem(i, null)
        }
        return removed
    }

    private fun level(player: Player): Int = plugin.playerDataManager.get(player.uniqueId).workshopLevel

    private fun create(menu: WorkshopMenu, title: String, size: Int = Menu.COMPACT_SIZE): Inventory =
        Menu.create(WorkshopHolder(menu), title, size)

    private fun button(material: Material, name: String, lore: List<String>): ItemStack =
        Menu.button(material, name, lore)

    private fun putCraftButton(inv: Inventory, slot: Int, player: Player, item: ItemStack, cost: List<String>) {
        val requirements = Cost.parse(cost, plugin)
        val data = plugin.playerDataManager.rememberInventoryMaterials(player)
        inv.setItem(slot, FacilityUi.withCost(item, requirements, "gui.common.click-craft", data))
    }

    private fun recipeUnlocked(player: Player, requirements: List<Cost.Requirement>): Boolean =
        Cost.isRecipeUnlocked(plugin.playerDataManager.rememberInventoryMaterials(player), requirements)

    private fun backButton(): ItemStack = Menu.back()

    private fun sendMissing(player: Player, reqs: List<Cost.Requirement>) {
        Messages.send(player, Texts.render("messages.common.missing-materials", "costs" to FacilityUi.describe(reqs)))
    }

    private fun prettyName(material: Material): String =
        VanillaItems.tag(material)

    companion object {
        private const val FILE_NAME = "facilities.yml"
        private const val MAX_CONVERT = 64

        private const val SLOT_REPAIR = 13
        private const val SLOT_TOOLS = 11
        private const val SLOT_MATERIALS = 13
        private const val SLOT_STATUS = 15

        private const val SLOT_TOOL_BOOKMARK = 11
        private const val SLOT_TOOL_HAMMER = 13
        private const val SLOT_TOOL_DEWLIGHT = 15
        private const val SLOT_TOOL_BANDAGE = 10
        private const val SLOT_TOOL_LEYLINE_PROBE = 14
        private const val SLOT_TOOL_REPAIR = 16
        private const val SLOT_TOOL_CRYSTAL = 12
        private const val SLOT_TOOL_FOCUS = 19
        private const val SLOT_TOOL_SOLAR = 20

        private val SLOT_BACK = Menu.BACK_SLOT
        private val MATERIAL_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16)
        private const val DISCOVERY_TOOL_REPAIR = "tool_repair"
    }
}
