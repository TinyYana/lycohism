package com.tinyyana.lycohism.command

import com.tinyyana.lycohism.Lycohism
import com.tinyyana.lycohism.multiblock.Rotation
import com.tinyyana.lycohism.util.Items
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import org.bukkit.block.BlockFace
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

/**
 * Root command for Lycohism. Dispatches simple subcommands; gameplay subcommands
 * are registered here as later milestones add content.
 */
class LycohismCommand(private val plugin: Lycohism) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.isEmpty()) {
            Messages.send(sender, Texts.render("commands.root", "version" to plugin.pluginMeta.version, "label" to label))
            return true
        }

        when (args[0].lowercase()) {
            "help" -> sendHelp(sender, label)
            "version" -> Messages.send(sender, Texts.render("commands.version", "version" to plugin.pluginMeta.version))
            "reload" -> {
                if (!requireAdmin(sender)) return true
                plugin.reload()
                Messages.send(sender, Texts.line("commands.reloaded"))
            }
            "debug" -> {
                if (!requireAdmin(sender)) return true
                val state = Texts.line(if (plugin.debug) "terms.enabled" else "terms.disabled")
                Messages.send(sender, Texts.render("commands.debug", "state" to state))
            }
            "workshop" -> {
                val player = sender as? Player ?: run {
                    Messages.send(sender, Texts.line("commands.player-only"))
                    return true
                }
                plugin.workshop.open(player)
            }
            "study" -> {
                val player = sender as? Player ?: run {
                    Messages.send(sender, Texts.line("commands.player-only"))
                    return true
                }
                plugin.study.open(player)
            }
            "greenhouse" -> {
                val player = sender as? Player ?: run {
                    Messages.send(sender, Texts.line("commands.player-only"))
                    return true
                }
                plugin.greenhouse.open(player)
            }
            "progress" -> {
                val player = sender as? Player ?: run {
                    Messages.send(sender, Texts.line("commands.player-only"))
                    return true
                }
                plugin.progressionManager.open(player)
            }
            "expedition" -> {
                if (!requireAdmin(sender)) return true
                val player = sender as? Player ?: run {
                    Messages.send(sender, Texts.line("commands.player-only"))
                    return true
                }
                plugin.expeditionManager.useGate(player, bypassUnlock = true)
            }
            "admin" -> {
                if (!requireAdmin(sender)) return true
                val player = sender as? Player ?: run {
                    Messages.send(sender, Texts.line("commands.player-only"))
                    return true
                }
                plugin.adminPanel.open(player)
            }
            "stage" -> handleStage(sender, label, args)
            "give" -> handleGive(sender, label, args)
            "build" -> handleBuild(sender, label, args)
            "blueprint" -> handleBlueprint(sender, label, args)
            "locate" -> handleLocate(sender, label, args)
            "nexus" -> handleNexus(sender, label, args)
            "upgrade" -> handleUpgrade(sender, label, args)
            else -> Messages.send(sender, Texts.render("commands.unknown", "label" to label))
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size == 1) {
            val prefix = args[0].lowercase()
            return SUBCOMMANDS.filter { it.startsWith(prefix) }
        }
        if (args.size == 2 && args[0].equals("give", ignoreCase = true)) {
            val prefix = args[1].lowercase()
            return plugin.adminPanel.itemIds.filter { it.startsWith(prefix) }
        }
        if (args.size == 2 && args[0].equals("stage", ignoreCase = true)) {
            val prefix = args[1].lowercase()
            return plugin.progressionManager.stageIds().filter { it.startsWith(prefix) }
        }
        if (args.size == 2 && args[0].equals("build", ignoreCase = true)) {
            val prefix = args[1].lowercase()
            return plugin.multiblockRegistry.ids().filter { it.startsWith(prefix) }
        }
        if (args.size == 3 && args[0].equals("build", ignoreCase = true)) {
            return listOf("ghost").filter { it.startsWith(args[2].lowercase()) }
        }
        if (args.size == 2 && args[0].equals("blueprint", ignoreCase = true)) {
            return plugin.multiblockRegistry.ids().filter { it.startsWith(args[1].lowercase()) }
        }
        if (args.size == 2 && args[0].equals("locate", ignoreCase = true)) {
            return plugin.structureLocator.ids().filter { it.startsWith(args[1].lowercase()) }
        }
        if (args.size == 2 && args[0].equals("nexus", ignoreCase = true)) {
            return listOf("share", "unshare").filter { it.startsWith(args[1].lowercase()) }
        }
        if (args.size == 3 && args[0].equals("nexus", ignoreCase = true)) {
            return plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
        }
        if (args.size == 2 && args[0].equals("upgrade", ignoreCase = true)) {
            return com.tinyyana.lycohism.facility.FacilityUpgrade.FACILITIES.filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }

    private fun handleStage(sender: CommandSender, label: String, args: Array<out String>) {
        if (!requireAdmin(sender)) return
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        val stage = args.getOrNull(1)?.lowercase()
        if (stage == null) {
            Messages.send(sender, Texts.render("commands.stage-usage", "label" to label))
            Messages.send(sender, Texts.render("commands.stage-list", "stages" to plugin.progressionManager.stageIds().joinToString(Texts.line("terms.list-separator"))))
            return
        }
        if (!plugin.progressionManager.setCurrentStage(player, stage)) {
            Messages.send(sender, Texts.render("commands.stage-unknown", "stage" to stage))
            return
        }
        Messages.send(sender, Texts.render("commands.stage-done", "stage" to stage))
        plugin.progressionManager.open(player)
    }

    private fun handleGive(sender: CommandSender, label: String, args: Array<out String>) {
        if (!requireAdmin(sender)) return
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        if (args.size < 2) {
            Messages.send(sender, Texts.render("commands.give-usage", "label" to label))
            Messages.send(sender, Texts.render("commands.give-items", "items" to plugin.adminPanel.itemIds.joinToString(Texts.line("terms.list-separator"))))
            return
        }
        val amount = args.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 64) ?: 1
        val item = plugin.adminPanel.buildItem(args[1].lowercase(), amount) ?: run {
            Messages.send(sender, Texts.render("commands.give-unknown", "item" to args[1]))
            return
        }
        Items.give(player, item)
        com.tinyyana.lycohism.util.Audit.log(player, "command-give", "${args[1].lowercase()} x$amount")
        Messages.send(sender, Texts.render("commands.give-done", "item" to Texts.line("content-names.${args[1].lowercase()}", args[1]), "amount" to amount.toString()))
    }

    /** Admin: hand out a 藍圖 for a multiblock structure (players build it via right-click). */
    private fun handleBlueprint(sender: CommandSender, label: String, args: Array<out String>) {
        if (!requireAdmin(sender)) return
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        val id = args.getOrNull(1)?.lowercase()
        if (id == null || plugin.multiblockRegistry.get(id) == null) {
            Messages.send(sender, Texts.render("commands.build-items", "items" to plugin.multiblockRegistry.ids().joinToString(Texts.line("terms.list-separator"))))
            return
        }
        Items.give(player, plugin.blueprint.createItem(id))
        com.tinyyana.lycohism.util.Audit.log(player, "blueprint-give", id)
        Messages.send(sender, Texts.render("commands.build-done", "id" to id))
    }

    /** Player: upgrade a facility to level 2, spending materials and the nearby nexus's 輝能. */
    private fun handleUpgrade(sender: CommandSender, label: String, args: Array<out String>) {
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        val facility = args.getOrNull(1)?.lowercase()
        if (facility == null || facility !in com.tinyyana.lycohism.facility.FacilityUpgrade.FACILITIES) {
            Messages.send(sender, Texts.render("messages.upgrade.usage", "label" to label))
            return
        }
        val result = com.tinyyana.lycohism.facility.FacilityUpgrade.upgrade(plugin, player, facility)
        val key = when (result) {
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.SUCCESS -> "messages.upgrade.done"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.NOT_REPAIRED -> "messages.upgrade.not-repaired"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.ALREADY_MAX -> "messages.upgrade.already"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.NO_STRUCTURE -> "messages.upgrade.no-structure"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.NO_NEXUS -> "messages.upgrade.no-nexus"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.MISSING_MATERIALS -> "messages.upgrade.missing-materials"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.MISSING_ENERGY -> "messages.upgrade.missing-energy"
            com.tinyyana.lycohism.facility.FacilityUpgrade.Result.UNKNOWN -> "messages.upgrade.usage"
        }
        Messages.send(sender, Texts.render(key, "label" to label, "facility" to facility))
    }

    /** Player: share/unshare the nearest 輝能核心 they own with another (online) player. */
    private fun handleNexus(sender: CommandSender, label: String, args: Array<out String>) {
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        val action = args.getOrNull(1)?.lowercase()
        val targetName = args.getOrNull(2)
        if ((action != "share" && action != "unshare") || targetName == null) {
            Messages.send(sender, Texts.render("messages.nexus.share-usage", "label" to label))
            return
        }
        val nexus = plugin.nexusManager.ownedNexusNear(player) ?: run {
            Messages.send(sender, Texts.line("messages.nexus.no-owned"))
            return
        }
        val target = plugin.server.getPlayerExact(targetName) ?: run {
            Messages.send(sender, Texts.render("messages.nexus.player-not-found", "player" to targetName))
            return
        }
        val changed = if (action == "share") plugin.nexusManager.share(nexus, target.uniqueId) else plugin.nexusManager.unshare(nexus, target.uniqueId)
        if (!changed) {
            Messages.send(sender, Texts.render("messages.nexus.share-noop", "state" to Texts.line(if (action == "share") "terms.shared" else "terms.not-shared")))
            return
        }
        Messages.send(sender, Texts.render("messages.nexus.${if (action == "share") "shared" else "unshared"}", "player" to target.name))
    }

    /** Admin: instant-stamp (or ghost-preview) a multiblock template where the player is looking. */
    private fun handleBuild(sender: CommandSender, label: String, args: Array<out String>) {
        if (!requireAdmin(sender)) return
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        if (args.size < 2) {
            Messages.send(sender, Texts.render("commands.build-usage", "label" to label))
            Messages.send(sender, Texts.render("commands.build-items", "items" to plugin.multiblockRegistry.ids().joinToString(Texts.line("terms.list-separator"))))
            return
        }
        val multiblock = plugin.multiblockRegistry.get(args[1].lowercase()) ?: run {
            Messages.send(sender, Texts.render("commands.build-unknown", "id" to args[1]))
            return
        }
        val target = player.getTargetBlockExact(6) ?: player.location.block.getRelative(BlockFace.DOWN)
        val world = target.world
        if (args.getOrNull(2)?.equals("ghost", ignoreCase = true) == true) {
            object : BukkitRunnable() {
                private var elapsed = 0L
                override fun run() {
                    if (elapsed >= GHOST_DURATION_TICKS || !player.isOnline) {
                        cancel()
                        return
                    }
                    multiblock.showGhost(plugin, world, target.x, target.y, target.z, Rotation.NONE, player)
                    elapsed += GHOST_INTERVAL_TICKS
                }
            }.runTaskTimer(plugin, 0L, GHOST_INTERVAL_TICKS)
            Messages.send(sender, Texts.render("commands.build-ghost", "id" to multiblock.id))
        } else {
            multiblock.place(world, target.x, target.y, target.z, Rotation.NONE)
            plugin.structureLocator.record(multiblock.id, target.location)
            // Placing also activates it (registers the tower/relay/nexus and floats its label), so a
            // /build tower actually produces — previously it only stamped blocks and stayed inert.
            com.tinyyana.lycohism.multiblock.StructureActivation.activate(plugin, player, multiblock.id, world.getBlockAt(target.x, target.y, target.z))
            Messages.send(sender, Texts.render("commands.build-done", "id" to multiblock.id))
        }
    }

    private fun handleLocate(sender: CommandSender, label: String, args: Array<out String>) {
        if (!requireAdmin(sender)) return
        val player = sender as? Player ?: run {
            Messages.send(sender, Texts.line("commands.player-only"))
            return
        }
        val id = args.getOrNull(1)?.lowercase()
        if (id == null || id !in plugin.structureLocator.ids()) {
            Messages.send(sender, Texts.render("commands.locate-usage", "label" to label, "items" to plugin.structureLocator.ids().joinToString(Texts.line("terms.list-separator"))))
            return
        }
        val location = plugin.structureLocator.nearest(id, player)
        if (location == null) {
            Messages.send(sender, Texts.render("commands.locate-none", "id" to id))
            return
        }
        player.compassTarget = location
        Messages.send(sender, Texts.render("commands.locate-done", "id" to id, "x" to location.blockX.toString(), "y" to location.blockY.toString(), "z" to location.blockZ.toString()))
    }

    private fun sendHelp(sender: CommandSender, label: String) {
        Texts.renderLines("commands.help.player", "label" to label).forEach { Messages.send(sender, it) }
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            Texts.renderLines("commands.help.admin", "label" to label).forEach { Messages.send(sender, it) }
        }
    }

    private fun requireAdmin(sender: CommandSender): Boolean {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            Messages.send(sender, Texts.line("commands.no-permission"))
            return false
        }
        return true
    }

    companion object {
        private const val PERMISSION_ADMIN = "lycohism.admin"
        private const val GHOST_INTERVAL_TICKS = 10L
        private const val GHOST_DURATION_TICKS = 300L
        private val SUBCOMMANDS = listOf("help", "version", "workshop", "study", "greenhouse", "progress", "nexus", "upgrade", "admin", "expedition", "stage", "reload", "debug", "give", "build", "blueprint", "locate")
    }
}
