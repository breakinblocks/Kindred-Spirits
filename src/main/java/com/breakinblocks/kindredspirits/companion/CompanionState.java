package com.breakinblocks.kindredspirits.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** State that must survive both chunk saves and dismissal. Deadlines use server game time. */
public record CompanionState(float health, Map<ResourceLocation, Long> cooldowns) {
    public static final CompanionState LEGACY = new CompanionState(-1.0f, Map.of());
    public static final Codec<CompanionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("health", -1.0f).forGetter(CompanionState::health),
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                            .optionalFieldOf("cooldowns", Map.of())
                            .forGetter(CompanionState::cooldowns))
            .apply(instance, CompanionState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionState> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public CompanionState {
        cooldowns = Map.copyOf(cooldowns);
    }
}
