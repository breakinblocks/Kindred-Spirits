package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.worldgen.BeachSuspiciousSandFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KindredFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, KindredSpirits.MOD_ID);

    public static final DeferredHolder<Feature<?>, BeachSuspiciousSandFeature> BEACH_SUSPICIOUS_SAND =
            FEATURES.register("beach_suspicious_sand", BeachSuspiciousSandFeature::new);

    private KindredFeatures() {}
}
