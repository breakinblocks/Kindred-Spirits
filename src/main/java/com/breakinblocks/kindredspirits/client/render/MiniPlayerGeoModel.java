package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.KindredSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MiniPlayerGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    private final ResourceLocation slimModel;

    public MiniPlayerGeoModel(ResourceLocation assetSubpath) {
        super(assetSubpath);
        this.slimModel = this.buildFormattedModelPath(KindredSpirits.id(assetSubpath.getPath() + "_slim"));
    }

    @Override
    public ResourceLocation getModelResource(CompanionEntity companion) {
        return KindredSkins.skinFor(companion).model() == PlayerSkin.Model.SLIM
                ? this.slimModel
                : super.getModelResource(companion);
    }

    @Override
    public ResourceLocation getTextureResource(CompanionEntity companion) {
        return KindredSkins.skinFor(companion).texture();
    }

    @Override
    public void setCustomAnimations(
            CompanionEntity companion, long instanceId, AnimationState<CompanionEntity> animationState) {
        CompanionGeoModel.turnHead(this, companion, animationState);
    }
}
