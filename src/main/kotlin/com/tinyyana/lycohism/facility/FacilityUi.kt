package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.data.PlayerData
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** Shared facility lore formatting so cost/action blocks stay visually identical. */
object FacilityUi {

    /**
     * The Lv1→Lv2 upgrade button shown in a facility's main menu. Spells out the v0.7.4 #3 flow:
     * build the 升級 structure, fill a nexus via towers→relays, then upgrade here. Also names the
     * concrete costs (nexus 輝能 + materials) so players see exactly how the upgrade happens (#2).
     */
    fun upgradeButton(plugin: Lycohism, facility: String, currentLevel: Int): ItemStack {
        val target = currentLevel + 1
        val cfg = plugin.config.getConfigurationSection("facility-upgrade")
        val (sun, moon, requirements) = FacilityUpgrade.costFor(plugin, cfg, target)
        val lore = buildList {
            if (target >= 3) {
                addAll(Texts.renderLines("gui.facility.upgrade-lore-3", "sun" to sun.toString(), "moon" to moon.toString()))
            } else {
                val structureName = Texts.line("content-names.${FacilityUpgrade.structureId(facility)}")
                addAll(Texts.renderLines("gui.facility.upgrade-lore", "structure" to structureName, "sun" to sun.toString(), "moon" to moon.toString()))
            }
            if (requirements.isNotEmpty()) {
                add(Texts.line("gui.common.requires"))
                addAll(costLines(requirements))
            }
            add("")
            add(Texts.line("gui.facility.upgrade-action"))
        }
        val icon = if (target >= 3) Material.HEART_OF_THE_SEA else Material.AMETHYST_CLUSTER
        return ItemStack(icon).apply {
            editMeta { meta ->
                meta.displayName(Messages.parse(Texts.render("gui.facility.upgrade", "level" to target.toString())).decoration(TextDecoration.ITALIC, false))
                meta.lore(lore.map { line -> Messages.parse(line).decoration(TextDecoration.ITALIC, false) })
                meta.setEnchantmentGlintOverride(true)
            }
        }
    }

    /**
     * Shown in the upgrade slot once a facility is fully upgraded (v0.9.2 #2): a barrier with a short
     * "already at max" note, so the slot reads as intentional instead of an empty filler pane.
     */
    fun maxedButton(): ItemStack = ItemStack(Material.BARRIER).apply {
        editMeta { meta ->
            meta.displayName(Messages.parse(Texts.line("gui.facility.maxed")).decoration(TextDecoration.ITALIC, false))
            meta.lore(Texts.lines("gui.facility.maxed-lore").map { Messages.parse(it).decoration(TextDecoration.ITALIC, false) })
        }
    }

    /**
     * Upgrade-button click: if the player is at a complete upgrade structure, upgrade; otherwise hand
     * them the structure's 藍圖 (once) so they can ghost-preview and build it (v0.7.5 #2 — players, and
     * frankly the dev, couldn't tell how to build the upgrade structure).
     */
    fun upgradeClick(plugin: Lycohism, player: Player, facility: String, reopen: () -> Unit) {
        // Lv2→Lv3 needs no upgrade structure (it's gated on the 蝕輝結晶), so go straight to the upgrade.
        if (FacilityUpgrade.level(plugin, player, facility) >= 2) {
            runUpgrade(plugin, player, facility, reopen)
            return
        }
        if (FacilityUpgrade.structureComplete(plugin, player, facility)) {
            runUpgrade(plugin, player, facility, reopen)
            return
        }
        val structureId = FacilityUpgrade.structureId(facility)
        val hasBlueprint = player.inventory.contents.any { stack ->
            stack?.itemMeta?.persistentDataContainer?.get(Keys.blueprintTarget, PersistentDataType.STRING) == structureId
        }
        if (!hasBlueprint) Items.give(player, plugin.blueprint.createItem(structureId))
        Messages.send(player, Texts.line("messages.upgrade.blueprint-hint"))
        player.playSound(player.location, org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f)
        player.closeInventory()
    }

    /** Runs the upgrade and reports the result; [reopen] refreshes the menu on success. */
    fun runUpgrade(plugin: Lycohism, player: Player, facility: String, reopen: () -> Unit) {
        val result = FacilityUpgrade.upgrade(plugin, player, facility)
        val key = when (result) {
            FacilityUpgrade.Result.SUCCESS -> "messages.upgrade.done"
            FacilityUpgrade.Result.NOT_REPAIRED -> "messages.upgrade.not-repaired"
            FacilityUpgrade.Result.ALREADY_MAX -> "messages.upgrade.already"
            FacilityUpgrade.Result.NO_STRUCTURE -> "messages.upgrade.no-structure"
            FacilityUpgrade.Result.NO_NEXUS -> "messages.upgrade.no-nexus"
            FacilityUpgrade.Result.MISSING_MATERIALS -> "messages.upgrade.missing-materials"
            FacilityUpgrade.Result.MISSING_ENERGY -> "messages.upgrade.missing-energy"
            FacilityUpgrade.Result.UNKNOWN -> "messages.upgrade.usage"
        }
        Messages.send(player, Texts.render(key, "facility" to facility, "label" to "lyco"))
        if (result == FacilityUpgrade.Result.SUCCESS) {
            player.playSound(player.location, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f)
            reopen()
        }
    }

    fun withCost(
        item: ItemStack,
        requirements: List<Cost.Requirement>,
        actionPath: String,
        playerData: PlayerData? = null,
    ): ItemStack {
        item.editMeta { meta ->
            val lore = (meta.lore() ?: mutableListOf()).toMutableList()
            lore.add(component(""))
            lore.add(component(Texts.line("gui.common.requires")))
            requirements.forEach { requirement ->
                val path = if (playerData == null || Cost.isKnown(playerData, requirement)) {
                    costLine(requirement)
                } else {
                    Texts.render("gui.common.cost-hidden", "amount" to requirement.amount.toString())
                }
                lore.add(component(path))
            }
            if (playerData != null && requirements.any { !Cost.isKnown(playerData, it) }) {
                lore.add(component(Texts.line("gui.common.cost-hidden-hint")))
            }
            lore.add(component(Texts.line(actionPath)))
            meta.lore(lore)
        }
        return item
    }

    fun costLines(requirements: List<Cost.Requirement>): List<String> =
        requirements.map(::costLine)

    fun describe(requirements: List<Cost.Requirement>): String =
        requirements.joinToString(Texts.line("terms.list-separator")) {
            Texts.render("gui.common.cost-plain", "item" to it.label, "amount" to it.amount.toString())
        }

    private fun costLine(requirement: Cost.Requirement): String =
        Texts.render("gui.common.cost-line", "item" to requirement.label, "amount" to requirement.amount.toString())

    private fun component(text: String) =
        Messages.parse(text).decoration(TextDecoration.ITALIC, false)
}
