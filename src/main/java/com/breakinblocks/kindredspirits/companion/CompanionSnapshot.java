package com.breakinblocks.kindredspirits.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public record CompanionSnapshot(String species, int level, int experience, int bondPoints, int stars,
                                CompanionProgress progress, Optional<String> name,
                                int command, int aggression, Optional<String> skin, ItemStack equipment) {
    private static final int LEGACY_BOND = -1;

    public static final Codec<CompanionSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species").forGetter(CompanionSnapshot::species),
            Codec.INT.fieldOf("level").forGetter(CompanionSnapshot::level),
            Codec.INT.optionalFieldOf("experience", 0).forGetter(CompanionSnapshot::experience),
            Codec.INT.optionalFieldOf("bond_points", LEGACY_BOND).forGetter(CompanionSnapshot::bondPoints),
            Codec.INT.optionalFieldOf("bond", 0).forGetter(snapshot -> 0),
            Codec.INT.optionalFieldOf("stars", 0).forGetter(CompanionSnapshot::stars),
            CompanionProgress.CODEC.optionalFieldOf("progress", new CompanionProgress())
                    .forGetter(CompanionSnapshot::progress),
            Codec.STRING.optionalFieldOf("name").forGetter(CompanionSnapshot::name),
            Codec.INT.optionalFieldOf("command", CompanionCommand.FOLLOW.ordinal())
                    .forGetter(CompanionSnapshot::command),
            Codec.INT.optionalFieldOf("aggression", CompanionAggression.NEUTRAL.ordinal())
                    .forGetter(CompanionSnapshot::aggression),
            Codec.STRING.optionalFieldOf("skin").forGetter(CompanionSnapshot::skin),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("equipment", ItemStack.EMPTY).forGetter(CompanionSnapshot::equipment)
    ).apply(instance, CompanionSnapshot::fromSaved));

    private static CompanionSnapshot fromSaved(String species, int level, int experience, int bondPoints,
                                               int legacyBond, int stars, CompanionProgress progress,
                                               Optional<String> name, int command, int aggression,
                                               Optional<String> skin, ItemStack equipment) {
        int points = bondPoints == LEGACY_BOND ? CompanionBondMath.migrateLegacy(legacyBond) : bondPoints;
        return new CompanionSnapshot(species, level, experience, points, stars, progress,
                name, command, aggression, skin, equipment);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CompanionSnapshot::species,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::level,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::experience,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::bondPoints,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::stars,
            CompanionProgress.STREAM_CODEC, CompanionSnapshot::progress,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), CompanionSnapshot::name,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::command,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::aggression,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), CompanionSnapshot::skin,
            ItemStack.OPTIONAL_STREAM_CODEC, CompanionSnapshot::equipment,
            CompanionSnapshot::new);

    public static CompanionSnapshot of(CompanionEntity companion) {
        return new CompanionSnapshot(
                companion.species().getSerializedName(),
                companion.getLevel(),
                companion.getExperience(),
                companion.getBondPoints(),
                companion.getStars(),
                companion.progress().copy(),
                Optional.ofNullable(companion.getCustomName()).map(Component::getString),
                companion.getCommand().ordinal(),
                companion.getAggression().ordinal(),
                companion.getSkinName().isEmpty() ? Optional.empty() : Optional.of(companion.getSkinName()),
                companion.equipment().copy());
    }

    public int bondLevel() {
        return CompanionBondMath.levelFromPoints(this.bondPoints);
    }

    public CompanionCommand commandValue() {
        return CompanionCommand.byOrdinal(this.command);
    }

    public CompanionAggression aggressionValue() {
        return CompanionAggression.byOrdinal(this.aggression);
    }

    public CompanionSnapshot withName(Optional<String> name) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bondPoints, this.stars,
                this.progress, name, this.command, this.aggression, this.skin, this.equipment);
    }

    public CompanionSnapshot withSkin(Optional<String> skin) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bondPoints, this.stars,
                this.progress, this.name, this.command, this.aggression, skin, this.equipment);
    }

    public CompanionSnapshot withCommand(int command) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bondPoints, this.stars,
                this.progress, this.name, command, this.aggression, this.skin, this.equipment);
    }

    public CompanionSnapshot withAggression(int aggression) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bondPoints, this.stars,
                this.progress, this.name, this.command, aggression, this.skin, this.equipment);
    }

    public CompanionSnapshot withBondPoints(int bondPoints) {
        return new CompanionSnapshot(this.species, this.level, this.experience,
                CompanionBondMath.clampPoints(bondPoints), this.stars,
                this.progress, this.name, this.command, this.aggression, this.skin, this.equipment);
    }

    public CompanionSnapshot withEquipment(ItemStack equipment) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bondPoints, this.stars,
                this.progress, this.name, this.command, this.aggression, this.skin, equipment);
    }

    public CompanionSnapshot prestiged() {
        if (!CompanionLevels.canPrestige(this.level, this.stars)) {
            return this;
        }

        return new CompanionSnapshot(this.species, CompanionLevels.MIN_LEVEL, 0, this.bondPoints, this.stars + 1,
                this.progress, this.name, this.command, this.aggression, this.skin, this.equipment);
    }

    public Optional<CompanionSpecies> resolveSpecies() {
        return CompanionSpecies.byName(this.species);
    }

    public Component displayName() {
        return this.name.map(Component::literal)
                .map(Component.class::cast)
                .orElseGet(() -> this.resolveSpecies()
                        .map(species -> (Component) Component.translatable(species.translationKey()))
                        .orElseGet(() -> Component.translatable("tooltip.kindredspirits.charm_unbound")));
    }
}
