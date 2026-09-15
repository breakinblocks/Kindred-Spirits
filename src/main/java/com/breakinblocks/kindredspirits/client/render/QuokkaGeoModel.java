package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

final class QuokkaGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    private static final Identifier SMILE = KindredSpirits.id("textures/entity/quokka_smile.png");

    QuokkaGeoModel() { super(KindredSpirits.id("quokka")); }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return CompanionRenderer.wears(state, KindredItems.QUOKKA_SNACK) ? SMILE : super.getTextureResource(state);
    }
}
