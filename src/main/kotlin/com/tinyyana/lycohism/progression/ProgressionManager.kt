package com.tinyyana.lycohism.progression

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.data.PlayerData
import com.tinyyana.lycohism.gui.Menu
import com.tinyyana.lycohism.gui.ProgressionHolder
import com.tinyyana.lycohism.util.Advancements
import com.tinyyana.lycohism.util.ConfigFiles
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.util.modifyMeta
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Loads the progression line from progression.yml and renders it as a dedicated, read-only GUI:
 * the 調律之路. A chapter is "done" when its discovery / advancement / facility requirements are
 * all met; the first unmet chapter is the player's current objective, later ones stay hidden so
 * the road keeps a sense of discovery (PROJECT_PLAN §18.2). Data-driven; new chapters need only
 * a YAML entry plus its lang text.
 */
class ProgressionManager(private val plugin: Lycohism) {

    private val stages = mutableListOf<ProgressionStage>()

    init {
        reload()
    }

    fun reload() {
        stages.clear()
        val section = ConfigFiles.load(plugin, FILE_NAME).getConfigurationSection("progression.stages")
        if (section == null) {
            plugin.logger.warning("$FILE_NAME has no 'progression.stages'; progression line is empty.")
            return
        }
        for (id in section.getKeys(false)) {
            val node = section.getConfigurationSection(id) ?: continue
            stages += parse(id, node)
        }
        registerAdvancements()
        plugin.server.onlinePlayers.forEach(::syncAdvancements)
        plugin.logger.info("Loaded ${stages.size} progression stages.")
    }

    // ---- Evaluation --------------------------------------------------------

    private fun isComplete(player: Player, stage: ProgressionStage): Boolean {
        if (stage.preview) return false
        val data = plugin.playerDataManager.get(player.uniqueId)
        if (!data.discoveries.containsAll(stage.requiredDiscoveries)) return false
        if (!Advancements.hasAll(player, stage.requiredAdvancements)) return false
        return stage.requiredFacilities.all { (id, level) -> facilityLevel(data, id) >= level }
    }

    /** Each chapter paired with its state for [player]. */
    fun statuses(player: Player): List<Pair<ProgressionStage, StageStatus>> {
        val currentId = stages.firstOrNull { !it.preview && !isComplete(player, it) }?.id
        return stages.map { stage ->
            val status = when {
                stage.preview -> StageStatus.UPCOMING
                isComplete(player, stage) -> StageStatus.DONE
                stage.id == currentId -> StageStatus.CURRENT
                else -> StageStatus.LOCKED
            }
            stage to status
        }
    }

    private fun facilityLevel(data: PlayerData, id: String): Int = when (id.lowercase()) {
        "workshop" -> data.workshopLevel
        "study" -> data.studyLevel
        "greenhouse" -> data.greenhouseLevel
        else -> 0
    }

    private fun setFacilityLevel(data: PlayerData, id: String, level: Int) {
        when (id.lowercase()) {
            "workshop" -> data.workshopLevel = level
            "study" -> data.studyLevel = level
            "greenhouse" -> data.greenhouseLevel = level
        }
    }

    fun stageIds(): List<String> = stages.filterNot { it.preview }.map { it.id }

    /** Resets controlled progression fields so [stageId] becomes the current debug stage. */
    fun setCurrentStage(player: Player, stageId: String): Boolean {
        val playable = stages.filterNot { it.preview }
        val index = playable.indexOfFirst { it.id == stageId }
        if (index < 0) return false
        val completed = playable.take(index)

        playable.flatMap { it.requiredAdvancements }.toSet().forEach { advancementKey ->
            setAdvancement(player, advancementKey, completed.any { advancementKey in it.requiredAdvancements })
        }
        plugin.playerDataManager.update(player.uniqueId) { data ->
            data.discoveries.removeAll(playable.flatMap { it.requiredDiscoveries + it.requiredFacilities.keys }.toSet())
            data.workshopLevel = 0
            data.studyLevel = 0
            data.greenhouseLevel = 0
            completed.forEach { stage ->
                data.discoveries.addAll(stage.requiredDiscoveries)
                stage.requiredFacilities.forEach { (id, level) ->
                    setFacilityLevel(data, id, maxOf(facilityLevel(data, id), level))
                    data.discoveries.add(id)
                }
            }
        }
        plugin.dewLight.syncRecipe(player)
        syncAdvancements(player)
        return true
    }

    private fun setAdvancement(player: Player, key: String, complete: Boolean) {
        val advancement = NamespacedKey.fromString(key)?.let(Bukkit::getAdvancement) ?: return
        val progress = player.getAdvancementProgress(advancement)
        if (complete) progress.remainingCriteria.forEach(progress::awardCriteria)
        else progress.awardedCriteria.forEach(progress::revokeCriteria)
    }

