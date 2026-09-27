package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record CompanionSnapshot(
        String species,
        int level,
        int experience,
        int bondPoints,
        int stars,
        CompanionProgress progress,
        Optional<String> name,
        int command,
        int aggression,
        Optional<String> skin,
        ItemStack equipment,
        List<String> disabledAbilities,
        CompanionState state,
        int dye) {
    private static final int LEGACY_BOND = -1;

    public static final Codec<CompanionSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("species").forGetter(CompanionSnapshot::species),
                    Codec.INT.fieldOf("level").forGetter(CompanionSnapshot::level),
                    Codec.INT.optionalFieldOf("experience", 0).forGetter(CompanionSnapshot::experience),
                    Codec.INT.optionalFieldOf("bond_points", LEGACY_BOND).forGetter(CompanionSnapshot::bondPoints),
                    Codec.INT.optionalFieldOf("bond", 0).forGetter(snapshot -> 0),
                    Codec.INT.optionalFieldOf("stars", 0).forGetter(CompanionSnapshot::stars),
                    CompanionProgress.CODEC
                            .optionalFieldOf("progress", new CompanionProgress())
                            .forGetter(CompanionSnapshot::progress),
                    Codec.STRING.optionalFieldOf("name").forGetter(CompanionSnapshot::name),
                    Codec.INT
                            .optionalFieldOf("command", CompanionCommand.FOLLOW.ordinal())
                            .forGetter(CompanionSnapshot::command),
                    Codec.INT
                            .optionalFieldOf("aggression", CompanionAggression.NEUTRAL.ordinal())
                            .forGetter(CompanionSnapshot::aggression),
                    Codec.STRING.optionalFieldOf("skin").forGetter(CompanionSnapshot::skin),
                    ItemStack.OPTIONAL_CODEC
                            .optionalFieldOf("equipment", ItemStack.EMPTY)
                            .forGetter(CompanionSnapshot::equipment),
                    Codec.STRING
                            .listOf()
                            .optionalFieldOf("disabled_abilities", List.of())
                            .forGetter(CompanionSnapshot::disabledAbilities),
                    CompanionState.CODEC
                            .optionalFieldOf("state", CompanionState.LEGACY)
                            .forGetter(CompanionSnapshot::state),
                    Codec.INT.optionalFieldOf("dye", CompanionEntity.NO_DYE).forGetter(CompanionSnapshot::dye))
            .apply(instance, CompanionSnapshot::fromSaved));

    private static CompanionSnapshot fromSaved(
            String species,
            int level,
            int experience,
            int bondPoints,
            int legacyBond,
            int stars,
            CompanionProgress progress,
            Optional<String> name,
            int command,
            int aggression,
            Optional<String> skin,
            ItemStack equipment,
            List<String> disabledAbilities,
            CompanionState state,
            int dye) {
        int points = bondPoints == LEGACY_BOND ? CompanionBondMath.migrateLegacy(legacyBond) : bondPoints;
        return new CompanionSnapshot(
                species,
                level,
                experience,
                points,
                stars,
                progress,
                name,
                command,
                aggression,
                skin,
                equipment,
                disabledAbilities,
                state,
                dye);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionSnapshot> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CompanionSnapshot decode(RegistryFriendlyByteBuf buffer) {
            return new CompanionSnapshot(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    CompanionProgress.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buffer),
                    CompanionState.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CompanionSnapshot snapshot) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, snapshot.species());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.level());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.experience());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.bondPoints());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.stars());
            CompanionProgress.STREAM_CODEC.encode(buffer, snapshot.progress());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, snapshot.name());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.command());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.aggression());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, snapshot.skin());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, snapshot.equipment());
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buffer, snapshot.disabledAbilities());
            CompanionState.STREAM_CODEC.encode(buffer, snapshot.state());
            ByteBufCodecs.VAR_INT.encode(buffer, snapshot.dye());
        }
    };

    public static CompanionSnapshot of(CompanionEntity companion) {
        return new CompanionSnapshot(
                companion.species().getSerializedName(),
                companion.getLevel(),
                companion.getExperience(),
                companion.getBondPoints(),
                companion.getStars(),
                companion.progress().copy(),
                Optional.ofNullable(companion.getCustomName()).map(Component::getString),
                companion.commandForStorage().ordinal(),
                companion.getAggression().ordinal(),
                nonEmpty(companion.getSkinName()),
                companion.equipment().copy(),
                companion.disabledAbilityNames(),
                companion.savedState(),
                companion.getDyeId());
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
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                name,
                this.command,
                this.aggression,
                this.skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withSkin(Optional<String> skin) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                this.name,
                this.command,
                this.aggression,
                skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withCommand(int command) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                this.name,
                command,
                this.aggression,
                this.skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withAggression(int aggression) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                this.name,
                this.command,
                aggression,
                this.skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withBondPoints(int bondPoints) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                CompanionBondMath.clampPoints(bondPoints),
                this.stars,
                this.progress,
                this.name,
                this.command,
                this.aggression,
                this.skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withEquipment(ItemStack equipment) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                this.name,
                this.command,
                this.aggression,
                this.skin,
                equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withDisabledAbilities(List<String> disabledAbilities) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                this.name,
                this.command,
                this.aggression,
                this.skin,
                this.equipment,
                disabledAbilities,
                this.state,
                this.dye);
    }

    public CompanionSnapshot withDye(int dye) {
        return new CompanionSnapshot(
                this.species,
                this.level,
                this.experience,
                this.bondPoints,
                this.stars,
                this.progress,
                this.name,
                this.command,
                this.aggression,
                this.skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                dye);
    }

    public static Optional<String> nonEmpty(String value) {
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    public static boolean isDisabled(List<String> disabledAbilities, CompanionAbility ability) {
        return disabledAbilities.contains(ability.id().getPath());
    }

    public boolean isDisabled(CompanionAbility ability) {
        return isDisabled(this.disabledAbilities, ability);
    }

    public List<CompanionAbility> activeAbilities(CompanionSpecies species) {
        return species.abilitiesAt(CompanionLevels.clampLevel(this.level), this.bondLevel()).stream()
                .filter(ability -> !this.isDisabled(ability))
                .toList();
    }

    public CompanionSnapshot prestiged() {
        if (!CompanionLevels.canPrestige(this.level, this.stars)) {
            return this;
        }

        return new CompanionSnapshot(
                this.species,
                CompanionLevels.MIN_LEVEL,
                0,
                this.bondPoints,
                this.stars + 1,
                this.progress,
                this.name,
                this.command,
                this.aggression,
                this.skin,
                this.equipment,
                this.disabledAbilities,
                this.state,
                this.dye);
    }

    public Optional<CompanionSpecies> resolveSpecies() {
        return CompanionSpecies.byName(this.species);
    }

    public boolean usesPlayerSkin() {
        return this.resolveSpecies().map(CompanionSpecies::usesPlayerSkin).orElse(false);
    }

    public Component displayName() {
        return this.name
                .map(Component::literal)
                .map(Component.class::cast)
                .orElseGet(() -> this.resolveSpecies()
                        .map(species -> (Component) Component.translatable(species.translationKey()))
                        .orElseGet(() -> Component.translatable("tooltip.kindredspirits.charm_unbound")));
    }
}
