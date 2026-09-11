package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

final class QuokkaGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    static final DataTicket<Boolean> SMILING = DataTicket.create("kindredspirits:quokka_smiling", Boolean.class);
    private static final Identifier SMILE = KindredSpirits.id("textures/entity/quokka_smile.png");

    QuokkaGeoModel() { super(KindredSpirits.id("quokka")); }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return state.getOrDefaultGeckolibData(SMILING, false) ? SMILE : super.getTextureResource(state);
    }
}
