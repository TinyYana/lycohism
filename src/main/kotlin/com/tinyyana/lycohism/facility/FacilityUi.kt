package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.data.PlayerData
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack

/** Shared facility lore formatting so cost/action blocks stay visually identical. */
object FacilityUi {

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