    // ---- GUI ---------------------------------------------------------------

    fun open(player: Player) {
        val inv = Menu.create(ProgressionHolder(), Texts.line("gui.progression.title"), Menu.LARGE_SIZE)
        val statuses = statuses(player)
        val done = statuses.count { it.second == StageStatus.DONE }
        val total = stages.count { !it.preview }

        inv.setItem(
            Menu.HEADER_SLOT,
            Menu.header(
                Texts.line("gui.progression.header"),
                *Texts.renderLines("gui.progression.header-lore", "done" to done.toString(), "total" to total.toString()).toTypedArray(),
            ),
        )

        statuses.forEachIndexed { index, (stage, status) ->
            if (index >= TRACK_SLOTS.size) return@forEachIndexed
            inv.setItem(TRACK_SLOTS[index], stageItem(stage, status))
        }
        inv.setItem(backSlot(), Menu.back())
        if (stages.size > TRACK_SLOTS.size) {
            plugin.logger.warning("Progression has ${stages.size} stages but only ${TRACK_SLOTS.size} track slots; extra chapters are hidden.")
        }
        player.openInventory(inv)
    }

    fun backSlot(): Int =
        Menu.backSlotAfter(TRACK_SLOTS.take(stages.size.coerceIn(1, TRACK_SLOTS.size)))

    private fun stageItem(stage: ProgressionStage, status: StageStatus): ItemStack = when (status) {
        StageStatus.DONE -> glint(
            Menu.button(stage.icon, Texts.render("gui.progression.done-name", "title" to stage.title), buildList {
                addAll(stage.lore)
                if (stage.details.isNotEmpty()) {
                    add("")
                    add(Texts.line("gui.progression.unlocked-label"))
                    addAll(stage.details)
                }
            }),
        )
        StageStatus.CURRENT -> Menu.button(
            stage.icon,
            Texts.render("gui.progression.current-name", "title" to stage.title),
            buildList {
                addAll(stage.lore)
                add("")
                add(Texts.line("gui.progression.objective-label"))
                if (stage.hint.isNotEmpty()) add(stage.hint)
            },
        )
        StageStatus.LOCKED -> Menu.button(
            LOCKED_MATERIAL,
            Texts.line("gui.progression.locked-name"),
            Texts.lines("gui.progression.locked-lore"),
        )
        StageStatus.UPCOMING -> Menu.button(
            stage.icon,
            Texts.render("gui.progression.upcoming-name", "title" to stage.title),
            stage.lore,
        )
    }

    private fun glint(item: ItemStack): ItemStack = item.apply {
        modifyMeta { it.setEnchantmentGlintOverride(true) }
    }

    // ---- Parsing -----------------------------------------------------------

