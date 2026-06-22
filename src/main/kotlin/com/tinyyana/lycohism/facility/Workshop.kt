package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.gui.WorkshopHolder
import com.tinyyana.lycohism.gui.WorkshopMenu
import com.tinyyana.lycohism.tool.EnergyCrystal
import com.tinyyana.lycohism.tool.FlowerBookmark
import com.tinyyana.lycohism.tool.LeylineProbe
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

    /** One material conversion: [amount] of [output] per click; [lv2] entries need the upgraded workshop. */
    private data class Conversion(val input: Material, val output: Material, val amount: Int, val lv2: Boolean)

    private var repairCost: List<String> = listOf("morning_dew:4", "OAK_PLANKS:8", "COBBLESTONE:8")
    private var conversions: List<Conversion> = defaultConversions()
    private var conversionBonus = 2
    private var toolMendingCost: List<String> = listOf("rain_breath:8", "moon_dew:4", "BOOK:1")
    private var toolMendingLevelCost = 15

    init {
        load()
    }

    fun load() {
        val node = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("workshop") ?: return

        repairCost = node.getStringList("repair-cost").ifEmpty { repairCost }
        val base = node.getStringList("conversions").mapNotNull { parseConversion(it, lv2 = false) }
        val lv2 = node.getStringList("conversions-2").mapNotNull { parseConversion(it, lv2 = true) }
        conversions = (base.ifEmpty { defaultConversions() } + lv2)
        conversionBonus = node.getInt("conversion-bonus", conversionBonus).coerceAtLeast(1)
        toolMendingCost = node.getStringList("tool-mending-cost").ifEmpty { toolMendingCost }
        toolMendingLevelCost = node.getInt("tool-mending-level-cost", toolMendingLevelCost).coerceAtLeast(1)
    }

    /** Parses "INPUT>OUTPUT" or "INPUT>OUTPUT:n" into a [Conversion]. */
    private fun parseConversion(token: String, lv2: Boolean): Conversion? {
        val parts = token.split(">")
        val input = parts.getOrNull(0)?.let { Material.matchMaterial(it.trim()) } ?: return null
        val outParts = parts.getOrNull(1)?.trim()?.split(":") ?: return null
        val output = Material.matchMaterial(outParts[0].trim()) ?: return null
        val amount = outParts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        return Conversion(input, output, amount, lv2)
    }

    private fun defaultConversions(): List<Conversion> = listOf(
        Conversion(Material.COBBLESTONE, Material.STONE, 1, false),
        Conversion(Material.STONE, Material.STONE_BRICKS, 1, false),
        Conversion(Material.SAND, Material.SANDSTONE, 1, false),
    )

    /** Content level actually exposed: Lv2 perks need the upgraded structure (or command). */
    private fun effectiveLevel(player: Player, upgraded: Boolean): Int =
        if (upgraded) level(player) else level(player).coerceAtMost(1)

    fun open(player: Player, upgraded: Boolean = true) = openMain(player, upgraded)

    // ---- Menus -------------------------------------------------------------

    private fun openMain(player: Player, upgraded: Boolean) {
        val stored = level(player)
        val broken = stored < 1
        val title = Texts.line(if (broken) "gui.workshop.broken" else "gui.workshop.root")
        val inv = create(WorkshopMenu.MAIN, title, upgraded)

        val headerLore = if (broken) {
            Texts.lines("gui.workshop.header-broken-lore")
        } else {
            Texts.lines("gui.workshop.header-main-lore")
        }
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.workshop.header-main"), *headerLore.toTypedArray()))

        if (stored < 1) {
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
            inv.setItem(SLOT_STATUS, button(Material.LECTERN, Texts.line("gui.workshop.status"), Texts.renderLines("gui.workshop.status-lore", "level" to stored.toString())))
            if (stored in 1 until FacilityUpgrade.MAX_LEVEL) inv.setItem(SLOT_UPGRADE, FacilityUi.upgradeButton(plugin, "workshop", stored))
            else inv.setItem(SLOT_UPGRADE, FacilityUi.maxedButton())
        }
        player.openInventory(inv)
    }

    /** Highest tool slot shown at [effective] level; the menu sizes itself to fit it plus a back row. */
    private fun toolsMaxSlot(effective: Int): Int =
        if (effective >= 2) maxOf(SLOT_TOOL_REPAIR, SLOT_TOOL_HAMMER_TIER_2, SLOT_TOOL_SOLAR, SLOT_TOOL_ENGINE_BP) else SLOT_TOOL_REPAIR

    private fun openTools(player: Player, upgraded: Boolean) {
        val effective = effectiveLevel(player, upgraded)
        val inv = create(
            WorkshopMenu.TOOLS,
            Menu.title(Texts.line("gui.workshop.root"), Texts.line("gui.workshop.tools-leaf")),
            upgraded,
            Menu.sizeFor(toolsMaxSlot(effective)),
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
        // 輝能之鏡改在輝能祭壇煉製（見 altars.yml），工房不再提供。
        if (effective >= 2) {
            putCraftButton(inv, SLOT_TOOL_HAMMER_TIER_2, player, plugin.stoneworkHammer.createItem(reinforced = true), plugin.stoneworkHammer.reinforcedCost)
            putCraftButton(inv, SLOT_TOOL_SOLAR, player, plugin.solarPick.createItem(), plugin.solarPick.craftCost)
            putCraftButton(inv, SLOT_TOOL_ENGINE_BP, player, plugin.blueprint.createItem(ENGINE_STRUCTURE_ID), ENGINE_BLUEPRINT_COST)
        }
        val mendingRequirements = Cost.parse(toolMendingCost, plugin)
        // Lv2：修補同時附耐久 III。Lv3（蝕輝）：再附一個對應的戰鬥附魔（武器鋒利／護甲保護／工具效率），均衡口味的「戰鬥向」強化。
        val mendingLorePath = when {
            effective >= 3 -> "gui.workshop.mending-lore-3"
            effective >= 2 -> "gui.workshop.mending-lore-2"
            else -> "gui.workshop.mending-lore"
        }
        inv.setItem(
            SLOT_TOOL_REPAIR, FacilityUi.withCost(
                button(Material.ENCHANTED_BOOK, Texts.line("gui.workshop.mending"), Texts.renderLines(mendingLorePath, "levels" to toolMendingLevelCost.toString())),
                mendingRequirements,
                "gui.common.click-enchant",
                plugin.playerDataManager.rememberInventoryMaterials(player),
            ),
        )
        inv.setItem(Menu.backSlotAfter(listOf(toolsMaxSlot(effective))), backButton())
        player.openInventory(inv)
    }

    /** Material slots actually filled at [effective] level, used for adaptive sizing + back placement. */
    private fun materialSlots(effective: Int): List<Int> {
        val shownCount = (if (effective >= 2) conversions else conversions.filterNot { it.lv2 }).size
        return MATERIAL_SLOTS.take(shownCount.coerceIn(1, MATERIAL_SLOTS.size))
    }

    private fun openMaterials(player: Player, upgraded: Boolean) {
        val effective = effectiveLevel(player, upgraded)
        val shown = if (effective >= 2) conversions else conversions.filterNot { it.lv2 }
        val inv = create(
            WorkshopMenu.MATERIALS,
            Menu.title(Texts.line("gui.workshop.root"), Texts.line("gui.workshop.materials-leaf")),
            upgraded,
            Menu.sizeFor(materialSlots(effective).maxOrNull() ?: Menu.HEADER_SLOT),
        )
        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(Texts.line("gui.workshop.materials-leaf"), *Texts.lines(if (effective >= 2) "gui.workshop.header-materials-lore-2" else "gui.workshop.header-materials-lore").toTypedArray()),
        )
        shown.forEachIndexed { index, conv ->
            if (index >= MATERIAL_SLOTS.size) return@forEachIndexed
            val yield = conv.amount * (if (effective >= 2) conversionBonus else 1)
            inv.setItem(
                MATERIAL_SLOTS[index],
                button(conv.output, Texts.render("gui.workshop.output-name", "item" to prettyName(conv.output)), listOf(
                    Texts.render("gui.workshop.conversion-n", "input" to prettyName(conv.input), "output" to prettyName(conv.output), "amount" to yield.toString()),
                    Texts.line("gui.workshop.conversion-action"),
                )),
            )
        }
        inv.setItem(Menu.backSlotAfter(materialSlots(effective)), backButton())
        player.openInventory(inv)
    }

    // ---- Click handling ----------------------------------------------------

    fun handleClick(player: Player, holder: WorkshopHolder, rawSlot: Int) {
        when (holder.menu) {
            WorkshopMenu.MAIN -> handleMain(player, rawSlot, holder.upgraded)
            WorkshopMenu.TOOLS -> handleTools(player, rawSlot, holder.upgraded)
            WorkshopMenu.MATERIALS -> handleMaterials(player, rawSlot, holder.upgraded)
        }
    }

    private fun handleMain(player: Player, rawSlot: Int, upgraded: Boolean) {
        if (level(player) < 1) {
            if (rawSlot == SLOT_REPAIR) repair(player, upgraded)
            return
        }
        when (rawSlot) {
            SLOT_TOOLS -> openTools(player, upgraded)
            SLOT_MATERIALS -> openMaterials(player, upgraded)
            SLOT_STATUS -> showStatus(player)
            SLOT_UPGRADE -> if (level(player) in 1 until FacilityUpgrade.MAX_LEVEL) doUpgrade(player, upgraded)
        }
    }

    private fun handleTools(player: Player, rawSlot: Int, upgraded: Boolean) {
        val effective = effectiveLevel(player, upgraded)
        if (rawSlot == Menu.backSlotAfter(listOf(toolsMaxSlot(effective)))) {
            openMain(player, upgraded)
            return
        }
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
            SLOT_TOOL_SOLAR -> if (effective >= 2) {
                craftTool(player, plugin.solarPick.craftCost, com.tinyyana.lycohism.tool.SolarPick.ID) { plugin.solarPick.createItem() }
            }
            SLOT_TOOL_HAMMER_TIER_2 -> if (effective >= 2) {
                craftTool(player, plugin.stoneworkHammer.reinforcedCost, StoneworkHammer.REINFORCED_ID) {
                    plugin.stoneworkHammer.createItem(reinforced = true)
                }
            }
            SLOT_TOOL_ENGINE_BP -> if (effective >= 2) giveEngineBlueprint(player)
            SLOT_TOOL_REPAIR -> repairHeldTool(player, effective)
        }
    }

    private fun handleMaterials(player: Player, rawSlot: Int, upgraded: Boolean) {
        val effective = effectiveLevel(player, upgraded)
        if (rawSlot == Menu.backSlotAfter(materialSlots(effective))) {
            openMain(player, upgraded)
            return
        }
        val shown = if (effective >= 2) conversions else conversions.filterNot { it.lv2 }
        val index = MATERIAL_SLOTS.indexOf(rawSlot)
        if (index in shown.indices) convert(player, shown[index], if (effective >= 2) conversionBonus else 1)
    }

    // ---- Actions -----------------------------------------------------------

    private fun repair(player: Player, upgraded: Boolean) {
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
        openMain(player, upgraded)
    }

    /** Lv1→Lv2: upgrade at the structure, else hand out its blueprint to build (FacilityUi). */
    private fun doUpgrade(player: Player, upgraded: Boolean) {
        FacilityUi.upgradeClick(plugin, player, "workshop") { openMain(player, upgraded) }
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

    /** Workshop Lv2: hands out the 自動調律機 blueprint so automation has a normal obtain path (v0.9.2 #1). */
    private fun giveEngineBlueprint(player: Player) {
        if (level(player) < 1) {
            Messages.send(player, Texts.line("messages.facility.workshop-required"))
            return
        }
        val reqs = Cost.parse(ENGINE_BLUEPRINT_COST, plugin)
        if (!recipeUnlocked(player, reqs)) {
            Messages.send(player, Texts.line("messages.common.recipe-locked"))
            return
        }
        if (!Cost.hasAll(player, reqs)) {
            sendMissing(player, reqs)
            return
        }
        Cost.consume(player, reqs)
        Items.give(player, plugin.blueprint.createItem(ENGINE_STRUCTURE_ID))
        com.tinyyana.lycohism.util.Audit.log(player, "blueprint-craft", "$ENGINE_STRUCTURE_ID (workshop)")
        Messages.send(player, Texts.render("messages.facility.blueprint-crafted", "item" to Texts.line("content-names.$ENGINE_STRUCTURE_ID")))
        player.playSound(player.location, org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f)
    }

    private fun convert(player: Player, conversion: Conversion, multiplier: Int) {
        val removed = removePlain(player, conversion.input, MAX_CONVERT)
        if (removed <= 0) {
            Messages.send(player, Texts.render("messages.facility.conversion-empty", "item" to prettyName(conversion.input)))
            return
        }
        val produced = removed * conversion.amount * multiplier
        Items.give(player, ItemStack(conversion.output, produced))
        Messages.send(player, Texts.render("messages.facility.conversion-done", "amount" to produced.toString(), "item" to prettyName(conversion.output)))
    }

    private fun repairHeldTool(player: Player, effective: Int) {
        val lv2 = effective >= 2
        val lv3 = effective >= 3
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
        // 升級工房：順手附加耐久 III（強化裝備，#4）。
        if (lv2 && Enchantment.UNBREAKING.canEnchantItem(item) && !meta.hasEnchant(Enchantment.UNBREAKING)) {
            meta.addEnchant(Enchantment.UNBREAKING, 3, true)
        }
        // 三階（蝕輝）：再附一個對應裝備類型的戰鬥附魔，均衡口味的「戰鬥向」三階強化。
        if (lv3) combatEnchant(item)?.let { (enchant, level) ->
            if (enchant.canEnchantItem(item) && !meta.hasEnchant(enchant)) meta.addEnchant(enchant, level, true)
        }
        item.itemMeta = meta
        player.inventory.setItemInMainHand(item)
        plugin.playerDataManager.discover(player.uniqueId, DISCOVERY_TOOL_REPAIR)
        val msg = when { lv3 -> "messages.facility.mending-applied-3"; lv2 -> "messages.facility.mending-applied-2"; else -> "messages.facility.mending-applied" }
        Messages.send(player, Texts.line(msg))
        player.playSound(player.location, org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f)
    }

    /** The type-appropriate combat/utility enchant the Lv3 mending bench adds, or null if none fits. */
    private fun combatEnchant(item: ItemStack): Pair<Enchantment, Int>? = when {
        Enchantment.SHARPNESS.canEnchantItem(item) -> Enchantment.SHARPNESS to 3
        Enchantment.POWER.canEnchantItem(item) -> Enchantment.POWER to 3
        Enchantment.PROTECTION.canEnchantItem(item) -> Enchantment.PROTECTION to 4
        Enchantment.EFFICIENCY.canEnchantItem(item) -> Enchantment.EFFICIENCY to 4
        else -> null
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

    private fun create(menu: WorkshopMenu, title: String, upgraded: Boolean, size: Int = Menu.COMPACT_SIZE): Inventory =
        Menu.create(WorkshopHolder(menu, upgraded), title, size)

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

        // Main menu: evenly spaced (10/12/14/16) so it reads cleanly with or without the upgrade button.
        private const val SLOT_REPAIR = 13
        private const val SLOT_TOOLS = 10
        private const val SLOT_MATERIALS = 12
        private const val SLOT_STATUS = 14
        private const val SLOT_UPGRADE = 16

        private const val SLOT_TOOL_BOOKMARK = 11
        private const val SLOT_TOOL_HAMMER = 13
        private const val SLOT_TOOL_DEWLIGHT = 15
        private const val SLOT_TOOL_BANDAGE = 10
        private const val SLOT_TOOL_LEYLINE_PROBE = 14
        private const val SLOT_TOOL_REPAIR = 16
        private const val SLOT_TOOL_CRYSTAL = 12
        private const val SLOT_TOOL_SOLAR = 20
        private const val SLOT_TOOL_HAMMER_TIER_2 = 19
        // v0.9.2 #1：工房 Lv2 製作自動調律機藍圖，給自動化設備一個正常取得途徑（先前只能手蓋／admin）。
        private const val SLOT_TOOL_ENGINE_BP = 21
        private val ENGINE_BLUEPRINT_COST = listOf("radiant_ore:2", "REDSTONE:4", "PAPER:1")
        private const val ENGINE_STRUCTURE_ID = "attunement_engine"

        private val MATERIAL_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16)
        private const val DISCOVERY_TOOL_REPAIR = "tool_repair"
    }
}
