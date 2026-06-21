package com.tinyyana.lycohism.admin

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.expedition.RainGate
import com.tinyyana.lycohism.gui.BackedHolder
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.progression.StageStatus
import com.tinyyana.lycohism.tool.DewLight
import com.tinyyana.lycohism.tool.EnergyCrystal
import com.tinyyana.lycohism.tool.FlowerBookmark
import com.tinyyana.lycohism.tool.FlowerVeinShears
import com.tinyyana.lycohism.tool.LeylineProbe
import com.tinyyana.lycohism.tool.MoonPouch
import com.tinyyana.lycohism.tool.MossBalm
import com.tinyyana.lycohism.tool.MossFertile
import com.tinyyana.lycohism.tool.RadiantFocus
import com.tinyyana.lycohism.tool.RainBandage
import com.tinyyana.lycohism.tool.StoneworkHammer
import com.tinyyana.lycohism.tool.TuningManual
import com.tinyyana.lycohism.tool.WindVane
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.SoundMusic
import com.tinyyana.lycohism.util.Texts
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/** One admin-only inventory entry point for item, progression, reload, and expedition testing. */
class AdminPanel(private val plugin: Lycohism) : Listener {

    val itemIds: List<String>
        get() = (materialIds() + TOOL_IDS).distinct()

    fun open(player: Player) = openMain(player)

    fun buildItem(id: String, amount: Int): ItemStack? {
        val item = plugin.phenomenonManager.get(id)?.let {
            plugin.phenomenonManager.createItem(it, amount)
        } ?: when (id) {
            DewLight.ID -> plugin.dewLight.createItem(amount)
            TuningManual.ID -> plugin.tuningManual.createItem()
            FlowerBookmark.ID -> plugin.flowerBookmark.createItem()
            StoneworkHammer.ID -> plugin.stoneworkHammer.createItem()
            FlowerVeinShears.ID -> plugin.flowerVeinShears.createItem()
            RainBandage.ID -> plugin.rainBandage.createItem(amount)
            WindVane.ID -> plugin.windVane.createItem()
            MoonPouch.ID -> plugin.moonPouch.createItem()
            LeylineProbe.ID -> plugin.leylineProbe.createItem()
            RainGate.ID -> plugin.rainGate.createItem()
            MossBalm.ID -> plugin.mossBalm.createItem(amount)
            MossFertile.ID -> plugin.mossFertile.createItem(amount)
            EnergyCrystal.ID -> plugin.energyCrystal.createItem(amount)
            RadiantFocus.ID -> plugin.radiantFocus.createItem(amount)
            com.tinyyana.lycohism.tool.SolarPick.ID -> plugin.solarPick.createItem(amount)
            com.tinyyana.lycohism.tool.StarCompass.ID -> plugin.starCompass.createItem(amount)
            com.tinyyana.lycohism.tool.LunarSpore.ID -> plugin.lunarSpore.createItem(amount)
            else -> null
        } ?: return null
        item.amount = amount
        return item
    }

    private fun openMain(player: Player) {
        val inv = Menu.create(AdminHolder(AdminPage.MAIN), Texts.line("gui.admin.title"))
        inv.setItem(Menu.HEADER_SLOT, Menu.header(
            Texts.line("gui.admin.header"),
            *Texts.renderLines(
                "gui.admin.header-lore",
                "version" to plugin.pluginMeta.version,
                "debug" to Texts.line(if (plugin.debug) "terms.enabled" else "terms.disabled"),
            ).toTypedArray(),
        ))
        inv.setItem(SLOT_MATERIALS, Menu.button(Material.GLOW_BERRIES, Texts.line("gui.admin.materials"), Texts.lines("gui.admin.materials-lore")))
        inv.setItem(SLOT_TOOLS, Menu.button(Material.NETHERITE_PICKAXE, Texts.line("gui.admin.tools"), Texts.lines("gui.admin.tools-lore")))
        inv.setItem(SLOT_STAGES, Menu.button(Material.COMPASS, Texts.line("gui.admin.stages"), Texts.lines("gui.admin.stages-lore")))
        inv.setItem(SLOT_EXPEDITION, Menu.button(Material.FILLED_MAP, Texts.line("gui.admin.expedition"), Texts.lines("gui.admin.expedition-lore")))
        inv.setItem(SLOT_RELOAD, Menu.button(Material.REPEATER, Texts.line("gui.admin.reload"), Texts.lines("gui.admin.reload-lore")))
        inv.setItem(SLOT_SOUNDS, Menu.button(Material.NOTE_BLOCK, Texts.line("gui.admin.sounds"), Texts.lines("gui.admin.sounds-lore")))
        player.openInventory(inv)
    }