    private fun parse(id: String, node: ConfigurationSection): ProgressionStage {
        val requires = node.getConfigurationSection("requires")
        return ProgressionStage(
            id = id,
            icon = Material.matchMaterial(node.getString("icon", "PAPER")!!.trim().uppercase()) ?: Material.PAPER,
            title = Texts.line("progression.stages.$id.title", id),
            lore = Texts.lines("progression.stages.$id.lore"),
            details = Texts.lines("progression.stages.$id.details"),
            hint = Texts.line("progression.stages.$id.hint", ""),
            requiredDiscoveries = requires?.getStringList("discoveries")?.map { it.trim() }?.toSet() ?: emptySet(),
            requiredAdvancements = parseAdvancements(id, requires),
            requiredFacilities = parseFacilities(requires),
            preview = node.getBoolean("preview", false),
            advancementParent = node.getString("advancement-parent")?.trim()?.takeIf { it.isNotEmpty() },
            advancementSound = node.getString("advancement-sound")?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun parseAdvancements(id: String, requires: ConfigurationSection?): Set<String> =
        requires?.getStringList("advancements")
            ?.map { it.trim() }
            ?.filter { key ->
                Advancements.isValidKey(key).also { valid ->
                    if (!valid) plugin.logger.warning("Progression '$id': invalid advancement key '$key'; ignoring.")
                }
            }
            ?.toSet() ?: emptySet()

    private fun parseFacilities(requires: ConfigurationSection?): Map<String, Int> {
        val facilities = requires?.getConfigurationSection("facilities") ?: return emptyMap()
        return facilities.getKeys(false).associateWith { facilities.getInt(it).coerceAtLeast(1) }
    }

    /** Mirrors completed Lycohism chapters into the vanilla advancement screen. */
    fun syncAdvancements(player: Player) {
        // Root "Lycohism" is granted on first sync (join) so the tab actually unlocks and the
        // chapter line has an obtained anchor — previously it was pinned incomplete and never fired.
        award(player, ROOT_KEY, true)
        award(player, INTRO_KEY, true)
        statuses(player).forEach { (stage, status) ->
            if (!stage.preview) award(player, key(stage.id), status == StageStatus.DONE, stage.advancementSound)
        }
    }

    @Suppress("DEPRECATION")
    private fun registerAdvancements() {
        val keys = stages.filterNot { it.preview }.map { key(it.id) } + INTRO_KEY + ROOT_KEY
        var removedPersistentAdvancement = false
        keys.asReversed().forEach { key ->
            removedPersistentAdvancement = Bukkit.getUnsafe().removeAdvancement(key) || removedPersistentAdvancement
        }
        // Paper 26.2: removeAdvancement only deletes storage; reloadData removes the running copy.
        if (removedPersistentAdvancement) plugin.server.reloadData()

        loadAdvancement(
            ROOT_KEY,
            advancementJson(
                title = plain(Texts.line("gui.progression.advancement-root-title")),
                description = plain(Texts.line("gui.progression.advancement-root-description")),
                icon = Material.MOSS_BLOCK,
                parent = null,
                showToast = false,
            ),
        )
        loadAdvancement(
            INTRO_KEY,
            advancementJson(
                title = plain(Texts.line("gui.progression.advancement-intro-title")),
                description = plain(Texts.line("gui.progression.advancement-intro-description")),
                icon = Material.WRITTEN_BOOK,
                parent = ROOT_KEY,
                announceToChat = true,
            ),
        )
        val playable = stages.filterNot { it.preview }
        playable.forEachIndexed { index, stage ->
            // Default to the previous chapter (linear chain); an explicit advancement-parent forks
            // the tree (e.g. a 日 and a 月 chapter both under the same earlier chapter = a branch).
            val parent = stage.advancementParent?.let { key(it) }
                ?: if (index == 0) INTRO_KEY else key(playable[index - 1].id)
            loadAdvancement(
                key(stage.id),
                advancementJson(
                    title = plain(stage.title),
                    description = plain((stage.details.ifEmpty { stage.lore }).joinToString(" ")),
                    icon = stage.icon,
                    parent = parent,
                    announceToChat = true,
                ),
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun loadAdvancement(key: NamespacedKey, json: String) {
        if (Bukkit.getAdvancement(key) != null) return
        runCatching { Bukkit.getUnsafe().loadAdvancement(key, json) }
            .onFailure { plugin.logger.warning("Could not load advancement '$key': ${it.message}") }
    }

    private fun award(player: Player, key: NamespacedKey, complete: Boolean, sound: String? = null) {
        val advancement = Bukkit.getAdvancement(key) ?: return
        val progress = player.getAdvancementProgress(advancement)
        val wasDone = progress.isDone
        if (complete) {
            progress.remainingCriteria.forEach(progress::awardCriteria)
        } else {
            progress.awardedCriteria.forEach(progress::revokeCriteria)
        }
        if (complete && !wasDone && sound != null) player.playSound(player.location, sound, 0.8f, 1.0f)
    }

    private fun advancementJson(
        title: String,
        description: String,
        icon: Material,
        parent: NamespacedKey?,
        showToast: Boolean = true,
        announceToChat: Boolean = false,
    ): String = JsonObject().apply {
        parent?.let { addProperty("parent", it.toString()) }
        add("display", JsonObject().apply {
            add("icon", JsonObject().apply { addProperty("id", icon.key.toString()) })
            addProperty("title", title)
            addProperty("description", description)
            addProperty("frame", "task")
            addProperty("show_toast", showToast)
            addProperty("announce_to_chat", announceToChat)
            addProperty("hidden", false)
            if (parent == null) addProperty("background", "minecraft:gui/advancements/backgrounds/husbandry")
        })
        add("criteria", JsonObject().apply {
            add("complete", JsonObject().apply { addProperty("trigger", "minecraft:impossible") })
        })
    }.toString()

    private fun plain(text: String): String =
        com.tinyyana.lycohism.util.Messages.format(text)
            .replace(Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE), "")

    private fun key(id: String) = NamespacedKey(plugin, "progression/$id")

    private companion object {
        const val FILE_NAME = "progression.yml"
        // Four inner rows (avoiding the bordered edges) read as a left-to-right road; the larger
        // 54-slot menu fits the growing chapter list (energy line + day/moon branch).
        val TRACK_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
        )
        val LOCKED_MATERIAL = Material.PAPER
        val ROOT_KEY = NamespacedKey("lycohism", "progression/root")
        val INTRO_KEY = NamespacedKey("lycohism", "progression/introduction")
    }
}
