package com.breakinblocks.kindredspirits.worldgen;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

public class BeachSuspiciousSandFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT_TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, KindredSpirits.id("archaeology/beach_sand"));
    private static final int MAX_DEPTH = 3;

    public BeachSuspiciousSandFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!KindredConfig.COMMON_SPEC.isLoaded() || !KindredConfig.COMMON.beachSuspiciousSand.get()) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos pos = context.origin().below(1 + context.random().nextInt(MAX_DEPTH));
        if (!level.getBlockState(pos).is(Blocks.SAND) || !level.getBlockState(pos.below()).isSolid()) {
            return false;
        }

        level.setBlock(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.getBlockEntity(pos, BlockEntityType.BRUSHABLE_BLOCK)
                .ifPresent(sand -> sand.setLootTable(LOOT_TABLE, pos.asLong()));
        return true;
    }
}
