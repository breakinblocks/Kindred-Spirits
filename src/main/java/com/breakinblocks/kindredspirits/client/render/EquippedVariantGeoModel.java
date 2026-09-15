package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredItem;

final class EquippedVariantGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    private final DeferredItem<?> item;
    private final Identifier variantModel;
    private final Identifier variantTexture;
    private final Identifier variantAnimations;

    EquippedVariantGeoModel(CompanionSpecies species, DeferredItem<?> item, String variant) {
        super(KindredSpirits.id(species.getSerializedName()));
        Identifier id = KindredSpirits.id(variant);
        this.item = item;
        this.variantModel = this.buildFormattedModelPath(id);
        this.variantTexture = this.buildFormattedTexturePath(id);
        this.variantAnimations = this.buildFormattedAnimationPath(id);
    }

    private boolean equipped(GeoRenderState state) {
        return CompanionRenderer.wears(state, this.item);
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return this.equipped(state) ? this.variantModel : super.getModelResource(state);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return this.equipped(state) ? this.variantTexture : super.getTextureResource(state);
    }

    @Override
    public Identifier getAnimationResource(CompanionEntity entity) {
        return entity.hasEquipment(this.item.get()) ? this.variantAnimations : super.getAnimationResource(entity);
    }
}
