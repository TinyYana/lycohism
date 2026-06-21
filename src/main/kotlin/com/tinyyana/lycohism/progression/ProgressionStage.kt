package com.tinyyana.lycohism.progression

import org.bukkit.Material

/**
 * One chapter on the 調律之路 (the macro progression line). Where the 調律手冊 gives the next
 * micro-step, the progression line is the whole arc at a glance: what the player has walked
 * through, where they stand now, and that more is coming. Data-driven via progression.yml;
 * all player-visible text lives in lang.yml under progression.stages.<id>.
 */
data class ProgressionStage(
    val id: String,
    /** Icon shown for the chapter when it is reached. */
    val icon: Material,
    /** MiniMessage chapter title. */
    val title: String,
    /** Short description lines, shown once the chapter is reached. */
    val lore: List<String>,
    /** Concrete features and knowledge shown after this chapter is complete. */
    val details: List<String>,
    /** One-line "what to do now", shown only while this is the current chapter. */
    val hint: String,
    /** Discovery ids that must all be recorded for the chapter to count as done. */
    val requiredDiscoveries: Set<String>,
    /** Vanilla advancement keys that must all be complete. */
    val requiredAdvancements: Set<String>,
    /** Facility id -> minimum level required (workshop / study / greenhouse). */
    val requiredFacilities: Map<String, Int>,
    /** A teaser chapter that is never "done" — used to preview planned-but-unbuilt content. */
    val preview: Boolean,
    /**
     * Id of the chapter this one hangs under in the *vanilla advancement* tree, or null to follow
     * the previous chapter (the default linear chain). Set this to fork the tree — e.g. a 日 and a
     * 月 chapter both pointing at the same earlier chapter create a visible day/moon branch.
     */
    val advancementParent: String?,
)

/** A chapter's state for one player: walked through, standing here, ahead, or a future teaser. */
enum class StageStatus { DONE, CURRENT, LOCKED, UPCOMING }
