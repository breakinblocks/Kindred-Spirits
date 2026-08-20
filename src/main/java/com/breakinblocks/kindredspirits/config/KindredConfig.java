package com.breakinblocks.kindredspirits.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class KindredConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = common.getRight();
        COMMON = common.getLeft();

        Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = client.getRight();
        CLIENT = client.getLeft();
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
                    .comment("Extra max health added per level.")
                    .defineInRange("health_per_level", 1.0, 0.0, 20.0);
            attackPerLevel = builder
                    .comment("Extra attack damage added per level.")
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
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue showLevelInName;

        Client(ModConfigSpec.Builder builder) {
            builder.push("display");
            showLevelInName = builder
                    .comment("Append the companion's level to its name plate.")
                    .define("show_level_in_name", true);
            builder.pop();
        }
    }

    private KindredConfig() {
    }
}
