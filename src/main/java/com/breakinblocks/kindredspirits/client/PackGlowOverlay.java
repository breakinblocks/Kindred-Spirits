package com.breakinblocks.kindredspirits.client;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class PackGlowOverlay {
    private static Set<Integer> marked = Set.of();
    private static long expiresAt;
    private static @Nullable ClientLevel sourceLevel;

    public static void show(List<Integer> entityIds, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        sourceLevel = minecraft.level;
        marked = Set.copyOf(entityIds);
        expiresAt = minecraft.level.getGameTime() + ticks;
    }

    public static void clientTick() {
        if (marked.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != sourceLevel || minecraft.level.getGameTime() >= expiresAt) {
            marked = Set.of();
            sourceLevel = null;
        }
    }

    public static void outline(Entity entity, EntityRenderState state) {
        if (state.outlineColor == 0
                && !marked.isEmpty()
                && entity.level() == sourceLevel
                && marked.contains(entity.getId())) {
            state.outlineColor = ARGB.opaque(entity.getTeamColor());
        }
    }

    private PackGlowOverlay() {}
}
