package com.tinyyana.lycohism.util

import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/**
 * Central registry of [NamespacedKey]s used for PersistentDataContainer tags.
 * Initialised once on enable so the rest of the plugin can reference stable keys.
 */
object Keys {

    /** Marks an item as a Lycohism custom item and stores its content id. */
    lateinit var itemId: NamespacedKey
        private set

    /** 花籤: world name + block coordinates of the remembered location. */
    lateinit var bookmarkWorld: NamespacedKey
        private set
    lateinit var bookmarkX: NamespacedKey
        private set
    lateinit var bookmarkY: NamespacedKey
        private set
    lateinit var bookmarkZ: NamespacedKey
        private set

    /** 月紗袋: compact id=count pairs for captured natural phenomena. */
    lateinit var pouchContents: NamespacedKey
        private set

    /** 遠征: the player's main-world origin, so the gate can return them home. */
    lateinit var expeditionOrigin: NamespacedKey
        private set

    /** 遠征: marks a mob as a Lycohism-spawned themed hazard, storing its expedition id. */
    lateinit var expeditionMob: NamespacedKey
        private set

    /** 蓄能晶: stored 日輝 / 月輝 charge held on the crystal item. */
    lateinit var crystalSun: NamespacedKey
        private set
    lateinit var crystalMoon: NamespacedKey
        private set

    /** 藍圖: the multiblock structure id this blueprint builds. */
    lateinit var blueprintTarget: NamespacedKey
        private set

    /** Building wand tier-2 placement mode stored on the item. */
    lateinit var wandMode: NamespacedKey
        private set

    /** Floating structure labels: structure id and controller coordinates. */
    lateinit var structureLabelAnchor: NamespacedKey
        private set

    /** v0.8 BOSS: marks an entity as the 蝕影守望者 (and tracks its phase). */
    lateinit var bossTag: NamespacedKey
        private set

    fun init(plugin: Plugin) {
        itemId = NamespacedKey(plugin, "item_id")
        bookmarkWorld = NamespacedKey(plugin, "bookmark_world")
        bookmarkX = NamespacedKey(plugin, "bookmark_x")
        bookmarkY = NamespacedKey(plugin, "bookmark_y")
        bookmarkZ = NamespacedKey(plugin, "bookmark_z")
        pouchContents = NamespacedKey(plugin, "pouch_contents")
        expeditionOrigin = NamespacedKey(plugin, "expedition_origin")
        expeditionMob = NamespacedKey(plugin, "expedition_mob")
        crystalSun = NamespacedKey(plugin, "crystal_sun")
        crystalMoon = NamespacedKey(plugin, "crystal_moon")
        blueprintTarget = NamespacedKey(plugin, "blueprint_target")
        wandMode = NamespacedKey(plugin, "wand_mode")
        structureLabelAnchor = NamespacedKey(plugin, "structure_label_anchor")
        bossTag = NamespacedKey(plugin, "boss_tag")
    }
}
