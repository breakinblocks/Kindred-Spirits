package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredItem;

final class EquippedVariantGeoModel extends CompanionGeoModel {
    private final DeferredItem<?> item;
    private final ResourceLocation variantModel;
    private final ResourceLocation variantTexture;
    private final ResourceLocation variantAnimations;

    EquippedVariantGeoModel(CompanionSpecies species, DeferredItem<?> item, String variant) {
        super(KindredSpirits.id(species.getSerializedName()));
        ResourceLocation id = KindredSpirits.id(variant);
        this.item = item;
        this.variantModel = this.buildFormattedModelPath(id);
        this.variantTexture = this.buildFormattedTexturePath(id);
        this.variantAnimations = this.buildFormattedAnimationPath(id);
    }

    private boolean equipped(CompanionEntity companion) {
        return companion.hasEquipment(this.item.get());
    }

    @Override
    public ResourceLocation getModelResource(CompanionEntity companion) {
        return this.equipped(companion) ? this.variantModel : super.getModelResource(companion);
    }

    @Override
    protected ResourceLocation baseTexture(CompanionEntity companion) {
        return this.equipped(companion) ? this.variantTexture : super.baseTexture(companion);
    }

    @Override
    public ResourceLocation getAnimationResource(CompanionEntity companion) {
        return this.equipped(companion) ? this.variantAnimations : super.getAnimationResource(companion);
    }
}
