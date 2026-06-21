package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.GreenhouseHolder
import com.tinyyana.lycohism.gui.GreenhouseMenu
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.tool.FlowerVeinShears
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.VanillaItems
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * 溫室 — v0.3 plant facility. Reclaimed by submitting materials; once at level 1 its GUI
 * cultivates plant materials from 花脈 and crafts the 花脈剪. Mirrors the 工房 structure and
 * shares the common Menu chrome for a consistent feel. Data-driven via facilities.yml.
 */
class Greenhouse(private val plugin: Lycohism) {

    /** input requirement -> output stack, parsed from "inputId:n>OUTPUT:n". */
    private data class Cultivation(val input: Cost.Requirement, val output: Material, val outputAmount: Int)

    private var repairCost: List<String> = listOf("flower_vein:4", "GLASS:8", "MOSS_BLOCK:4")
    private var cultivations: List<Cultivation> = emptyList()

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("greenhouse")
        if (node == null) {
            cultivations = defaultCultivations()
            return
        }
        repairCost = node.getStringList("repair-cost").ifEmpty { repairCost }
        val parsed = node.getStringList("cultivations")
            .mapNotNull { parseCultivation(it) }
            .filterNot { it.output == Material.CAKE } // v0.3 placeholder, not a plant cultivation
        cultivations = (parsed + defaultCultivations()).distinctBy { it.output }
    }

    fun open(player: Player) = openMain(player)

    // ---- Menus -------------------------------------------------------------

    private fun openMain(player: Player) {
        val broken = level(player) < 1
        val title = Texts.line(if (broken) "gui.greenhouse.broken" else "gui.greenhouse.root")
        val inv = Menu.create(GreenhouseHolder(GreenhouseMenu.MAIN), title)

        val headerLore = if (broken) {
            Texts.lines("gui.greenhouse.header-broken-lore")
        } else {
            Texts.lines("gui.greenhouse.header-main-lore")
        }
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.greenhouse.header-main"), *headerLore.toTypedArray()))

        if (broken) {
            inv.setItem(
                SLOT_REPAIR,
                Menu.button(Material.FLOWERING_AZALEA, Texts.line("gui.greenhouse.repair"), buildList {
                    addAll(Texts.lines("gui.greenhouse.repair-lore"))
                    add("")
                    add(Texts.line("gui.common.requires"))
                    addAll(FacilityUi.costLines(Cost.parse(repairCost, plugin)))
                    add("")
                    add(Texts.line("gui.common.click-repair"))
                }),
            )
        } else {
            inv.setItem(SLOT_CULTIVATE, Menu.button(Material.BONE_MEAL, Texts.line("gui.greenhouse.cultivate-leaf"), Texts.lines("gui.greenhouse.cultivate-lore")))
            inv.setItem(SLOT_TOOLS, Menu.button(Material.SHEARS, Texts.line("gui.greenhouse.tools-leaf"), Texts.lines("gui.greenhouse.tools-lore")))
            inv.setItem(SLOT_STATUS, Menu.button(Material.OAK_SAPLING, Texts.line("gui.greenhouse.status"), Texts.renderLines("gui.greenhouse.status-lore", "level" to level(player).toString())))
        }
        player.openInventory(inv)
    }

    private fun openCultivate(player: Player) {
        val inv = Menu.create(
            GreenhouseHolder(GreenhouseMenu.CULTIVATE),
            Menu.title(Texts.line("gui.greenhouse.root"), Texts.line("gui.greenhouse.cultivate-leaf")),
            if (cultivations.size > 7) Menu.EXTENDED_SIZE else Menu.COMPACT_SIZE,
        )
        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(Texts.line("gui.greenhouse.cultivate-leaf"), *Texts.lines("gui.greenhouse.header-cultivate-lore").toTypedArray()),
        )
        cultivations.forEachIndexed { index, cultivation ->
            if (index >= CONTENT_SLOTS.size) return@forEachIndexed
            inv.setItem(
                CONTENT_SLOTS[index],
                Menu.button(cultivation.output, Texts.render("gui.greenhouse.output-name", "item" to prettyName(cultivation.output), "amount" to cultivation.outputAmount.toString()), listOf(
                    Texts.render("gui.common.cost-plain", "item" to cultivation.input.label, "amount" to cultivation.input.amount.toString()),
                    Texts.line("gui.common.click-cultivate"),
                )),
            )
        }
        inv.setItem(cultivateBackSlot(), Menu.back())
        player.openInventory(inv)
    }

    private fun openTools(player: Player) {
        val inv = Menu.create(
            GreenhouseHolder(GreenhouseMenu.TOOLS),
            Menu.title(Texts.line("gui.greenhouse.root"), Texts.line("gui.greenhouse.tools-leaf")),
        )
        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(Texts.line("gui.greenhouse.tools-leaf"), *Texts.lines("gui.greenhouse.header-tools-lore").toTypedArray()),
        )
        putCraftButton(inv, SLOT_TOOL_SHEARS, player, plugin.flowerVeinShears.createItem(), plugin.flowerVeinShears.cost)
        // 雨後森林專屬產出：只能用苔華製作，給遠征一個「非去不可」的回流理由。
        putCraftButton(inv, SLOT_TOOL_BALM, player, plugin.mossBalm.createItem(), plugin.mossBalm.cost)
        putCraftButton(inv, SLOT_TOOL_FERTILE, player, plugin.mossFertile.createItem(), plugin.mossFertile.cost)
        if (level(player) >= 2) {
            putCraftButton(inv, SLOT_TOOL_SPORE, player, plugin.lunarSpore.createItem(), plugin.lunarSpore.craftCost)
        }
        inv.setItem(Menu.BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    // ---- Click handling ----------------------------------------------------

    fun handleClick(player: Player, holder: GreenhouseHolder, rawSlot: Int) {
        when (holder.menu) {
            GreenhouseMenu.MAIN -> handleMain(player, rawSlot)
            GreenhouseMenu.CULTIVATE -> handleCultivate(player, rawSlot)
            GreenhouseMenu.TOOLS -> handleTools(player, rawSlot)
        }
    }

    private fun handleMain(player: Player, rawSlot: Int) {
        if (level(player) < 1) {
            if (rawSlot == SLOT_REPAIR) repair(player)
            return
        }
        when (rawSlot) {
            SLOT_CULTIVATE -> openCultivate(player)
            SLOT_TOOLS -> openTools(player)
            SLOT_STATUS -> showStatus(player)
        }
    }

    private fun handleCultivate(player: Player, rawSlot: Int) {
        if (rawSlot == cultivateBackSlot()) {
            openMain(player)
            return
        }
        val index = CONTENT_SLOTS.indexOf(rawSlot)
        if (index in cultivations.indices) cultivate(player, cultivations[index])
    }

    private fun handleTools(player: Player, rawSlot: Int) {
        when (rawSlot) {
            SLOT_TOOL_SHEARS -> craftShears(player)
            SLOT_TOOL_BALM -> craftTool(player, plugin.mossBalm.cost, com.tinyyana.lycohism.tool.MossBalm.ID) { plugin.mossBalm.createItem() }
            SLOT_TOOL_FERTILE -> craftTool(player, plugin.mossFertile.cost, com.tinyyana.lycohism.tool.MossFertile.ID) { plugin.mossFertile.createItem() }
            SLOT_TOOL_SPORE -> if (level(player) >= 2) {
                craftTool(player, plugin.lunarSpore.craftCost, com.tinyyana.lycohism.tool.LunarSpore.ID) { plugin.lunarSpore.createItem() }
            }
            Menu.BACK_SLOT -> openMain(player)
        }
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
            data.greenhouseLevel = 1
            data.discoveries.add("greenhouse")
        }
        Messages.send(player, Texts.line("messages.facility.greenhouse-repaired"))
        player.playSound(player.location, Sound.ITEM_BONE_MEAL_USE, 0.8f, 1.2f)
        openMain(player)
    }

    private fun cultivate(player: Player, cultivation: Cultivation) {
        val reqs = listOf(cultivation.input)
        if (!Cost.hasAll(player, reqs)) {
            sendMissing(player, reqs)
            return
        }
        Cost.consume(player, reqs)
        Items.give(player, ItemStack(cultivation.output, cultivation.outputAmount))
        Messages.send(player, Texts.render("messages.facility.cultivated", "amount" to cultivation.outputAmount.toString(), "item" to prettyName(cultivation.output)))
        player.playSound(player.location, Sound.ITEM_BONE_MEAL_USE, 0.7f, 1.3f)
    }

    private inline fun craftTool(
        player: Player,
        cost: List<String>,
        discoveryId: String,
        factory: () -> ItemStack,
    ) {
        if (level(player) < 1) {
            Messages.send(player, Texts.line("messages.facility.greenhouse-required"))
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
        plugin.playerDataManager.discover(player.uniqueId, discoveryId)
        Messages.send(player, Texts.render("messages.facility.greenhouse-crafted", "item" to Texts.line("content-names.$discoveryId")))
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f)
    }

    private fun craftShears(player: Player) {
        if (level(player) < 1) {
            Messages.send(player, Texts.line("messages.facility.greenhouse-required"))
            return
        }
        val reqs = Cost.parse(plugin.flowerVeinShears.cost, plugin)
        if (!recipeUnlocked(player, reqs)) {
            Messages.send(player, Texts.line("messages.common.recipe-locked"))
            return
        }
        if (!Cost.hasAll(player, reqs)) {
            sendMissing(player, reqs)
            return
        }
        Cost.consume(player, reqs)
        Items.give(player, plugin.flowerVeinShears.createItem())
        plugin.playerDataManager.discover(player.uniqueId, FlowerVeinShears.ID)
        Messages.send(player, Texts.render("messages.facility.greenhouse-crafted", "item" to Texts.line("content-names.${FlowerVeinShears.ID}")))
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f)
    }

    private fun showStatus(player: Player) {
        Texts.renderLines("messages.facility.greenhouse-status", "level" to level(player).toString())
            .forEach { Messages.send(player, it) }
    }

    // ---- Helpers -----------------------------------------------------------

    private fun putCraftButton(inv: Inventory, slot: Int, player: Player, item: ItemStack, cost: List<String>) {
        val requirements = Cost.parse(cost, plugin)
        val data = plugin.playerDataManager.rememberInventoryMaterials(player)
        inv.setItem(slot, FacilityUi.withCost(item, requirements, "gui.common.click-craft", data))
    }

    private fun recipeUnlocked(player: Player, requirements: List<Cost.Requirement>): Boolean =
        Cost.isRecipeUnlocked(plugin.playerDataManager.rememberInventoryMaterials(player), requirements)

    private fun cultivateBackSlot(): Int =
        Menu.backSlotAfter(CONTENT_SLOTS.take(cultivations.size.coerceIn(1, CONTENT_SLOTS.size)))

    private fun parseCultivation(token: String): Cultivation? {
        val parts = token.split(">")
        if (parts.size != 2) return null
        val input = Cost.parse(listOf(parts[0].trim()), plugin).firstOrNull() ?: return null
        val outParts = parts[1].trim().split(":")
        val output = Material.matchMaterial(outParts[0].trim()) ?: return null
        val amount = outParts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        return Cultivation(input, output, amount)
    }

    private fun defaultCultivations(): List<Cultivation> = listOfNotNull(
        parseCultivation("flower_vein:1>BONE_MEAL:4"),
        parseCultivation("flower_vein:1>WHEAT_SEEDS:4"),
        parseCultivation("flower_vein:2>SUGAR_CANE:3"),
        parseCultivation("flower_vein:2>MOSS_BLOCK:1"),
        parseCultivation("flower_vein:3>CHERRY_SAPLING:1"),
        parseCultivation("flower_vein:4>SPORE_BLOSSOM:1"),
        parseCultivation("flower_vein:6>TORCHFLOWER_SEEDS:1"),
    )

    private fun level(player: Player): Int = plugin.playerDataManager.get(player.uniqueId).greenhouseLevel

    private fun sendMissing(player: Player, reqs: List<Cost.Requirement>) {
        Messages.send(player, Texts.render("messages.common.missing-materials", "costs" to FacilityUi.describe(reqs)))
    }

    private fun prettyName(material: Material): String =
        VanillaItems.tag(material)

    companion object {
        private const val FILE_NAME = "facilities.yml"
        private const val SLOT_REPAIR = 13
        private const val SLOT_CULTIVATE = 11
        private const val SLOT_TOOLS = 13
        private const val SLOT_STATUS = 15
        private const val SLOT_TOOL_SHEARS = 11
        private const val SLOT_TOOL_BALM = 13
        private const val SLOT_TOOL_FERTILE = 15
        private const val SLOT_TOOL_SPORE = 10
        // Two inner rows so 花脈 and v0.5 苔華 cultivations both stay visible.
        private val CONTENT_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25)
    }
}
