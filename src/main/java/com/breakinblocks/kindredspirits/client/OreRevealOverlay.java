package com.breakinblocks.kindredspirits.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class OreRevealOverlay {
    private static final float RED = 1.0f;
    private static final float GREEN = 0xD2 / 255.0f;
    private static final float BLUE = 0x4A / 255.0f;
    private static final float STROKE_ALPHA = 0xCC / 255.0f;
    private static final float FILL_ALPHA = 0x30 / 255.0f;
    private static final double PADDING = -0.02;

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
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || positions.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != sourceLevel) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        for (BlockPos pos : positions) {
            AABB box = new AABB(pos).inflate(PADDING);
            LevelRenderer.addChainedFilledBoxVertices(
                    pose,
                    buffers.getBuffer(RevealRenderTypes.FILL),
                    box.minX,
                    box.minY,
                    box.minZ,
                    box.maxX,
                    box.maxY,
                    box.maxZ,
                    RED,
                    GREEN,
                    BLUE,
                    FILL_ALPHA);
        }
        buffers.endBatch(RevealRenderTypes.FILL);

        for (BlockPos pos : positions) {
            LevelRenderer.renderLineBox(
                    pose,
                    buffers.getBuffer(RevealRenderTypes.LINES),
                    new AABB(pos).inflate(PADDING),
                    RED,
                    GREEN,
                    BLUE,
                    STROKE_ALPHA);
        }
        buffers.endBatch(RevealRenderTypes.LINES);

        pose.popPose();
    }

    private static final class RevealRenderTypes extends RenderType {
        private static final RenderType LINES = create(
                "kindredspirits_ore_reveal_lines",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES,
                1536,
                false,
                false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(1.5)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));

        private static final RenderType FILL = create(
                "kindredspirits_ore_reveal_fill",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLE_STRIP,
                1536,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));

        private RevealRenderTypes(
                String name,
                VertexFormat format,
                VertexFormat.Mode mode,
                int bufferSize,
                boolean affectsCrumbling,
                boolean sortOnUpload,
                Runnable setupState,
                Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }
    }

    private OreRevealOverlay() {}
}
