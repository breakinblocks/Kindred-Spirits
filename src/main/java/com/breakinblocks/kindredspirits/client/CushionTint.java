package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.block.CompanionBedBlock;
import com.breakinblocks.kindredspirits.block.CushionColor;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public final class CushionTint {
    public static final BlockTintSource BLOCK = new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            return ARGB.opaque(state.getValue(CompanionBedBlock.COLOR).rgb());
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of(CompanionBedBlock.COLOR);
        }
    };

    public record Item() implements ItemTintSource {
        public static final MapCodec<Item> MAP_CODEC = MapCodec.unit(new Item());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            BlockItemStateProperties properties =
                    stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
            CushionColor colour = properties.get(CompanionBedBlock.COLOR);
            return ARGB.opaque((colour == null ? CushionColor.NONE : colour).rgb());
        }

        @Override
        public MapCodec<Item> type() {
            return MAP_CODEC;
        }
    }

    private CushionTint() {}
}
