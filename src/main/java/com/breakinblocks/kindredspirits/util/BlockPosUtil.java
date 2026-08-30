package com.breakinblocks.kindredspirits.util;

import net.minecraft.core.BlockPos;

public final class BlockPosUtil {
    public static Iterable<BlockPos> cube(BlockPos center, int radius) {
        return BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius));
    }

    private BlockPosUtil() {
    }
}
