package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DyeMaskGeoLayer extends GeoRenderLayer<CompanionEntity, Void, LivingEntityRenderState> {
    private static final Map<Identifier, Boolean> PRESENT = new ConcurrentHashMap<>();

    DyeMaskGeoLayer(GeoRenderer<CompanionEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    protected Identifier getTextureResource(LivingEntityRenderState renderState) {
        return this.getGeoModel().getTextureResource(renderState)
                .withPath(path -> path.replace(".png", "_dyemask.png"));
    }

    @Override
    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> renderPass, SubmitNodeCollector collector) {
        int dye = renderPass.getOrDefaultGeckolibData(CompanionRenderer.DYE, CompanionEntity.NO_DYE);
        if (!renderPass.willRender() || dye == CompanionEntity.NO_DYE) {
            return;
        }

        Identifier mask = this.getTextureResource(renderPass.renderState());
        if (!exists(mask)) {
            return;
        }

        RenderType renderType = this.renderer.getRenderType(renderPass.renderState(), mask);
        int light = renderPass.packedLight();
        int overlay = renderPass.packedOverlay();
        int colour = ARGB.multiply(renderPass.renderColor(), ARGB.opaque(DyeColor.byId(dye).getTextureDiffuseColor()));
        collector.order(1).submitCustomGeometry(renderPass.poseStack(), renderType, (pose, buffer) -> {
            PoseStack poseStack = renderPass.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPass.renderPosed(() -> renderPass.model().render(renderPass, buffer, light, overlay, colour));
            poseStack.popPose();
        });
    }

    private static boolean exists(Identifier texture) {
        return PRESENT.computeIfAbsent(texture,
                id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
    }
}