    private fun openItems(player: Player, page: AdminPage, ids: List<String>) {
        val path = if (page == AdminPage.MATERIALS) "materials" else "tools"
        val inv = Menu.create(AdminHolder(page), Menu.title(Texts.line("gui.admin.title"), Texts.line("gui.admin.$path")), Menu.LARGE_SIZE)
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.admin.$path"), *Texts.lines("gui.admin.items-header-lore").toTypedArray()))
        ids.take(ADMIN_ITEM_SLOTS.size).forEachIndexed { index, id ->
            val sample = buildItem(id, 1) ?: return@forEachIndexed
            inv.setItem(ADMIN_ITEM_SLOTS[index], Menu.button(
                sample.type,
                Texts.line("content-names.$id", id),
                listOf(Texts.render("gui.admin.item-id", "id" to id), Texts.line("gui.admin.click-give")),
            ))
        }
        inv.setItem(ADMIN_ITEM_BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    private fun openStages(player: Player) {
        val stages = plugin.progressionManager.stageIds()
        val current = plugin.progressionManager.statuses(player)
            .firstOrNull { it.second == StageStatus.CURRENT }?.first?.id
        val inv = Menu.create(AdminHolder(AdminPage.STAGES), Menu.title(Texts.line("gui.admin.title"), Texts.line("gui.admin.stages")), Menu.EXTENDED_SIZE)
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.admin.stages"), *Texts.lines("gui.admin.stages-header-lore").toTypedArray()))
        stages.take(ITEM_SLOTS.size).forEachIndexed { index, id ->
            val item = Menu.button(
                Material.COMPASS,
                Texts.line("progression.stages.$id.title", id),
                listOf(Texts.render("gui.admin.stage-id", "id" to id), Texts.line("gui.admin.click-stage")),
            )
            if (id == current) item.editMeta { it.setEnchantmentGlintOverride(true) }
            inv.setItem(ITEM_SLOTS[index], item)
        }
        inv.setItem(EXTENDED_BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    private fun openExpeditions(player: Player) {
        val inv = Menu.create(AdminHolder(AdminPage.EXPEDITION), Menu.title(Texts.line("gui.admin.title"), Texts.line("gui.admin.expedition")), Menu.EXTENDED_SIZE)
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.admin.expedition"), *Texts.lines("gui.admin.expedition-header-lore").toTypedArray()))
        val expeditions = plugin.expeditionManager.all()
        expeditions.take(ITEM_SLOTS.size).forEachIndexed { index, expedition ->
            inv.setItem(ITEM_SLOTS[index], Menu.button(
                Material.FILLED_MAP,
                expedition.displayName,
                listOf(Texts.render("gui.admin.expedition-id", "id" to expedition.id), Texts.line("gui.admin.click-expedition")),
            ))
        }
        // A return option occupies the row below the listed expeditions.
        inv.setItem(EXPEDITION_RETURN_SLOT, Menu.button(Material.OAK_DOOR, Texts.line("gui.admin.expedition-return"), Texts.lines("gui.admin.expedition-return-lore")))
        inv.setItem(EXTENDED_BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    private fun handleExpeditions(player: Player, slot: Int) {
        when (slot) {
            EXTENDED_BACK_SLOT -> { openMain(player); return }
            EXPEDITION_RETURN_SLOT -> {
                player.closeInventory()
                if (plugin.expeditionManager.expeditionAt(player.world) != null) plugin.expeditionManager.leave(player)
                return
            }
        }
        val expedition = plugin.expeditionManager.all().getOrNull(ITEM_SLOTS.indexOf(slot)) ?: return
        player.closeInventory()
        plugin.expeditionManager.useGateFor(player, expedition, bypassUnlock = true)
    }

    private fun openSounds(player: Player) {
        val inv = Menu.create(AdminHolder(AdminPage.SOUNDS), Menu.title(Texts.line("gui.admin.title"), Texts.line("gui.admin.sounds")))
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.admin.sounds"), *Texts.lines("gui.admin.sounds-header-lore").toTypedArray()))
        SoundMusic.THEMES.forEachIndexed { index, theme ->
            inv.setItem(SOUND_SLOTS[index], Menu.button(
                theme.icon,
                Texts.line("gui.admin.sound-${theme.id}"),
                Texts.lines("gui.admin.click-sound"),
            ))
        }
        inv.setItem(Menu.BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? AdminHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (!player.hasPermission(PERMISSION_ADMIN)) {
            player.closeInventory()
            return
        }
        when (holder.page) {
            AdminPage.MAIN -> handleMain(player, event.rawSlot)
            AdminPage.MATERIALS -> handleItems(player, event.rawSlot, materialIds())
            AdminPage.TOOLS -> handleItems(player, event.rawSlot, TOOL_IDS)
            AdminPage.STAGES -> handleStages(player, event.rawSlot)
            AdminPage.EXPEDITION -> handleExpeditions(player, event.rawSlot)
            AdminPage.SOUNDS -> handleSounds(player, event.rawSlot)
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is AdminHolder) event.isCancelled = true
    }

    private fun handleMain(player: Player, slot: Int) {
        when (slot) {
            SLOT_MATERIALS -> openItems(player, AdminPage.MATERIALS, materialIds())
            SLOT_TOOLS -> openItems(player, AdminPage.TOOLS, TOOL_IDS)
            SLOT_STAGES -> openStages(player)
            SLOT_EXPEDITION -> openExpeditions(player)
            SLOT_RELOAD -> {
                plugin.reload()
                Messages.send(player, Texts.line("commands.reloaded"))
                openMain(player)
            }
            SLOT_SOUNDS -> openSounds(player)
        }
    }

    private fun handleItems(player: Player, slot: Int, ids: List<String>) {
        if (slot == ADMIN_ITEM_BACK_SLOT) {
            openMain(player)
            return
        }
        val id = ids.getOrNull(ADMIN_ITEM_SLOTS.indexOf(slot)) ?: return
        val item = buildItem(id, 1) ?: return
        Items.give(player, item)
        com.tinyyana.lycohism.util.Audit.log(player, "admin-give", "$id x1")
        Messages.send(player, Texts.render("commands.give-done", "item" to Texts.line("content-names.$id", id), "amount" to "1"))
        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.4f)
    }

    private fun handleStages(player: Player, slot: Int) {
        if (slot == EXTENDED_BACK_SLOT) {
            openMain(player)
            return
        }
        val id = plugin.progressionManager.stageIds().getOrNull(ITEM_SLOTS.indexOf(slot)) ?: return
        if (plugin.progressionManager.setCurrentStage(player, id)) {
            Messages.send(player, Texts.render("commands.stage-done", "stage" to id))
            openStages(player)
        }
    }

    private fun handleSounds(player: Player, slot: Int) {
        if (slot == Menu.BACK_SLOT) {
            openMain(player)
            return
        }
        val theme = SoundMusic.THEMES.getOrNull(SOUND_SLOTS.indexOf(slot)) ?: return
        SoundMusic.play(plugin, player, theme)
    }

    private fun materialIds(): List<String> = plugin.phenomenonManager.all().map { it.id }

    private class AdminHolder(val page: AdminPage) : BackedHolder {
        override lateinit var backing: Inventory
    }

    private enum class AdminPage { MAIN, MATERIALS, TOOLS, STAGES, EXPEDITION, SOUNDS }

    private companion object {
        const val PERMISSION_ADMIN = "lycohism.admin"
        const val SLOT_MATERIALS = 9
        const val SLOT_TOOLS = 11
        const val SLOT_STAGES = 13
        const val SLOT_EXPEDITION = 15
        const val SLOT_RELOAD = 17
        const val SLOT_SOUNDS = 22
        val SOUND_SLOTS = listOf(10, 12, 14, 16)
        val ITEM_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25)
        val ADMIN_ITEM_SLOTS = ITEM_SLOTS + listOf(28, 29, 30, 31, 32, 33, 34)
        val EXTENDED_BACK_SLOT = Menu.backSlotAfter(ITEM_SLOTS)
        val ADMIN_ITEM_BACK_SLOT = Menu.backSlotAfter(ADMIN_ITEM_SLOTS)
        const val EXPEDITION_RETURN_SLOT = 33
        val TOOL_IDS = listOf(
            DewLight.ID,
            TuningManual.ID,
            FlowerBookmark.ID,
            StoneworkHammer.ID,
            FlowerVeinShears.ID,
            RainBandage.ID,
            WindVane.ID,
            MoonPouch.ID,
            LeylineProbe.ID,
            RainGate.ID,
            MossBalm.ID,
            MossFertile.ID,
            EnergyCrystal.ID,
            RadiantFocus.ID,
            com.tinyyana.lycohism.tool.SolarPick.ID,
            com.tinyyana.lycohism.tool.StarCompass.ID,
            com.tinyyana.lycohism.tool.LunarSpore.ID,
        )
    }
}
