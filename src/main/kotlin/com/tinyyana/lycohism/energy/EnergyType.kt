package com.tinyyana.lycohism.energy

/**
 * The two attunements of 輝能 (radiant energy), TinyYana's v0.6 "日與月" decision. 日輝 charges
 * under an open daytime sky; 月輝 under an open night sky. Different abilities draw on different
 * energies, and the permanent-night expedition is the practical place to farm 月輝.
 */
enum class EnergyType(val id: String, val chargesByDay: Boolean) {
    SUN("sun", true),
    MOON("moon", false);

    /** lang.yml key for this energy's short label. */
    val labelPath: String get() = "energy.$id"
}
