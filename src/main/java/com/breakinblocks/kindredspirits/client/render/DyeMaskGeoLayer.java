package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.client.CompanionSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

final class DyeMaskGeoLayer extends GeoRenderLayer<CompanionEntity> {
    DyeMaskGeoLayer(GeoRenderer<CompanionEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ResourceLocation getTextureResource(CompanionEntity companion) {
        return super.getTextureResource(companion).withPath(path -> path.replace(".png", "_dyemask.png"));
    }

    @Override
    public void render(
            PoseStack poseStack,
            CompanionEntity companion,
            BakedGeoModel bakedModel,
            @Nullable RenderType renderType,
            MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay) {
        int dye = companion.getDyeId();
        if (dye == CompanionEntity.NO_DYE) {
            return;
        }

        ResourceLocation mask = this.getTextureResource(companion);
        if (!CompanionSkins.exists(mask)) {
            return;
        }

        RenderType maskType = this.getRenderer().getRenderType(companion, mask, bufferSource, partialTick);
        if (maskType == null) {
            return;
        }

        int colour = FastColor.ARGB32.multiply(
                this.getRenderer()
                        .getRenderColor(companion, partialTick, packedLight)
                        .argbInt(),
                FastColor.ARGB32.opaque(DyeColor.byId(dye).getTextureDiffuseColor()));
        this.getRenderer()
                .reRender(
                        bakedModel,
                        poseStack,
                        bufferSource,
                        companion,
                        maskType,
                        bufferSource.getBuffer(maskType),
                        partialTick,
                        packedLight,
                        packedOverlay,
                        colour);
    }
}
