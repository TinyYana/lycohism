package com.tinyyana.lycohism

import com.tinyyana.lycohism.admin.AdminPanel
import com.tinyyana.lycohism.command.LycohismCommand
import com.tinyyana.lycohism.data.PlayerDataManager
import com.tinyyana.lycohism.energy.EnergyManager
import com.tinyyana.lycohism.energy.EnergyService
import com.tinyyana.lycohism.energy.EnergyTowers
import com.tinyyana.lycohism.energy.NexusManager
import com.tinyyana.lycohism.expedition.ExpeditionHazards
import com.tinyyana.lycohism.expedition.ExpeditionManager
import com.tinyyana.lycohism.expedition.RainGate
import com.tinyyana.lycohism.facility.Greenhouse
import com.tinyyana.lycohism.facility.Workshop
import com.tinyyana.lycohism.facility.Study
import com.tinyyana.lycohism.facility.StudyBooks
import com.tinyyana.lycohism.listener.FacilityAccessListener
import com.tinyyana.lycohism.listener.MobDropListener
import com.tinyyana.lycohism.listener.MoonPouchListener
import com.tinyyana.lycohism.listener.GreenhouseListener
import com.tinyyana.lycohism.listener.NaturePhenomenonListener
import com.tinyyana.lycohism.listener.PlayerListener
import com.tinyyana.lycohism.listener.ProgressionListener
import com.tinyyana.lycohism.listener.StudyListener
import com.tinyyana.lycohism.listener.ToolUseListener
import com.tinyyana.lycohism.listener.VanillaCraftGuardListener
import com.tinyyana.lycohism.listener.WorkshopListener
import com.tinyyana.lycohism.phenomenon.PhenomenonManager
import com.tinyyana.lycohism.phenomenon.RainTending
import com.tinyyana.lycohism.multiblock.AltarManager
import com.tinyyana.lycohism.multiblock.MultiblockRegistry
import com.tinyyana.lycohism.progression.ProgressionManager
import com.tinyyana.lycohism.tool.Blueprint
import com.tinyyana.lycohism.tool.DewLight
import com.tinyyana.lycohism.tool.EnergyCrystal
import com.tinyyana.lycohism.tool.FlowerBookmark
import com.tinyyana.lycohism.tool.FlowerVeinShears
import com.tinyyana.lycohism.tool.HeldToolStatusBar
import com.tinyyana.lycohism.tool.LeylineProbe
import com.tinyyana.lycohism.tool.MossBalm
import com.tinyyana.lycohism.tool.LunarSpore
import com.tinyyana.lycohism.tool.MossFertile
import com.tinyyana.lycohism.tool.RadiantFocus
import com.tinyyana.lycohism.tool.RainBandage
import com.tinyyana.lycohism.tool.SolarPick
import com.tinyyana.lycohism.tool.StarCompass
import com.tinyyana.lycohism.tool.StoneworkHammer
import com.tinyyana.lycohism.tool.TuningManual
import com.tinyyana.lycohism.tool.MoonPouch
import com.tinyyana.lycohism.tool.WindVane
import com.tinyyana.lycohism.util.Keys
import com.tinyyana.lycohism.util.Messages
import com.tinyyana.lycohism.util.Texts
import com.tinyyana.lycohism.world.StructureGenerator
import org.bukkit.plugin.java.JavaPlugin

/**
 * Lycohism core plugin entry point.
 *
 * Keeps lifecycle, config loading, command wiring, event registration and player
 * storage in one place while gameplay behaviour remains in focused modules.
 */
class Lycohism : JavaPlugin() {

    lateinit var playerDataManager: PlayerDataManager
        private set

    lateinit var phenomenonManager: PhenomenonManager
        private set

    lateinit var energyManager: EnergyManager
        private set

    lateinit var energyTowers: EnergyTowers
        private set

    lateinit var energyService: EnergyService
        private set

    lateinit var nexusManager: NexusManager
        private set

    lateinit var energyCrystal: EnergyCrystal
        private set

    lateinit var radiantFocus: RadiantFocus
        private set

    lateinit var solarPick: SolarPick
        private set

    lateinit var starCompass: StarCompass
        private set

    lateinit var lunarSpore: LunarSpore
        private set

    lateinit var dewLight: DewLight
        private set

    lateinit var flowerBookmark: FlowerBookmark
        private set

    lateinit var rainBandage: RainBandage
        private set

    lateinit var rainTending: RainTending
        private set

    lateinit var stoneworkHammer: StoneworkHammer
        private set

    lateinit var flowerVeinShears: FlowerVeinShears
        private set

    lateinit var tuningManual: TuningManual
        private set

    lateinit var windVane: WindVane
        private set

    lateinit var moonPouch: MoonPouch
        private set

    lateinit var leylineProbe: LeylineProbe
        private set

    lateinit var mossBalm: MossBalm
        private set

    lateinit var mossFertile: MossFertile
        private set

    lateinit var heldToolStatusBar: HeldToolStatusBar
        private set

    lateinit var workshop: Workshop
        private set

    lateinit var studyBooks: StudyBooks
        private set

    lateinit var study: Study
        private set

    lateinit var greenhouse: Greenhouse
        private set

    lateinit var expeditionManager: ExpeditionManager
        private set

    lateinit var progressionManager: ProgressionManager
        private set

    lateinit var multiblockRegistry: MultiblockRegistry
        private set

    lateinit var blueprint: Blueprint
        private set

    lateinit var altarManager: AltarManager
        private set

    lateinit var expeditionHazards: ExpeditionHazards
        private set

    lateinit var rainGate: RainGate
        private set

    lateinit var adminPanel: AdminPanel
        private set

    lateinit var structureGenerator: StructureGenerator
        private set

