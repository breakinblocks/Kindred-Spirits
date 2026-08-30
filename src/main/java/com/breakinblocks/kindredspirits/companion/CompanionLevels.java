package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.List;

public final class CompanionLevels {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_STORAGE_ROWS = 3;
    public static final int MAX_STORAGE_SLOTS = MAX_STORAGE_ROWS * 9;

    public record AttributeBonus(Holder<Attribute> attribute, AttributeModifier modifier) {
    }

    private static final List<AttributeBonus> STAR_BONUSES = List.of(
            new AttributeBonus(Attributes.MAX_HEALTH, new AttributeModifier(KindredSpirits.id("star_1"),
                    0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            new AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KindredSpirits.id("star_2"),
                    0.1, AttributeModifier.Operation.ADD_VALUE)),
            new AttributeBonus(Attributes.MOVEMENT_SPEED, new AttributeModifier(KindredSpirits.id("star_3"),
                    0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            new AttributeBonus(Attributes.ARMOR, new AttributeModifier(KindredSpirits.id("star_4"),
                    2.0, AttributeModifier.Operation.ADD_VALUE)),
            new AttributeBonus(Attributes.ATTACK_DAMAGE, new AttributeModifier(KindredSpirits.id("star_5"),
                    0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)));

    public static int maxLevel() {
        return KindredConfig.COMMON.maxLevel.get();
    }

    public static int maxStars() {
        return KindredConfig.COMMON.maxStars.get();
    }

    public static int clampLevel(int level) {
        return Math.clamp(level, MIN_LEVEL, maxLevel());
    }

    public static int clampStars(int stars) {
        return Math.clamp(stars, 0, maxStars());
    }

    public static boolean canPrestige(int level, int stars) {
        return level >= maxLevel() && stars < maxStars();
    }

    public static int experienceToNext(int level) {
        int base = KindredConfig.COMMON.baseExperience.get();
        return base + (level - 1) * (base / 2);
    }

    public static double growth(CompanionSpecies species, int stars) {
        return species.growthMultiplier() * (1.0 + KindredConfig.COMMON.starGrowthBonus.get() * stars);
    }

    private static double statAt(double base, double perLevel, CompanionSpecies species, int level, int stars) {
        return base + (level - 1) * perLevel * growth(species, stars);
    }

    public static double healthAt(CompanionSpecies species, int level, int stars) {
        return statAt(species.baseHealth(), KindredConfig.COMMON.healthPerLevel.get(), species, level, stars);
    }

    public static double attackDamageAt(CompanionSpecies species, int level, int stars) {
        return statAt(species.attackDamage(), KindredConfig.COMMON.attackPerLevel.get(), species, level, stars);
    }

    public static double armourAt(CompanionSpecies species, int level, int stars) {
        return statAt(species.armour(), KindredConfig.COMMON.armourPerLevel.get(), species, level, stars);
    }

    public static double speedAt(CompanionSpecies species, int level, int stars) {
        return statAt(species.moveSpeed(), KindredConfig.COMMON.speedPerLevel.get(), species, level, stars);
    }

    public static void applyBaseStats(AttributeMap attributes, CompanionSpecies species, int level, int stars) {
        setBase(attributes, Attributes.MAX_HEALTH, healthAt(species, level, stars));
        setBase(attributes, Attributes.ATTACK_DAMAGE, attackDamageAt(species, level, stars));
        setBase(attributes, Attributes.ARMOR, armourAt(species, level, stars));
        setBase(attributes, Attributes.MOVEMENT_SPEED, speedAt(species, level, stars));
    }

    private static void setBase(AttributeMap attributes, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = attributes.getInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public static void applyBonuses(AttributeMap attributes, Collection<AttributeBonus> all,
                                    Collection<AttributeBonus> active, boolean permanent) {
        for (AttributeBonus bonus : all) {
            AttributeInstance instance = attributes.getInstance(bonus.attribute());
            if (instance != null) {
                instance.removeModifier(bonus.modifier().id());
            }
        }

        for (AttributeBonus bonus : active) {
            AttributeInstance instance = attributes.getInstance(bonus.attribute());
            if (instance == null) {
                continue;
            }
            if (permanent) {
                instance.addOrReplacePermanentModifier(bonus.modifier());
            } else {
                instance.addTransientModifier(bonus.modifier());
            }
        }
    }

    public static List<AttributeBonus> starBonuses() {
        return STAR_BONUSES;
    }

    public static List<AttributeBonus> starBonusesAt(int stars) {
        return STAR_BONUSES.subList(0, Math.clamp(stars, 0, STAR_BONUSES.size()));
    }

    public static int storageSlots(CompanionSpecies species, int bondLevel) {
        return CompanionBondMath.storageSlots(species, bondLevel);
    }

    private CompanionLevels() {
    }
}
