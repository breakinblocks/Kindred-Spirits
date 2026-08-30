package com.breakinblocks.kindredspirits.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WispLightBlock extends Block {
    public static final MapCodec<WispLightBlock> CODEC = simpleCodec(WispLightBlock::new);
    public static final int LIGHT = 10;
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
    private static final DustParticleOptions FLAME = new DustParticleOptions(0xB040FF, 0.8f);

    public WispLightBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<WispLightBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double y = pos.getY() + 0.15 + random.nextDouble() * 0.3;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        level.addParticle(FLAME, x, y, z, 0.0, 0.02, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.01, 0.0);
        }
    }
}
