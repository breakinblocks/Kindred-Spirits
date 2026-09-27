package com.breakinblocks.kindredspirits.block;

import com.breakinblocks.kindredspirits.companion.CompanionSpawns;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.registry.KindredParticles;
import com.breakinblocks.kindredspirits.registry.KindredSounds;
import com.breakinblocks.kindredspirits.util.BlockPosUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrexEggBlock extends Block {
    public static final MapCodec<TrexEggBlock> CODEC = simpleCodec(TrexEggBlock::new);
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int MAX_HATCH = 2;
    private static final int HATCH_TICKS = 6000;
    private static final int WARM_RADIUS = 2;
    private static final int RANDOM_OFFSET = 100;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 14.0);

    public TrexEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    public MapCodec<TrexEggBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(this)) {
            return;
        }

        if (level instanceof ServerLevel server && isWarm(level, pos)) {
            server.sendParticles(
                    KindredParticles.EGG_WARMTH.get(),
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    8,
                    0.25,
                    0.15,
                    0.25,
                    0.01);
        }
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(state));
        this.scheduleStage(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int hatch = state.getValue(HATCH);
        level.sendParticles(
                KindredParticles.EGG_WARMTH.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                hatch == MAX_HATCH ? 12 : 4,
                0.25,
                0.15,
                0.25,
                0.02);
        if (hatch < MAX_HATCH) {
            level.playSound(
                    null,
                    pos,
                    KindredSounds.TREX_EGG_CRACK.get(),
                    SoundSource.BLOCKS,
                    0.7f,
                    0.9f + random.nextFloat() * 0.2f);
            level.setBlock(pos, state.setValue(HATCH, hatch + 1), Block.UPDATE_CLIENTS);
            this.scheduleStage(level, pos);
            return;
        }

        level.playSound(
                null,
                pos,
                KindredSounds.TREX_EGG_HATCH.get(),
                SoundSource.BLOCKS,
                0.7f,
                0.9f + random.nextFloat() * 0.2f);
        if (CompanionSpawns.spawnWild(level, CompanionSpecies.TREX, pos.getCenter(), random.nextFloat() * 360.0f)
                != null) {
            level.destroyBlock(pos, false);
        } else {
            this.scheduleStage(level, pos);
        }
    }

    private void scheduleStage(Level level, BlockPos pos) {
        int total = isWarm(level, pos) ? HATCH_TICKS / 2 : HATCH_TICKS;
        level.scheduleTick(
                pos, this, total / (MAX_HATCH + 1) + level.getRandom().nextInt(RANDOM_OFFSET));
    }

    public static boolean isWarm(BlockGetter level, BlockPos pos) {
        for (BlockPos check : BlockPosUtil.cube(pos, WARM_RADIUS)) {
            BlockState state = level.getBlockState(check);
            if (state.is(BlockTags.CAMPFIRES) || state.getBlock() instanceof BaseTorchBlock) {
                return true;
            }
        }
        return false;
    }
}
