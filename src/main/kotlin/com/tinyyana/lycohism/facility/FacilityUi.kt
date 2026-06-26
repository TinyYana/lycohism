package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.data.PlayerData
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** Shared facility lore formatting so cost/action blocks stay visually identical. */
object FacilityUi {

    fun upgradeButton(plugin: Lycohism, facility: String, currentLevel: Int, playerLocale: String = ""): ItemStack {
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
                addAll(costLines(requirements, playerLocale))
            }
            add("")
            add(Texts.line("gui.facility.upgrade-action"))
        }
        val icon = if (target >= 3) Material.HEART_OF_THE_SEA else Material.AMETHYST_CLUSTER
        return ItemStack(icon).apply {
            modifyMeta { meta ->
                Messages.applyDisplayName(meta, Texts.render("gui.facility.upgrade", "level" to target.toString()))
                Messages.applyLore(meta, lore)
                meta.setEnchantmentGlintOverride(true)
            }
        }
    }

    fun maxedButton(): ItemStack = ItemStack(Material.BARRIER).apply {
        modifyMeta { meta ->
            Messages.applyDisplayName(meta, Texts.line("gui.facility.maxed"))
            Messages.applyLore(meta, Texts.lines("gui.facility.maxed-lore"))
        }
    }

    fun upgradeClick(plugin: Lycohism, player: Player, facility: String, reopen: () -> Unit) {
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
        playerLocale: String = "",
    ): ItemStack {
        item.modifyMeta { meta ->
            val lore = Messages.getLore(meta)
            lore.add(Messages.loreLine(""))
            lore.add(Messages.loreLine(Texts.line("gui.common.requires")))
            requirements.forEach { requirement ->
                val path = if (playerData == null || Cost.isKnown(playerData, requirement)) {
                    costLine(requirement, playerLocale)
                } else {
                    Texts.render("gui.common.cost-hidden", "amount" to requirement.amount.toString())
                }
                lore.add(Messages.loreLine(path))
            }
            if (playerData != null && requirements.any { !Cost.isKnown(playerData, it) }) {
                lore.add(Messages.loreLine(Texts.line("gui.common.cost-hidden-hint")))
            }
            lore.add(Messages.loreLine(Texts.line(actionPath)))
            @Suppress("DEPRECATION")
            meta.setLore(lore)
        }
        return item
    }

    fun costLines(requirements: List<Cost.Requirement>, playerLocale: String = ""): List<String> =
        requirements.map { costLine(it, playerLocale) }

    fun describe(requirements: List<Cost.Requirement>, playerLocale: String = ""): String =
        requirements.joinToString(Texts.line("terms.list-separator")) {
            Texts.render("gui.common.cost-plain", "item" to it.labelFor(playerLocale), "amount" to it.amount.toString())
        }

    private fun costLine(requirement: Cost.Requirement, playerLocale: String = ""): String =
        Texts.render("gui.common.cost-line", "item" to requirement.labelFor(playerLocale), "amount" to requirement.amount.toString())
}
