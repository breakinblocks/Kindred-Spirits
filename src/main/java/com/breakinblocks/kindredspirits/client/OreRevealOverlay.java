package com.breakinblocks.kindredspirits.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

import java.util.List;

public final class OreRevealOverlay {
    private static final GizmoStyle STYLE = GizmoStyle.strokeAndFill(0xCCFFD24A, 1.5f, 0x30FFD24A);
    private static final float PADDING = -0.02f;

    private static List<BlockPos> positions = List.of();
    private static long expiresAt;

    public static void show(List<BlockPos> ores, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        positions = List.copyOf(ores);
        expiresAt = minecraft.level.getGameTime() + ticks;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (positions.isEmpty()) {
            return;
        }
        if (minecraft.level == null || minecraft.level.getGameTime() >= expiresAt) {
            positions = List.of();
            return;
        }
        for (BlockPos pos : positions) {
            Gizmos.cuboid(pos, PADDING, STYLE).setAlwaysOnTop();
        }
    }

    private OreRevealOverlay() {
    }
}