    private lateinit var facilityAccessListener: FacilityAccessListener

    /** Verbose console logging toggle, driven by config.yml. */
    var debug: Boolean = false
        private set

    override fun onEnable() {
        instance = this

        Keys.init(this)
        saveDefaultConfig()
        loadSettings()
        Texts.load(this)
        com.tinyyana.lycohism.util.Audit.init(this)

        playerDataManager = PlayerDataManager(this)
        phenomenonManager = PhenomenonManager(this)
        energyManager = EnergyManager(this)
        energyTowers = EnergyTowers(this)
        nexusManager = NexusManager(this)
        energyService = EnergyService(this)
        energyCrystal = EnergyCrystal(this)
        radiantFocus = RadiantFocus(this)
        solarPick = SolarPick(this)
        starCompass = StarCompass(this)
        lunarSpore = LunarSpore(this)
        dewLight = DewLight(this)
        flowerBookmark = FlowerBookmark(this)
        rainBandage = RainBandage(this)
        rainTending = RainTending(this)
        stoneworkHammer = StoneworkHammer(this)
        flowerVeinShears = FlowerVeinShears(this)
        tuningManual = TuningManual(this)
        windVane = WindVane(this)
        moonPouch = MoonPouch(this)
        leylineProbe = LeylineProbe(this)
        mossBalm = MossBalm(this)
        mossFertile = MossFertile(this)
        heldToolStatusBar = HeldToolStatusBar(this)
        workshop = Workshop(this)
        studyBooks = StudyBooks(this)
        study = Study(this)
        greenhouse = Greenhouse(this)
        expeditionManager = ExpeditionManager(this)
        expeditionHazards = ExpeditionHazards(this)
        rainGate = RainGate(this)
        progressionManager = ProgressionManager(this)
        multiblockRegistry = MultiblockRegistry(this)
        blueprint = Blueprint(this)
        altarManager = AltarManager(this)
        structureGenerator = StructureGenerator(this)
        dewLight.registerRecipe()
        adminPanel = AdminPanel(this)

        registerCommands()
        registerListeners()
        heldToolStatusBar.start()
        expeditionHazards.start()
        energyService.start()

        logger.info("Lycohism v${pluginMeta.version} enabled.")
    }

    override fun onDisable() {
        if (::heldToolStatusBar.isInitialized) {
            heldToolStatusBar.stop()
        }
        if (::expeditionHazards.isInitialized) {
            expeditionHazards.stop()
        }
        if (::energyService.isInitialized) {
            energyService.stop()
        }
        if (::nexusManager.isInitialized) {
            nexusManager.save()
        }
        if (::playerDataManager.isInitialized) {
            playerDataManager.saveAll()
        }
        com.tinyyana.lycohism.util.Audit.close()
        logger.info("Lycohism disabled.")
    }

    /** Re-read config.yml and apply settings without a full restart. */
    fun reload() {
        reloadConfig()
        loadSettings()
        Texts.load(this)
        com.tinyyana.lycohism.util.Audit.init(this)
        phenomenonManager.reload()
        energyManager.load()
        energyTowers.load()
        nexusManager.load()
        energyService.load()
        energyCrystal.load()
        radiantFocus.load()
        solarPick.load()
        starCompass.load()
        lunarSpore.load()
        dewLight.load()
        flowerBookmark.load()
        rainBandage.load()
        rainTending.load()
        stoneworkHammer.load()
        flowerVeinShears.load()
        windVane.load()
        moonPouch.load()
        leylineProbe.load()
        mossBalm.load()
        mossFertile.load()
        workshop.load()
        study.load()
        greenhouse.load()
        expeditionManager.reload()
        rainGate.load()
        progressionManager.reload()
        multiblockRegistry.load()
        altarManager.reload()
        structureGenerator.load()
        dewLight.registerRecipe()
        facilityAccessListener.load()
    }

    private fun loadSettings() {
        debug = config.getBoolean("debug", false)
        Messages.prefix = config.getString("prefix")
            ?: "<gradient:#f7a8b8:#ffd6e0>[Lycohism]</gradient> <reset>"
    }

    private fun registerCommands() {
        getCommand("lycohism")?.let { command ->
            val handler = LycohismCommand(this)
            command.setExecutor(handler)
            command.tabCompleter = handler
        } ?: logger.warning("Command 'lycohism' is not declared in plugin.yml.")
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerListener(this), this)
        server.pluginManager.registerEvents(NaturePhenomenonListener(this), this)
        server.pluginManager.registerEvents(MobDropListener(this), this)
        server.pluginManager.registerEvents(ToolUseListener(this), this)
        server.pluginManager.registerEvents(WorkshopListener(this), this)
        server.pluginManager.registerEvents(StudyListener(this), this)
        server.pluginManager.registerEvents(ProgressionListener(this), this)
        server.pluginManager.registerEvents(VanillaCraftGuardListener(this), this)
        server.pluginManager.registerEvents(GreenhouseListener(this), this)
        server.pluginManager.registerEvents(MoonPouchListener(this), this)
        server.pluginManager.registerEvents(adminPanel, this)
        server.pluginManager.registerEvents(structureGenerator, this)
        server.pluginManager.registerEvents(com.tinyyana.lycohism.listener.MultiblockListener(this), this)
        server.pluginManager.registerEvents(com.tinyyana.lycohism.listener.AltarListener(this), this)

        facilityAccessListener = FacilityAccessListener(this)
        server.pluginManager.registerEvents(facilityAccessListener, this)
    }

    /** Logs only when debug mode is enabled. Cheap to call from gameplay code. */
    fun debugLog(message: String) {
        if (debug) logger.info("[DEBUG] $message")
    }

    companion object {
        lateinit var instance: Lycohism
            private set
    }
}
