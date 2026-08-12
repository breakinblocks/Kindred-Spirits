package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class KindredDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, KindredSpirits.MOD_ID);

    public static final Supplier<DataComponentType<UUID>> BOUND_COMPANION =
            DATA_COMPONENTS.register("bound_companion", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final Supplier<DataComponentType<CompanionSnapshot>> COMPANION_SNAPSHOT =
            DATA_COMPONENTS.register("companion_snapshot", () -> DataComponentType.<CompanionSnapshot>builder()
                    .persistent(CompanionSnapshot.CODEC)
                    .networkSynchronized(CompanionSnapshot.STREAM_CODEC)
                    .build());

    public record CompanionSnapshot(String species, int level, int bond) {
        public static final Codec<CompanionSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("species").forGetter(CompanionSnapshot::species),
                Codec.INT.fieldOf("level").forGetter(CompanionSnapshot::level),
                Codec.INT.fieldOf("bond").forGetter(CompanionSnapshot::bond)
        ).apply(instance, CompanionSnapshot::new));

        public static final StreamCodec<io.netty.buffer.ByteBuf, CompanionSnapshot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, CompanionSnapshot::species,
                ByteBufCodecs.VAR_INT, CompanionSnapshot::level,
                ByteBufCodecs.VAR_INT, CompanionSnapshot::bond,
                CompanionSnapshot::new);

        public Optional<CompanionSpecies> resolveSpecies() {
            return CompanionSpecies.byName(this.species);
        }
    }

    private KindredDataComponents() {
    }
}
