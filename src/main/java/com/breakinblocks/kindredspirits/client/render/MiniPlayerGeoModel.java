package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.KindredSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MiniPlayerGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    private static final float BABY_SCALE = 0.5f;
    private static final float BABY_BODY_DROP = 12.0f;
    private static final float BABY_LEG_DROP = 6.0f;
    private static final float BABY_LEG_INSET = 0.95f;

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
        if (companion.isBabyBody()) {
            this.shrinkBody();
        }
    }

    private void shrinkBody() {
        GeoBone root = this.getAnimationProcessor().getBone("root");
        if (root != null) {
            root.setPosY(root.getPosY() * BABY_SCALE);
        }
        GeoBone body = this.getAnimationProcessor().getBone("body");
        if (body != null) {
            body.setPosY(-BABY_BODY_DROP);
            body.updateScale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
        }
        GeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            head.updateScale(1.0f / BABY_SCALE, 1.0f / BABY_SCALE, 1.0f / BABY_SCALE);
        }
        this.shrinkLeg("right_leg", BABY_LEG_INSET);
        this.shrinkLeg("left_leg", -BABY_LEG_INSET);
    }

    private void shrinkLeg(String name, float inset) {
        GeoBone leg = this.getAnimationProcessor().getBone(name);
        if (leg != null) {
            leg.updatePosition(inset, -BABY_LEG_DROP, 0.0f);
            leg.updateScale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
        }
    }
}
