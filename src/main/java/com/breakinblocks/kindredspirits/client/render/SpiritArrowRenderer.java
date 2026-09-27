package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.SpiritArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class SpiritArrowRenderer extends ArrowRenderer<SpiritArrow, ArrowRenderState> {
    private static final Identifier TEXTURE = KindredSpirits.id("textures/entity/projectiles/spirit_arrow.png");

    public SpiritArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }

    @Override
    protected int getBlockLightLevel(SpiritArrow entity, BlockPos pos) {
        return 15;
    }
}
