package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.util.BlockPosUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CompanionTinkering {
    private static final int SPARK_COUNT = 8;

    public static @Nullable BlockPos accelerateNearby(ServerLevel level, BlockPos center, int radius,
                                                      int extraTicks, RandomSource random) {
        List<BlockPos> targets = new ArrayList<>();

        for (BlockPos pos : BlockPosUtil.cube(center, radius)) {
            if (level.getBlockState(pos).hasBlockEntity() && ticker(level, pos) != null) {
                targets.add(pos.immutable());
            }
        }

        if (targets.isEmpty()) {
            return null;
        }

        BlockPos pick = targets.get(random.nextInt(targets.size()));
        return accelerate(level, pick, extraTicks) ? pick : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean accelerate(ServerLevel level, BlockPos pos, int extraTicks) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockEntityTicker ticker = ticker(level, pos);

        if (blockEntity == null || ticker == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        for (int i = 0; i < extraTicks && !blockEntity.isRemoved(); i++) {
            ticker.tick(level, pos, state, blockEntity);
        }

        return true;
    }

    private static @Nullable BlockEntityTicker<?> ticker(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null : level.getBlockState(pos).getTicker(level, blockEntity.getType());
    }

    public static void sparks(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, SPARK_COUNT, 0.3, 0.3, 0.3, 0.05);
    }

    private CompanionTinkering() {
    }
}
