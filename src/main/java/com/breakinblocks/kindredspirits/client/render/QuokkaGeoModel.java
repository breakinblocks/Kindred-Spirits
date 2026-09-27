package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

final class QuokkaGeoModel extends CompanionGeoModel {
    private static final Identifier SMILE = KindredSpirits.id("textures/entity/quokka_smile.png");

    QuokkaGeoModel() {
        super(KindredSpirits.id("quokka"));
    }

    @Override
    protected Identifier baseTexture(GeoRenderState state) {
        return CompanionRenderer.wears(state, KindredItems.QUOKKA_SNACK) ? SMILE : super.baseTexture(state);
    }
}
