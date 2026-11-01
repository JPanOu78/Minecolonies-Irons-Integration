package com.minecoloniesirons.colony.equipment;

/**
 * Magic schools associated with Iron's Spells 'n Spellbooks robes and mage armor.
 * Unlocked starting at Research Tier 2 (Arcane Repository), granting school-specific buffs.
 */
public enum MageRobeSchool {
    NONE("None", 0),
    ARCANE("Arcane", 2),
    FIRE("Pyromancy", 3),
    ICE("Cryomancy", 3),
    LIGHTNING("Electromancy", 3),
    HOLY("Holy", 3),
    BLOOD("Blood", 4),
    EVOCATION("Evocation", 4),
    ENDER("Ender", 4),
    NETHERITE("Netherite Master", 5);

    private final String displayName;
    private final int minTier;

    MageRobeSchool(String displayName, int minTier) {
        this.displayName = displayName;
        this.minTier = minTier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinTier() {
        return minTier;
    }
}

