package com.tinyyana.lycohism.listener

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.expedition.RainGate
import com.tinyyana.lycohism.phenomenon.RainTending
import com.tinyyana.lycohism.tool.Blueprint
import com.tinyyana.lycohism.tool.BuildingWand
import com.tinyyana.lycohism.tool.DewLight
import com.tinyyana.lycohism.tool.EnergyCrystal
import com.tinyyana.lycohism.tool.FlowerBookmark
import com.tinyyana.lycohism.tool.FlowerVeinShears
import com.tinyyana.lycohism.tool.LeylineProbe
import com.tinyyana.lycohism.tool.MossBalm
import com.tinyyana.lycohism.tool.MossFertile
import com.tinyyana.lycohism.tool.LunarSpore
import com.tinyyana.lycohism.tool.RadiantFocus
import com.tinyyana.lycohism.tool.RainBandage
import com.tinyyana.lycohism.tool.SolarPick
import com.tinyyana.lycohism.tool.StarCompass
import com.tinyyana.lycohism.tool.StoneworkHammer
import com.tinyyana.lycohism.tool.TuningManual
import com.tinyyana.lycohism.tool.WindVane
import com.tinyyana.lycohism.tool.MoonPouch
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/** Routes right-click activation of Lycohism life tools to their behaviour. */
class ToolUseListener(private val plugin: Lycohism) : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val id = Items.idOf(item) ?: return
        val player = event.player
        val rightClick = event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK

        if (id == WindVane.ID || id == LeylineProbe.ID) {
            if (!rightClick) return
            event.isCancelled = true
            plugin.heldToolStatusBar.showNow(player)
            return
        }

        if (event.hand != EquipmentSlot.HAND) return

        when (id) {
            RainBandage.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                if (plugin.rainBandage.use(player, item)) {
                    plugin.playerDataManager.discover(player.uniqueId, RainBandage.ID)
                    Messages.actionBar(player, Texts.line("messages.tools.bandage-used"))
                } else {
                    Messages.actionBar(player, Texts.line("messages.tools.bandage-not-needed"))
                }
            }

            DewLight.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                plugin.dewLight.use(player, item)
                plugin.playerDataManager.discover(player.uniqueId, DewLight.ID)
                Messages.actionBar(player, Texts.line("messages.tools.dew-light-used"))
            }

            TuningManual.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                plugin.tuningManual.open(player)
            }

            FlowerBookmark.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                if (player.isSneaking) {
                    plugin.flowerBookmark.remember(player, item)
                } else {
                    plugin.flowerBookmark.guide(player, item)
                }
            }

            RainGate.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                if (plugin.expeditionManager.useGate(player)) {
                    plugin.playerDataManager.discover(player.uniqueId, RainGate.ID)
                }
            }

            MoonPouch.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                val released = plugin.moonPouch.release(player, item)
                Messages.actionBar(
                    player,
                    Texts.line(if (released) "messages.tools.moon-pouch-released" else "messages.tools.moon-pouch-empty"),
                )
            }

            StoneworkHammer.ID, StoneworkHammer.REINFORCED_ID -> {
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                // Damping: ignore the rapid follow-up clicks so a held/spammed right-click doesn't
                // re-fire mid-confirm. The cooldown also shows the vanilla hotbar sweep as feedback.
                if (player.getCooldown(item.type) > 0) { event.isCancelled = true; return }
                val block = event.clickedBlock ?: return
                when (plugin.stoneworkHammer.tryCycle(player, block, event.blockFace, item)) {
                    StoneworkHammer.Result.PREVIEW -> {
                        event.isCancelled = true
                        player.setCooldown(item.type, TOOL_COOLDOWN_TICKS)
                        Messages.actionBar(player, Texts.line("messages.tools.hammer-preview"))
                    }
                    StoneworkHammer.Result.CHANGED -> {
                        event.isCancelled = true
                        player.setCooldown(item.type, TOOL_COOLDOWN_TICKS)
                    }
                    StoneworkHammer.Result.INVALID -> Unit
                }
            }

            BuildingWand.ID, BuildingWand.TIER_2_ID -> {
                // Mode switch: sneak + right-click anywhere (air or block) so it's discoverable
                // without needing to aim at a block.
                if (player.isSneaking && id == BuildingWand.TIER_2_ID) {
                    if (!rightClick) return
                    event.isCancelled = true
                    val mode = plugin.buildingWand.cycleMode(item)
                    player.inventory.setItemInMainHand(item)
                    Messages.actionBar(player, Texts.render("messages.tools.wand-mode", "mode" to Texts.line("terms.wand-mode.${mode.name.lowercase()}")))
                    return
                }
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                event.isCancelled = true
                if (player.getCooldown(item.type) > 0) return
                val result = plugin.buildingWand.use(player, event.clickedBlock ?: return, event.blockFace, item)
                player.setCooldown(item.type, TOOL_COOLDOWN_TICKS)
                if (!result.preview && result.amount > 0) plugin.playerDataManager.discover(player.uniqueId, id)
                Messages.actionBar(player, Texts.render(if (result.preview) "messages.tools.wand-preview" else "messages.tools.building-wand", "amount" to result.amount.toString(), "mode" to Texts.line("terms.wand-mode.${result.mode.name.lowercase()}")))
            }

            FlowerVeinShears.ID -> {
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                val block = event.clickedBlock ?: return
                if (plugin.flowerVeinShears.handle(player, block, item)) {
                    event.isCancelled = true
                    plugin.playerDataManager.discover(player.uniqueId, FlowerVeinShears.ID)
                }
            }

            MossBalm.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                plugin.mossBalm.use(player, item)
                plugin.playerDataManager.discover(player.uniqueId, MossBalm.ID)
                Messages.actionBar(player, Texts.line("messages.tools.moss-balm-used"))
            }

            MossFertile.ID -> {
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                val block = event.clickedBlock ?: return
                event.isCancelled = true // replace vanilla single-block bone meal with the area version
                if (plugin.mossFertile.use(player, block, item)) {
                    plugin.playerDataManager.discover(player.uniqueId, MossFertile.ID)
                    Messages.actionBar(player, Texts.line("messages.tools.moss-fertile-used"))
                } else {
                    Messages.actionBar(player, Texts.line("messages.tools.moss-fertile-empty"))
                }
            }

            RainTending.RAIN_BREATH_ID -> {
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                val block = event.clickedBlock ?: return
                if (plugin.rainTending.tend(player, block, item)) {
                    event.isCancelled = true
                    plugin.playerDataManager.discover(player.uniqueId, RainTending.DISCOVERY_ID)
                }
            }

            EnergyCrystal.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                val moved = plugin.energyCrystal.bank(player, item)
                player.inventory.setItemInMainHand(item) // persist the drained crystal
                plugin.playerDataManager.discover(player.uniqueId, EnergyCrystal.ID)
                Messages.actionBar(
                    player,
                    Texts.render(
                        if (moved > 0) "messages.tools.crystal-banked" else "messages.tools.crystal-empty",
                        "amount" to moved.toString(),
                    ),
                )
            }

            SolarPick.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                Messages.actionBar(player, Texts.line(if (plugin.solarPick.use(player)) "messages.tools.solar-pick-used" else "messages.tools.solar-pick-empty"))
            }

            StarCompass.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                Messages.actionBar(player, plugin.starCompass.reading(player))
                plugin.playerDataManager.discover(player.uniqueId, StarCompass.ID)
            }

            LunarSpore.ID -> {
                if (event.action != Action.RIGHT_CLICK_BLOCK) return
                val block = event.clickedBlock ?: return
                event.isCancelled = true
                Messages.actionBar(player, Texts.line(if (plugin.lunarSpore.use(player, block)) "messages.tools.lunar-spore-used" else "messages.tools.lunar-spore-empty"))
            }

            Blueprint.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                if (player.isSneaking) {
                    val key = when (plugin.blueprint.build(player, item)) {
                        Blueprint.Result.BUILT -> "messages.tools.blueprint-built"
                        Blueprint.Result.MISSING_MATERIALS -> "messages.tools.blueprint-missing"
                        Blueprint.Result.INVALID -> "messages.tools.blueprint-invalid"
                    }
                    Messages.actionBar(player, Texts.line(key))
                } else {
                    plugin.blueprint.preview(player, item)
                    Messages.actionBar(player, Texts.line("messages.tools.blueprint-preview"))
                    Messages.send(player, Texts.render("messages.tools.blueprint-materials", "materials" to plugin.blueprint.materialSummary(item)))
                }
            }

            RadiantFocus.ID -> {
                if (!rightClick) return
                event.isCancelled = true
                val moon = player.isSneaking
                if (plugin.radiantFocus.use(player, moon)) {
                    plugin.playerDataManager.discover(player.uniqueId, RadiantFocus.ID)
                    Messages.actionBar(player, Texts.line(if (moon) "messages.tools.focus-moon" else "messages.tools.focus-sun"))
                } else {
                    Messages.actionBar(player, Texts.line(if (moon) "messages.tools.focus-no-moon" else "messages.tools.focus-no-sun"))
                }
            }

            EMBER_BLOOM_ID -> {
                if (!rightClick) return
                event.isCancelled = true
                player.addPotionEffect(
                    PotionEffect(PotionEffectType.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS, 0, false, true, true),
                )
                if (player.gameMode != GameMode.CREATIVE) {
                    if (item.amount <= 1) player.inventory.setItemInMainHand(null) else item.amount -= 1
                }
                player.playSound(player.location, Sound.ITEM_FIRECHARGE_USE, 0.7f, 1.2f)
                plugin.playerDataManager.discover(player.uniqueId, EMBER_BLOOM_ID)
                Messages.actionBar(player, Texts.line("messages.tools.ember-used"))
            }
        }
    }

    private companion object {
        /** Phenomenon id for 燼華; cracking it grants brief Fire Resistance. */
        const val EMBER_BLOOM_ID = "ember_bloom"
        const val FIRE_RESISTANCE_TICKS = 600 // 30s

        /** Short post-action cooldown that gives the build tools "阻尼感" and stops click spam re-firing. */
        const val TOOL_COOLDOWN_TICKS = 4
    }
}
