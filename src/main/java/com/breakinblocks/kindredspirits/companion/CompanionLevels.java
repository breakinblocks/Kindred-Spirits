package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.config.KindredConfig;

public final class CompanionLevels {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_STORAGE_ROWS = 3;
    public static final int MAX_STORAGE_SLOTS = MAX_STORAGE_ROWS * 9;

    public static int maxLevel() {
        return KindredConfig.COMMON.maxLevel.get();
    }

    public static int experienceToNext(int level) {
        int base = KindredConfig.COMMON.baseExperience.get();
        return base + (level - 1) * (base / 2);
    }

    public static double healthAt(CompanionSpecies species, int level) {
        return species.baseHealth()
                + (level - 1) * KindredConfig.COMMON.healthPerLevel.get() * species.growthMultiplier();
    }

    public static double attackDamageAt(CompanionSpecies species, int level) {
        return species.attackDamage()
                + (level - 1) * KindredConfig.COMMON.attackPerLevel.get() * species.growthMultiplier();
    }

    public static int bondCap() {
        return KindredConfig.COMMON.maxBond.get();
    }

    public static int storageSlots(CompanionSpecies species, int bond) {
        int max = species.storageSlots();
        int unlocked = Math.ceilDiv(max * Math.clamp(bond, 0, bondCap()), bondCap());
        return Math.clamp(Math.max(KindredConfig.COMMON.storageBaseSlots.get(), unlocked), 0, max);
    }

    private CompanionLevels() {
    }
}
