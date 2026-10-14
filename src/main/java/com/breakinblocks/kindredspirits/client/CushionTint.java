package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.block.CompanionBedBlock;
import com.breakinblocks.kindredspirits.block.CushionColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class CushionTint {
    public static int block(
            BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        return FastColor.ARGB32.opaque(state.getValue(CompanionBedBlock.COLOR).rgb());
    }

    public static int item(ItemStack stack, int tintIndex) {
        BlockItemStateProperties properties =
                stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        CushionColor colour = properties.get(CompanionBedBlock.COLOR);
        return FastColor.ARGB32.opaque((colour == null ? CushionColor.NONE : colour).rgb());
    }

    private CushionTint() {}
}
