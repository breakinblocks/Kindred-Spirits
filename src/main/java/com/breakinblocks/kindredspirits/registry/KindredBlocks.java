package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.block.TrexEggBlock;
import com.breakinblocks.kindredspirits.block.WispLightBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KindredBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KindredSpirits.MOD_ID);

    public static final DeferredBlock<WispLightBlock> WISP_LIGHT = BLOCKS.registerBlock(
            "wisp_light",
            WispLightBlock::new,
            BlockBehaviour.Properties.of()
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .replaceable()
                    .instabreak()
                    .noTerrainParticles()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.CANDLE)
                    .lightLevel(state -> WispLightBlock.LIGHT));

    public static final DeferredBlock<TrexEggBlock> TREX_EGG = BLOCKS.registerBlock(
            "trex_egg",
            TrexEggBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5f)
                    .noOcclusion()
                    .sound(SoundType.STONE));

    private KindredBlocks() {}
}
