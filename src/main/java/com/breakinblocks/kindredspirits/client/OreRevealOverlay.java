package com.breakinblocks.kindredspirits.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

public final class OreRevealOverlay {
    private static final GizmoStyle STYLE = GizmoStyle.strokeAndFill(0xCCFFD24A, 1.5f, 0x30FFD24A);
    private static final float PADDING = -0.02f;

    private static List<BlockPos> positions = List.of();
    private static long expiresAt;
    private static ClientLevel sourceLevel;

    public static void show(List<BlockPos> ores, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        sourceLevel = minecraft.level;
        positions = List.copyOf(ores);
        expiresAt = minecraft.level.getGameTime() + ticks;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (positions.isEmpty()) {
            return;
        }
        if (minecraft.level == null || minecraft.level != sourceLevel || minecraft.level.getGameTime() >= expiresAt) {
            positions = List.of();
            sourceLevel = null;
            return;
        }
        for (BlockPos pos : positions) {
            Gizmos.cuboid(pos, PADDING, STYLE).setAlwaysOnTop();
        }
    }

    private OreRevealOverlay() {}
}
