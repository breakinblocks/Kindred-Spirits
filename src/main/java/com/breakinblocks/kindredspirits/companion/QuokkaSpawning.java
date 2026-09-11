package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class QuokkaSpawning {
    @SubscribeEvent
    public static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(KindredEntities.type(CompanionSpecies.QUOKKA), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) ->
                        random.nextInt(3) != 0 && pos.getY() >= level.getSeaLevel()
                                && (level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                                || level.getBlockState(pos.below()).is(BlockTags.LEAVES)),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private QuokkaSpawning() {}
}
