package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.client.CompanionSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

class CompanionGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    CompanionGeoModel(Identifier asset) {
        super(asset);
    }

    @Override
    public final Identifier getTextureResource(GeoRenderState state) {
        return CompanionSkins.skinned(this.baseTexture(state), state.getOrDefaultGeckolibData(CompanionRenderer.SKIN, ""));
    }

    protected Identifier baseTexture(GeoRenderState state) {
        return super.getTextureResource(state);
    }
}
