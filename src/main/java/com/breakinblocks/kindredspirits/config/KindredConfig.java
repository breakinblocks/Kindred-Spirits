package com.breakinblocks.kindredspirits.config;

import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class KindredConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    public static final ModConfigSpec STARTUP_SPEC;
    public static final Startup STARTUP;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = common.getRight();
        COMMON = common.getLeft();

        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = client.getRight();
        CLIENT = client.getLeft();

        Pair<Startup, ModConfigSpec> startup = new ModConfigSpec.Builder().configure(Startup::new);
        STARTUP_SPEC = startup.getRight();
        STARTUP = startup.getLeft();
    }

    public enum SpeciesStat {
        HEALTH,
        MOVE_SPEED,
        ATTACK_DAMAGE,
        ARMOUR,
        KNOCKBACK_RESISTANCE,
        FOLLOW_RANGE,
        GROWTH
    }

    public static double speciesStat(CompanionSpecies species, SpeciesStat stat, double fallback) {
        Startup.SpeciesStats stats = STARTUP.stats(species);

        if (stats == null || !STARTUP_SPEC.isLoaded()) {
            return fallback;
        }

        return switch (stat) {
            case HEALTH -> stats.health().get();
            case MOVE_SPEED -> stats.moveSpeed().get();
            case ATTACK_DAMAGE -> stats.attackDamage().get();
            case ARMOUR -> stats.armour().get();
            case KNOCKBACK_RESISTANCE -> stats.knockbackResistance().get();
            case FOLLOW_RANGE -> stats.followRange().get();
            case GROWTH -> stats.growth().get();
        };
    }

    public static final class Startup {
        private final Map<CompanionSpecies, SpeciesStats> species = new EnumMap<>(CompanionSpecies.class);

        Startup(ModConfigSpec.Builder builder) {
            builder.comment(
                            "Level 1 base stats for each companion species.",
                            "Attributes are built while the game loads, so changes need a restart.",
                            "These are not synced: a server and its clients should use the same values.")
                    .push("species");

            for (CompanionSpecies value : CompanionSpecies.values()) {
                CompanionSpecies.Stats defaults = value.defaults();

                builder.push(value.getSerializedName());
                this.species.put(
                        value,
                        new SpeciesStats(
                                builder.comment("Base max health at level 1.")
                                        .defineInRange("max_health", defaults.health(), 1.0, 1024.0),
                                builder.comment("Base movement speed. A vanilla wolf is 0.3.")
                                        .defineInRange("movement_speed", defaults.moveSpeed(), 0.0, 2.0),
                                builder.comment("Base attack damage at level 1.")
                                        .defineInRange("attack_damage", defaults.attackDamage(), 0.0, 1024.0),
                                builder.comment("Base armour.").defineInRange("armour", defaults.armour(), 0.0, 30.0),
                                builder.comment("Base knockback resistance, 0 to 1.")
                                        .defineInRange(
                                                "knockback_resistance", defaults.knockbackResistance(), 0.0, 1.0),
                                builder.comment(
                                                "How far it will notice something to fight, and how far its goals reach.")
                                        .defineInRange(
                                                "follow_range", CompanionSpecies.DEFAULT_FOLLOW_RANGE, 1.0, 128.0),
                                builder.comment(
                                                "Multiplier on the per-level health and attack gains from the common config.")
                                        .defineInRange("growth_multiplier", defaults.growth(), 0.0, 10.0)));
                builder.pop();
            }

            builder.pop();
        }

        public SpeciesStats stats(CompanionSpecies value) {
            return this.species.get(value);
        }

        public record SpeciesStats(
                ModConfigSpec.DoubleValue health,
                ModConfigSpec.DoubleValue moveSpeed,
                ModConfigSpec.DoubleValue attackDamage,
                ModConfigSpec.DoubleValue armour,
                ModConfigSpec.DoubleValue knockbackResistance,
                ModConfigSpec.DoubleValue followRange,
                ModConfigSpec.DoubleValue growth) {}
    }

    public static final class Common {
        public final ModConfigSpec.IntValue maxLevel;
        public final ModConfigSpec.IntValue baseExperience;
        public final ModConfigSpec.IntValue experiencePerFeed;
        public final ModConfigSpec.DoubleValue healingPerItem;
        public final ModConfigSpec.DoubleValue bandageHealing;
        public final ModConfigSpec.IntValue storageBaseSlots;
        public final ModConfigSpec.DoubleValue healthPerLevel;
        public final ModConfigSpec.DoubleValue attackPerLevel;
        public final ModConfigSpec.DoubleValue armourPerLevel;
        public final ModConfigSpec.DoubleValue speedPerLevel;
        public final ModConfigSpec.DoubleValue xpShare;
        public final ModConfigSpec.IntValue saturationSoftCap;
        public final ModConfigSpec.IntValue saturationHardCap;
        public final ModConfigSpec.IntValue saturationResetChunks;
        public final ModConfigSpec.IntValue restedCap;
        public final ModConfigSpec.IntValue maxStars;
        public final ModConfigSpec.DoubleValue starGrowthBonus;
        public final ModConfigSpec.IntValue bondMaxLevel;
        public final ModConfigSpec.IntValue bondBaseCost;
        public final ModConfigSpec.DoubleValue bondGrowth;
        public final ModConfigSpec.IntValue bondFeedCooldownSeconds;
        public final ModConfigSpec.DoubleValue bondFeedFraction;
        public final ModConfigSpec.IntValue bondDeathFloor;
        public final ModConfigSpec.IntValue afkSeconds;
        public final ModConfigSpec.DoubleValue tamingChance;
        public final ModConfigSpec.BooleanValue beachSuspiciousSand;
        public final ModConfigSpec.BooleanValue abilitiesEnabled;
        public final ModConfigSpec.IntValue crushingMightDust;
        public final ModConfigSpec.ConfigValue<List<? extends String>> helpingHandRecipeBlacklist;
        public final ModConfigSpec.IntValue helpingHandReverseDepth;
        public final ModConfigSpec.IntValue reviveCooldownSeconds;
        public final ModConfigSpec.DoubleValue reviveExperiencePenalty;
        public final ModConfigSpec.BooleanValue mimicOwnerWeapon;
        public final ModConfigSpec.BooleanValue allowSkinChoice;

        Common(ModConfigSpec.Builder builder) {
            builder.push("progression");
            maxLevel = builder.comment("Highest level a companion can reach.").defineInRange("max_level", 30, 1, 200);
            baseExperience = builder.comment(
                            "Experience needed to go from level 1 to level 2. Each level adds half this again.")
                    .defineInRange("base_experience", 93, 1, 10000);
            experiencePerFeed = builder.comment("Experience granted when a companion is fed.")
                    .defineInRange("experience_per_feed", 4, 0, 1000);
            storageBaseSlots = builder.comment(
                            "Storage slots a companion grants at zero bond. The rest unlock as bond grows,",
                            "up to the species maximum.")
                    .defineInRange("storage_base_slots", 3, 0, 27);
            healingPerItem = builder.comment(
                            "Health restored when a companion is given an item from the companion_healing tag.")
                    .defineInRange("healing_per_item", 10.0, 0.0, 1024.0);
            bandageHealing = builder.comment(
                            "Health restored by a Spirit Bandage, which is also in the companion_healing tag.")
                    .defineInRange("bandage_healing", 6.0, 0.0, 1024.0);
            healthPerLevel = builder.comment("Extra max health added per level, before the species growth multiplier.")
                    .defineInRange("health_per_level", 1.0, 0.0, 20.0);
            attackPerLevel = builder.comment(
                            "Extra attack damage added per level, before the species growth multiplier.")
                    .defineInRange("attack_per_level", 0.25, 0.0, 20.0);
            armourPerLevel = builder.comment("Extra armour added per level, before the species growth multiplier.")
                    .defineInRange("armour_per_level", 0.1, 0.0, 5.0);
            speedPerLevel = builder.comment(
                            "Extra movement speed added per level, before the species growth multiplier.")
                    .defineInRange("speed_per_level", 0.002, 0.0, 0.1);
            xpShare = builder.comment(
                            "Share of the experience the owner earns that the bonded companion also receives.")
                    .defineInRange("xp_share", 0.5, 0.0, 10.0);
            saturationSoftCap = builder.comment(
                            "Companion experience gained recently before gains drop to half rate.",
                            "Saturation decays by one point a second.")
                    .defineInRange("saturation_soft_cap", 200, 0, 100000);
            saturationHardCap = builder.comment("Saturation at which gains drop to a tenth. Moving away clears it.")
                    .defineInRange("saturation_hard_cap", 500, 0, 100000);
            saturationResetChunks = builder.comment(
                            "Chunks the owner has to move from where saturation started to clear it.")
                    .defineInRange("saturation_reset_chunks", 3, 1, 64);
            restedCap = builder.comment(
                            "Most rested experience a companion can store. Rested experience builds after five",
                            "minutes without a gain and doubles gains until it is spent.")
                    .defineInRange("rested_cap", 1200, 0, 100000);
            maxStars = builder.comment("Stars a companion can earn by prestiging at max level.")
                    .defineInRange("max_stars", 5, 0, 10);
            starGrowthBonus = builder.comment("Extra per-level stat growth for each star, as a fraction.")
                    .defineInRange("star_growth_bonus", 0.1, 0.0, 5.0);
            builder.pop();

            builder.push("bond");
            bondMaxLevel = builder.comment("Highest bond level a companion can reach.")
                    .defineInRange("bond_max_level", 30, 1, 100);
            bondBaseCost = builder.comment(
                            "Bond points needed for bond level 1. A companion earns one point a second",
                            "while out with an active owner.")
                    .defineInRange("bond_base_cost", 760, 1, 1000000);
            bondGrowth = builder.comment("Multiplier on the point cost of each further bond level.")
                    .defineInRange("bond_growth", 1.06, 1.0, 3.0);
            bondFeedCooldownSeconds = builder.comment("Seconds between feeds of the species' taming item for bond.")
                    .defineInRange("bond_feed_cooldown_seconds", 600, 0, 86400);
            bondFeedFraction = builder.comment(
                            "Share of the bond points already earned that one taming item feed grants.")
                    .defineInRange("bond_feed_fraction", 0.05, 0.0, 1.0);
            bondDeathFloor = builder.comment("Bond level above which dying costs the companion a bond level.")
                    .defineInRange("bond_death_floor", 10, 0, 100);
            afkSeconds = builder.comment(
                            "Seconds without the owner moving or looking around before bond stops growing.")
                    .defineInRange("afk_seconds", 120, 1, 86400);
            builder.pop();

            builder.push("taming");
            tamingChance = builder.comment("Chance that one of the species' taming items tames a wild companion.")
                    .defineInRange("taming_chance", 0.33, 0.0, 1.0);
            builder.pop();

            builder.push("worldgen");
            beachSuspiciousSand = builder.comment(
                            "Scatter the odd block of suspicious sand into beach sand as new chunks generate. Brushing it can turn up a T-Rex Egg.")
                    .define("beach_suspicious_sand", true);
            builder.pop();

            builder.push("abilities");
            abilitiesEnabled = builder.comment(
                            "Run companion abilities. Turn off to leave companions as cosmetic pets.")
                    .define("abilities_enabled", true);
            crushingMightDust = builder.comment(
                            "Dust the T-Rex's Crushing Might makes from each raw ore or ore item it crushes (into c:dusts/<x>).")
                    .defineInRange("crushing_might_dust", 3, 1, 64);
            helpingHandRecipeBlacklist = builder.comment(
                            "Crafting recipe ids the Mini Player's Helping Hand never copies. An entry ending in * matches every id that starts with the rest, so \"somemod:*\" covers a whole mod and \"somemod:compress/*\" one folder.")
                    .defineListAllowEmpty(
                            "helping_hand_recipe_blacklist",
                            List.of(),
                            () -> "",
                            entry -> entry instanceof String text && !text.isBlank());
            helpingHandReverseDepth = builder.comment(
                            "How many recipe steps Helping Hand follows from a crafted item looking for a way back to what went into it. Any recipe chain that leads back (an ingot block to ingots and ingots to the block, say) is never copied. Recipes of every type count, smelting and stonecutting included.")
                    .defineInRange("helping_hand_reverse_depth", 4, 1, 16);
            builder.pop();

            builder.push("charm");
            reviveCooldownSeconds = builder.comment(
                            "Seconds a charm must rest before it can revive a companion that died.")
                    .defineInRange("revive_cooldown_seconds", 300, 0, 86400);
            reviveExperiencePenalty = builder.comment(
                            "Fraction of progress towards the next level lost when a companion is revived.")
                    .defineInRange("revive_experience_penalty", 0.5, 0.0, 1.0);
            mimicOwnerWeapon = builder.comment(
                            "Let a companion that carries weapons copy the weapon its owner is holding.",
                            "Turn off to leave it with its own default weapon.")
                    .define("mimic_owner_weapon", true);
            allowSkinChoice = builder.comment(
                            "Let players name the player skin a Mini Player wears.",
                            "Turn off to keep every Mini Player wearing its owner's skin.")
                    .define("allow_skin_choice", true);
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue showLevelInName;
        public final ModConfigSpec.BooleanValue resolveCompanionSkins;

        Client(ModConfigSpec.Builder builder) {
            builder.push("display");
            showLevelInName = builder.comment("Append the companion's level to its name plate.")
                    .define("show_level_in_name", true);
            resolveCompanionSkins = builder.comment(
                            "Look up real player skins for companions that wear one.",
                            "Turn off to dress every Mini Player in a default skin instead.")
                    .define("resolve_companion_skins", true);
            builder.pop();
        }
    }

    private KindredConfig() {}
}
