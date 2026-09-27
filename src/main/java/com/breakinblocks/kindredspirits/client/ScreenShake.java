package com.breakinblocks.kindredspirits.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jspecify.annotations.Nullable;

public final class ScreenShake {
    private static final int DURATION_TICKS = 12;
    private static final float STRENGTH_DEGREES = 1.6f;

    private static long startedAt = -1L;
    private static @Nullable ClientLevel sourceLevel;

    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        sourceLevel = minecraft.level;
        startedAt = minecraft.level.getGameTime();
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (startedAt < 0L) {
            return;
        }
        if (minecraft.level == null || minecraft.level != sourceLevel) {
            startedAt = -1L;
            return;
        }

        double age = minecraft.level.getGameTime() - startedAt + event.getPartialTick();
        if (age >= DURATION_TICKS) {
            startedAt = -1L;
            return;
        }

        double fade = 1.0 - age / DURATION_TICKS;
        float amount = (float) (STRENGTH_DEGREES
                * fade
                * fade
                * minecraft.options.screenEffectScale().get());
        event.setPitch(event.getPitch() + amount * (float) Math.sin(age * 2.9));
        event.setYaw(event.getYaw() + amount * 0.6f * (float) Math.sin(age * 2.3 + 1.0));
        event.setRoll(event.getRoll() + amount * 0.8f * (float) Math.sin(age * 3.7 + 2.0));
    }

    private ScreenShake() {}
}
