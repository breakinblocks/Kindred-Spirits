package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.SpiritArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class SpiritArrowRenderer extends ArrowRenderer<SpiritArrow> {
    private static final ResourceLocation TEXTURE = KindredSpirits.id("textures/entity/projectiles/spirit_arrow.png");

    public SpiritArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritArrow entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(SpiritArrow entity, BlockPos pos) {
        return 15;
    }
}
