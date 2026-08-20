package com.breakinblocks.kindredspirits.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public record CompanionSnapshot(String species, int level, int experience, int bond, Optional<String> name,
                                int command, int aggression, Optional<String> skin) {
    public static final Codec<CompanionSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species").forGetter(CompanionSnapshot::species),
            Codec.INT.fieldOf("level").forGetter(CompanionSnapshot::level),
            Codec.INT.optionalFieldOf("experience", 0).forGetter(CompanionSnapshot::experience),
            Codec.INT.fieldOf("bond").forGetter(CompanionSnapshot::bond),
            Codec.STRING.optionalFieldOf("name").forGetter(CompanionSnapshot::name),
            Codec.INT.optionalFieldOf("command", CompanionCommand.FOLLOW.ordinal())
                    .forGetter(CompanionSnapshot::command),
            Codec.INT.optionalFieldOf("aggression", CompanionAggression.NEUTRAL.ordinal())
                    .forGetter(CompanionSnapshot::aggression),
            Codec.STRING.optionalFieldOf("skin").forGetter(CompanionSnapshot::skin)
    ).apply(instance, CompanionSnapshot::new));

    public static final StreamCodec<ByteBuf, CompanionSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CompanionSnapshot::species,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::level,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::experience,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::bond,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), CompanionSnapshot::name,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::command,
            ByteBufCodecs.VAR_INT, CompanionSnapshot::aggression,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), CompanionSnapshot::skin,
            CompanionSnapshot::new);

    public static CompanionSnapshot of(CompanionEntity companion) {
        return new CompanionSnapshot(
                companion.species().getSerializedName(),
                companion.getLevel(),
                companion.getExperience(),
                companion.getBond(),
                Optional.ofNullable(companion.getCustomName()).map(Component::getString),
                companion.getCommand().ordinal(),
                companion.getAggression().ordinal(),
                companion.getSkinName().isEmpty() ? Optional.empty() : Optional.of(companion.getSkinName()));
    }

    public CompanionCommand commandValue() {
        return CompanionCommand.byOrdinal(this.command);
    }

    public CompanionAggression aggressionValue() {
        return CompanionAggression.byOrdinal(this.aggression);
    }

    public CompanionSnapshot withName(Optional<String> name) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bond, name,
                this.command, this.aggression, this.skin);
    }

    public CompanionSnapshot withSkin(Optional<String> skin) {
        return new CompanionSnapshot(this.species, this.level, this.experience, this.bond, this.name,
                this.command, this.aggression, skin);
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
