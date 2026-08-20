package com.breakinblocks.kindredspirits.config;

import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;

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
            builder.comment("Level 1 base stats for each companion species.",
                            "Attributes are built while the game loads, so changes need a restart.",
                            "These are not synced: a server and its clients should use the same values.")
                    .push("species");

            for (CompanionSpecies value : CompanionSpecies.values()) {
                CompanionSpecies.Stats defaults = value.defaults();

                builder.push(value.getSerializedName());
                this.species.put(value, new SpeciesStats(
                        builder.comment("Base max health at level 1.")
                                .defineInRange("max_health", defaults.health(), 1.0, 1024.0),
                        builder.comment("Base movement speed. A vanilla wolf is 0.3.")
                                .defineInRange("movement_speed", defaults.moveSpeed(), 0.0, 2.0),
                        builder.comment("Base attack damage at level 1.")
                                .defineInRange("attack_damage", defaults.attackDamage(), 0.0, 1024.0),
                        builder.comment("Base armour.")
                                .defineInRange("armour", defaults.armour(), 0.0, 30.0),
                        builder.comment("Base knockback resistance, 0 to 1.")
                                .defineInRange("knockback_resistance", defaults.knockbackResistance(), 0.0, 1.0),
                        builder.comment("How far it will notice something to fight, and how far its goals reach.")
                                .defineInRange("follow_range", CompanionSpecies.DEFAULT_FOLLOW_RANGE, 1.0, 128.0),
                        builder.comment("Multiplier on the per-level health and attack gains from the common config.")
                                .defineInRange("growth_multiplier", defaults.growth(), 0.0, 10.0)));
                builder.pop();
            }

            builder.pop();
        }

        public SpeciesStats stats(CompanionSpecies value) {
            return this.species.get(value);
        }

        public record SpeciesStats(ModConfigSpec.DoubleValue health,
                                   ModConfigSpec.DoubleValue moveSpeed,
                                   ModConfigSpec.DoubleValue attackDamage,
                                   ModConfigSpec.DoubleValue armour,
                                   ModConfigSpec.DoubleValue knockbackResistance,
                                   ModConfigSpec.DoubleValue followRange,
                                   ModConfigSpec.DoubleValue growth) {
        }
    }

    public static final class Common {
        public final ModConfigSpec.IntValue maxLevel;
        public final ModConfigSpec.IntValue baseExperience;
        public final ModConfigSpec.IntValue experiencePerFeed;
        public final ModConfigSpec.IntValue maxBond;
        public final ModConfigSpec.DoubleValue healthPerLevel;
        public final ModConfigSpec.DoubleValue attackPerLevel;
        public final ModConfigSpec.BooleanValue abilitiesEnabled;
        public final ModConfigSpec.IntValue reviveCooldownSeconds;
        public final ModConfigSpec.DoubleValue reviveExperiencePenalty;
        public final ModConfigSpec.BooleanValue mimicOwnerWeapon;
        public final ModConfigSpec.BooleanValue allowSkinChoice;

        Common(ModConfigSpec.Builder builder) {
            builder.push("progression");
            maxLevel = builder
                    .comment("Highest level a companion can reach.")
                    .defineInRange("max_level", 30, 1, 200);
            baseExperience = builder
                    .comment("Experience needed to go from level 1 to level 2. Each level adds half this again.")
                    .defineInRange("base_experience", 20, 1, 10000);
            experiencePerFeed = builder
                    .comment("Experience granted when a companion is fed.")
                    .defineInRange("experience_per_feed", 4, 0, 1000);
            maxBond = builder
                    .comment("Highest bond value a companion can reach.")
                    .defineInRange("max_bond", 100, 1, 1000);
            healthPerLevel = builder
                    .comment("Extra max health added per level, before the species growth multiplier.")
                    .defineInRange("health_per_level", 1.0, 0.0, 20.0);
            attackPerLevel = builder
                    .comment("Extra attack damage added per level, before the species growth multiplier.")
                    .defineInRange("attack_per_level", 0.25, 0.0, 20.0);
            builder.pop();

            builder.push("abilities");
            abilitiesEnabled = builder
                    .comment("Run companion abilities. Turn off to leave companions as cosmetic pets.")
                    .define("abilities_enabled", true);
            builder.pop();

            builder.push("charm");
            reviveCooldownSeconds = builder
                    .comment("Seconds a charm must rest before it can revive a companion that died.")
                    .defineInRange("revive_cooldown_seconds", 300, 0, 86400);
            reviveExperiencePenalty = builder
                    .comment("Fraction of progress towards the next level lost when a companion is revived.")
                    .defineInRange("revive_experience_penalty", 0.5, 0.0, 1.0);
            mimicOwnerWeapon = builder
                    .comment("Let a companion that carries weapons copy the weapon its owner is holding.",
                            "Turn off to leave it with its own default weapon.")
                    .define("mimic_owner_weapon", true);
            allowSkinChoice = builder
                    .comment("Let players name the player skin a Mini Player wears.",
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
            showLevelInName = builder
                    .comment("Append the companion's level to its name plate.")
                    .define("show_level_in_name", true);
            resolveCompanionSkins = builder
                    .comment("Look up real player skins for companions that wear one.",
                            "Turn off to dress every Mini Player in a default skin instead.")
                    .define("resolve_companion_skins", true);
            builder.pop();
        }
    }

    private KindredConfig() {
    }
}
