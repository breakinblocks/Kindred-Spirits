package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.config.KindredConfig;

public final class CompanionBondMath {
    public static final int PASSIVE_INTERVAL_TICKS = 20;
    public static final int PASSIVE_POINTS = 1;
    public static final double PASSIVE_RANGE = 32.0;
    public static final int MIN_FEED_POINTS = 60;
    public static final int FAST_RATE_LEVEL = 15;
    public static final double GUARD_RANGE = 16.0;
    public static final double GUARD_ARMOUR = 2.0;
    private static final int LEGACY_MAX = 100;

    public static int maxLevel() {
        return KindredConfig.COMMON.bondMaxLevel.get();
    }

    public static int costForLevel(int level) {
        if (level < 1) {
            return 0;
        }

        // Reserve room for every configured level, even at extreme growth rates.
        double cost = KindredConfig.COMMON.bondBaseCost.get()
                * Math.pow(KindredConfig.COMMON.bondGrowth.get(), level - 1);
        return (int) Math.clamp(Math.round(cost), 1L, Integer.MAX_VALUE / maxLevel());
    }

    public static int pointsAtLevelStart(int level) {
        int total = 0;
        for (int i = 1; i <= Math.min(level, maxLevel()); i++) {
            total += costForLevel(i);
        }
        return total;
    }

    public static int totalPoints() {
        return pointsAtLevelStart(maxLevel());
    }

    public static int clampPoints(int points) {
        return Math.clamp(points, 0, totalPoints());
    }

    public static int levelFromPoints(int points) {
        int level = 0;
        int spent = 0;

        while (level < maxLevel() && points >= spent + costForLevel(level + 1)) {
            spent += costForLevel(level + 1);
            level++;
        }

        return level;
    }

    public static int pointsIntoLevel(int points) {
        return clampPoints(points) - pointsAtLevelStart(levelFromPoints(points));
    }

    public static int costToNext(int points) {
        int level = levelFromPoints(points);
        return level >= maxLevel() ? costForLevel(maxLevel()) : costForLevel(level + 1);
    }

    public static double fraction(int level) {
        return Math.clamp(level / (double) maxLevel(), 0.0, 1.0);
    }

    public static int feedPoints(int points) {
        return Math.max(MIN_FEED_POINTS, (int) Math.round(points * KindredConfig.COMMON.bondFeedFraction.get()));
    }

    public static int feedCooldownTicks() {
        return KindredConfig.COMMON.bondFeedCooldownSeconds.get() * 20;
    }

    public static int afterDeath(int points) {
        int level = levelFromPoints(points);
        return level > KindredConfig.COMMON.bondDeathFloor.get() ? pointsAtLevelStart(level - 1) : points;
    }

    public static int tier(int level) {
        if (level >= maxLevel()) {
            return 2;
        }
        return level >= FAST_RATE_LEVEL ? 1 : 0;
    }

    public static double rateMultiplier(int level) {
        return 1.0 / (1 << tier(level));
    }

    public static double experienceMultiplier(int level) {
        return 1.0 + 0.5 * fraction(level);
    }

    public static int reviveCooldownTicks(int level) {
        return (int) Math.round(KindredConfig.COMMON.reviveCooldownSeconds.get() * 20 * (1.0 - 0.5 * fraction(level)));
    }

    public static boolean grantsGuard(int level) {
        return level >= maxLevel();
    }

    public static int storageSlots(CompanionSpecies species, int level) {
        int max = species.storageSlots();
        int unlocked = (int) Math.ceil(max * fraction(level));
        return Math.clamp(Math.max(KindredConfig.COMMON.storageBaseSlots.get(), unlocked), 0, max);
    }

    public static int migrateLegacy(int legacyBond) {
        return (int) Math.round(totalPoints() * (Math.clamp(legacyBond, 0, LEGACY_MAX) / (double) LEGACY_MAX));
    }

    public static int afkTicks() {
        return KindredConfig.COMMON.afkSeconds.get() * 20;
    }

    private CompanionBondMath() {
    }
}
