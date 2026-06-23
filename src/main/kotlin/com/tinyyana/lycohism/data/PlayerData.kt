package com.tinyyana.lycohism.data

import java.util.UUID

/**
 * In-memory progression state for a single player.
 *
 * v0.1 skeleton keeps only a generic discovery set; concrete progression fields
 * (collected 自然現象, unlocked tools, facility levels …) are added as content lands.
 */
class PlayerData(val uuid: UUID) {

    /** Ids of things the player has discovered (自然現象、工具、設施里程碑 … ). */
    val discoveries: MutableSet<String> = mutableSetOf()

    /** Legacy vanilla-material history retained when loading older player files. */
    val knownMaterials: MutableSet<String> = mutableSetOf()

    /** 工房 level. 0 = not yet repaired; 1 = repaired (v0.1 cap). */
    var workshopLevel: Int = 0

    /** 書房 level. 0 = not repaired; 1 = record features available. */
    var studyLevel: Int = 0

    /** 溫室 level. 0 = not reclaimed; 1 = cultivation + plant tools available. */
    var greenhouseLevel: Int = 0

    /** Warn before Lycohism materials are consumed by a vanilla crafting recipe. */
    var confirmVanillaCrafts: Boolean = true

    /** Warn before breaking a block that belongs to a complete Lycohism multiblock. */
    var confirmStructureBreaks: Boolean = true

    /** 日輝 pool: radiant energy gathered under a daytime sky, spent by sun-attuned abilities. */
    var sunEnergy: Int = 0

    /** 月輝 pool: radiant energy gathered under a night sky, spent by moon-attuned abilities. */
    var moonEnergy: Int = 0

    /** Coordinate keys ("world,x,y,z") of sealed shrines this player has already unlocked. */
    val unsealed: MutableSet<String> = mutableSetOf()

    fun hasDiscovered(id: String): Boolean = id in discoveries
}
