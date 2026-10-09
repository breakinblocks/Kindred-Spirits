package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.MeteorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MeteorRenderer extends GeoEntityRenderer<MeteorEntity> {
    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(KindredSpirits.id("meteor")));
        this.withScale(1.25f);
    }

    @Override
    protected int getBlockLightLevel(MeteorEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public RenderType getRenderType(
            MeteorEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
