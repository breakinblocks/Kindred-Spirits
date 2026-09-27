package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.MeteorEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class MeteorRenderer extends GeoEntityRenderer<MeteorEntity, EntityRenderState> {
    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(KindredSpirits.id("meteor")));
        this.withScale(1.25f);
    }

    @Override
    protected int getBlockLightLevel(MeteorEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
