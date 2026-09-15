package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

final class TrexGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    private static final Identifier GLOVES = KindredSpirits.id("trex_gloves");

    private final Identifier glovesModel = this.buildFormattedModelPath(GLOVES);
    private final Identifier glovesTexture = this.buildFormattedTexturePath(GLOVES);
    private final Identifier glovesAnimations = this.buildFormattedAnimationPath(GLOVES);

    TrexGeoModel() {
        super(KindredSpirits.id("trex"));
    }

    private static boolean gloved(GeoRenderState state) {
        return CompanionRenderer.wears(state, KindredItems.BOXING_GLOVES);
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return gloved(state) ? this.glovesModel : super.getModelResource(state);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return gloved(state) ? this.glovesTexture : super.getTextureResource(state);
    }

    @Override
    public Identifier getAnimationResource(CompanionEntity entity) {
        return entity.hasEquipment(KindredItems.BOXING_GLOVES.get()) ? this.glovesAnimations : super.getAnimationResource(entity);
    }
}
