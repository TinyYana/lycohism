package com.tinyyana.lycohism.facility

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.gui.StudyHolder
import com.tinyyana.lycohism.gui.StudyMenu
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.modifyMeta
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

    fun open(player: Player, upgraded: Boolean = true) = openMain(player, upgraded)

    /** Content level actually exposed: Lv2 perks need the upgraded structure (or command). */
    private fun effectiveLevel(player: Player, upgraded: Boolean): Int =
        if (upgraded) level(player) else level(player).coerceAtMost(1)

    // ---- Menus -------------------------------------------------------------

    private fun openMain(player: Player, upgraded: Boolean) {
        val stored = level(player)
        val sealed = stored <= 0
        val title = Texts.line(if (sealed) "gui.study.sealed" else "gui.study.root")
        val inv = create(StudyMenu.MAIN, title, upgraded)

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
            inv.setItem(SLOT_STATUS, Menu.button(Material.LECTERN, Texts.line("gui.study.status"), Texts.renderLines("gui.study.status-lore", "level" to stored.toString())))
            if (stored in 1 until FacilityUpgrade.MAX_LEVEL) inv.setItem(SLOT_UPGRADE, FacilityUi.upgradeButton(plugin, "study", stored))
            else inv.setItem(SLOT_UPGRADE, FacilityUi.maxedButton())
        }
        player.openInventory(inv)
    }

    private fun openBooks(player: Player, upgraded: Boolean) {
        val inv = create(StudyMenu.BOOKS, Menu.title(Texts.line("gui.study.root"), Texts.line("gui.study.books-leaf")), upgraded)
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.study.books-leaf"), *Texts.lines("gui.study.header-books-lore").toTypedArray()))
        inv.setItem(SLOT_MANUAL, Menu.button(Material.BOOK, Texts.line("gui.study.manual"), Texts.lines("gui.study.manual-lore")))
        inv.setItem(SLOT_COMPENDIUM, Menu.button(Material.WRITTEN_BOOK, Texts.line("gui.study.compendium"), Texts.lines("gui.study.compendium-lore")))
        inv.setItem(SLOT_RECORD, Menu.button(Material.KNOWLEDGE_BOOK, Texts.line("gui.study.record"), Texts.lines("gui.study.record-lore")))
        inv.setItem(Menu.BACK_SLOT, Menu.back())
        player.openInventory(inv)
    }

    /** One entry in the 器物 leaf, paired with its action; the list drives both layout and clicks so
     *  buttons stay centred (Menu.centeredRow) and grow symmetrically as Lv2/Lv3 add 地圖標記/全境預報. */
    private class ToolEntry(val render: ItemStack, val onClick: () -> Unit)

    private fun toolEntries(player: Player, effective: Int): List<ToolEntry> {
        val data = plugin.playerDataManager.rememberInventoryMaterials(player)
        fun craftEntry(create: () -> ItemStack, cost: List<String>, onClick: () -> Unit) =
            ToolEntry(FacilityUi.withCost(create(), Cost.parse(cost, plugin), "gui.common.click-craft", data), onClick)
        return buildList {
            add(craftEntry({ plugin.windVane.createItem() }, plugin.windVane.cost) {
                craftTool(player, plugin.windVane.cost, WindVane.ID) { plugin.windVane.createItem() }
            })
            add(craftEntry({ plugin.moonPouch.createItem() }, plugin.moonPouch.cost) {
                craftTool(player, plugin.moonPouch.cost, MoonPouch.ID) { plugin.moonPouch.createItem() }
            })
            // 星圖羅盤提早到書房 Lv1（#1：原本要塔產能升 Lv2 才能做，但它正是用來找塔的，邏輯倒置）。
            add(craftEntry({ plugin.starCompass.createItem() }, plugin.starCompass.craftCost) {
                craftTool(player, plugin.starCompass.craftCost, com.tinyyana.lycohism.tool.StarCompass.ID) { plugin.starCompass.createItem() }
            })
            // Lv2（升級結構）：標記最近能量塔到一張鎖定地圖（QoL，#4）。
            if (effective >= 2) add(ToolEntry(
                Menu.button(Material.FILLED_MAP, Texts.line("gui.study.map-mark"), Texts.lines("gui.study.map-mark-lore")),
            ) { markNearestTower(player) })
            // Lv2（升級結構）：現象凝縮台藍圖（書房風格的自動化機器）。
            if (effective >= 2) add(craftEntry({ plugin.blueprint.createItem(CONDENSER_ID) }, CONDENSER_BP_COST) {
                craftTool(player, CONDENSER_BP_COST, CONDENSER_ID) { plugin.blueprint.createItem(CONDENSER_ID) }
            })
            // Lv3（蝕輝）：全境預報——讀出此刻此地所有可採集的自然現象，均衡口味的「探索向」三階強化。
            if (effective >= 3) add(ToolEntry(
                Menu.button(Material.SPYGLASS, Texts.line("gui.study.forecast"), Texts.lines("gui.study.forecast-lore")),
            ) { forecastAll(player) })
        }
    }

    private fun openTools(player: Player, upgraded: Boolean) {
        val entries = toolEntries(player, effectiveLevel(player, upgraded))
        val slots = Menu.centeredRow(entries.size)
        val inv = create(
            StudyMenu.TOOLS,
            Menu.title(Texts.line("gui.study.root"), Texts.line("gui.study.tools-leaf")),
            upgraded,
            Menu.sizeFor(slots.max()),
        )
        inv.setItem(Menu.HEADER_SLOT, Menu.header(Texts.line("gui.study.tools-leaf"), *Texts.lines("gui.study.header-tools-lore").toTypedArray()))
        entries.forEachIndexed { i, entry -> inv.setItem(slots[i], entry.render) }
        inv.setItem(Menu.backSlotAfter(slots), Menu.back())
        player.openInventory(inv)
    }

    private fun openExpedition(player: Player, upgraded: Boolean) {
        val inv = create(StudyMenu.EXPEDITION, Menu.title(Texts.line("gui.study.root"), Texts.line("gui.study.expedition-leaf")), upgraded)
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
                // Per-expedition hint when present (e.g. 暮蝕需走過日月兩線), else the generic gate lore.
                Texts.lines("gui.study.gate-locked-lore-${expedition.id}", Texts.lines("gui.study.gate-locked-lore")),
            )
        }

    // ---- Click handling ----------------------------------------------------

    fun handleClick(player: Player, holder: StudyHolder, slot: Int) {
        when (holder.menu) {
            StudyMenu.MAIN -> handleMain(player, slot, holder.upgraded)
            StudyMenu.BOOKS -> handleBooks(player, slot, holder.upgraded)
            StudyMenu.TOOLS -> handleTools(player, slot, holder.upgraded)
            StudyMenu.EXPEDITION -> handleExpedition(player, slot, holder.upgraded)
        }
    }

    private fun handleMain(player: Player, slot: Int, upgraded: Boolean) {
        if (level(player) <= 0) {
            if (slot == SLOT_REPAIR) repair(player, upgraded)
            return
        }
        when (slot) {
            SLOT_BOOKS -> openBooks(player, upgraded)
            SLOT_TOOLS -> openTools(player, upgraded)
            SLOT_PROGRESS -> plugin.progressionManager.open(player)
            SLOT_EXPEDITION -> openExpedition(player, upgraded)
            SLOT_STATUS -> showStatus(player)
            SLOT_UPGRADE -> if (level(player) in 1 until FacilityUpgrade.MAX_LEVEL) FacilityUi.upgradeClick(plugin, player, "study") { openMain(player, upgraded) }
        }
    }

    private fun handleBooks(player: Player, slot: Int, upgraded: Boolean) {
        when (slot) {
            SLOT_MANUAL -> openBookNextTick(player) { plugin.tuningManual.open(player) }
            SLOT_COMPENDIUM -> openBookNextTick(player) { plugin.studyBooks.openCompendium(player) }
            SLOT_RECORD -> openBookNextTick(player) { plugin.studyBooks.openDiscoveryRecord(player) }
            Menu.BACK_SLOT -> openMain(player, upgraded)
        }
    }

    private fun handleTools(player: Player, slot: Int, upgraded: Boolean) {
        val entries = toolEntries(player, effectiveLevel(player, upgraded))
        val slots = Menu.centeredRow(entries.size)
        val index = slots.indexOf(slot)
        if (index in entries.indices) {
            entries[index].onClick()
            return
        }
        if (slot == Menu.backSlotAfter(slots)) openMain(player, upgraded)
    }

    /** Lv3 全境預報: lists every natural phenomenon collectable here and now. */
    private fun forecastAll(player: Player) {
        val available = plugin.phenomenonManager.available(player.location, player)
            .filter { it.chance > 0.0 }
        if (available.isEmpty()) {
            Messages.send(player, Texts.line("messages.facility.forecast-none"))
            return
        }
        Messages.send(player, Texts.line("messages.facility.forecast-header"))
        available.forEach { phenomenon ->
            Messages.send(player, Texts.render("messages.facility.forecast-line", "name" to Texts.line("content-names.${phenomenon.id}", phenomenon.displayName)))
        }
        player.playSound(player.location, Sound.ITEM_SPYGLASS_USE, 0.7f, 1.2f)
    }

    private fun handleExpedition(player: Player, slot: Int, upgraded: Boolean) {
        if (slot == Menu.BACK_SLOT) {
            openMain(player, upgraded)
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

    private fun repair(player: Player, upgraded: Boolean) {
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
        open(player, upgraded)
    }

    /** Lv2 QoL: hands the player a locked map centred on the nearest 輝能塔 (#4 「地圖標記塔」). */
    private fun markNearestTower(player: Player) {
        val loc = player.location
        val nearest = plugin.energyTowers.all()
            .filter { it.world == player.world.name }
            .minByOrNull { val dx = it.x - loc.x; val dz = it.z - loc.z; dx * dx + dz * dz }
        if (nearest == null) {
            Messages.send(player, Texts.line("messages.tools.compass-none"))
            return
        }
        val view = plugin.server.createMap(player.world)
        view.centerX = nearest.x
        view.centerZ = nearest.z
        view.scale = org.bukkit.map.MapView.Scale.NORMAL
        // A persistent target marker at the tower (like a vanilla explorer map) so it's useful even
        // before the terrain is rendered; left unlocked so the map fills in as the player travels.
        view.addRenderer(TowerMarkerRenderer(towerCursorType()))
        val towerName = Texts.line("content-names.${if (nearest.type == com.tinyyana.lycohism.energy.EnergyType.SUN) "sun_tower" else "moon_tower"}")
        val map = ItemStack(Material.FILLED_MAP).apply {
            modifyMeta(org.bukkit.inventory.meta.MapMeta::class.java) { meta ->
                meta.mapView = view
                Messages.applyDisplayName(meta, Texts.render("gui.study.map-mark-name", "name" to towerName))
            }
        }
        Items.give(player, map)
        com.tinyyana.lycohism.util.Audit.log(player, "tower-map", "${nearest.x},${nearest.z}")
        Messages.send(player, Texts.render("messages.facility.tower-marked", "x" to nearest.x.toString(), "z" to nearest.z.toString()))
        player.playSound(player.location, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.7f, 1.2f)
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

    private fun create(menu: StudyMenu, title: String, upgraded: Boolean, size: Int = Menu.COMPACT_SIZE): Inventory =
        Menu.create(StudyHolder(menu, upgraded), title, size)

    /** Best-effort target-cursor type (enum names vary across versions). */
    private fun towerCursorType(): org.bukkit.map.MapCursor.Type =
        listOf("TARGET_POINT", "RED_X", "RED_MARKER").firstNotNullOfOrNull { name ->
            runCatching { org.bukkit.map.MapCursor.Type.valueOf(name) }.getOrNull()
        } ?: org.bukkit.map.MapCursor.Type.values().first()

    /** Paints a single persistent marker at the map's centre (the tower it was made for). */
    private class TowerMarkerRenderer(private val type: org.bukkit.map.MapCursor.Type) : org.bukkit.map.MapRenderer() {
        override fun render(map: org.bukkit.map.MapView, canvas: org.bukkit.map.MapCanvas, player: Player) {
            if (canvas.cursors.size() > 0) return
            canvas.cursors.addCursor(org.bukkit.map.MapCursor(0.toByte(), 0.toByte(), 0.toByte(), type, true))
        }
    }

    companion object {
        private const val FILE_NAME = "facilities.yml"
        private const val CONDENSER_ID = "phenomenon_condenser"
        private val CONDENSER_BP_COST = listOf("rain_breath:2", "AMETHYST_SHARD:4", "PAPER:1")

        // MAIN — six entries sit consecutively (11–16) for a clean row; upgrade takes 16 when shown.
        private const val SLOT_REPAIR = 13
        private const val SLOT_BOOKS = 11
        private const val SLOT_TOOLS = 12
        private const val SLOT_PROGRESS = 13
        private const val SLOT_EXPEDITION = 14
        private const val SLOT_STATUS = 15
        private const val SLOT_UPGRADE = 16

        // 書冊 leaf
        private const val SLOT_MANUAL = 11
        private const val SLOT_COMPENDIUM = 13
        private const val SLOT_RECORD = 15

        // 器物 leaf 的工具改由 toolEntries + Menu.centeredRow 動態置中，不再用固定 slot 常數。

        // 遠征 leaf — 一格一個遠征，目前四個（雨後森林／永夜荒原／潮汐深淵／暮蝕之境），
        // 對稱排在第二排。暮蝕之境是 BOSS 戰場，靠 requires-advancements 鎖到走過日月兩線才解。
        // ponytail: 4 格剛好容納現有四個遠征；之後超過 4 個再改成分頁。
        private const val SLOT_GATE = 13
        private val EXPEDITION_SLOTS = listOf(10, 12, 14, 16)

        private const val DISCOVERY_ID = "study"
    }
}
