package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.client.CompanionSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

class CompanionGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    CompanionGeoModel(ResourceLocation asset) {
        super(asset);
    }

    @Override
    public final ResourceLocation getTextureResource(CompanionEntity companion) {
        return CompanionSkins.skinned(
                this.baseTexture(companion), companion.species().usesPlayerSkin() ? "" : companion.getSkinName());
    }

    protected ResourceLocation baseTexture(CompanionEntity companion) {
        return super.getTextureResource(companion);
    }

    @Override
    public void setCustomAnimations(
            CompanionEntity companion, long instanceId, AnimationState<CompanionEntity> animationState) {
        turnHead(this, companion, animationState);
    }

    static void turnHead(
            GeoModel<CompanionEntity> model, CompanionEntity companion, AnimationState<CompanionEntity> state) {
        CompanionSpecies species = companion.species();
        List<String> headBones = species.headBones();
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (headBones.isEmpty() || data == null) {
            return;
        }

        float limit = species.maxHeadYaw();
        float yaw = Mth.clamp(Mth.wrapDegrees(-data.netHeadYaw()), -limit, limit) * Mth.DEG_TO_RAD / headBones.size();
        float pitch = Mth.clamp(-data.headPitch(), -limit, limit) * Mth.DEG_TO_RAD / headBones.size();

        boolean yawOnZ = species.headYawAxis() == Direction.Axis.Z;

        for (String name : headBones) {
            GeoBone bone = model.getAnimationProcessor().getBone(name);
            if (bone == null) {
                continue;
            }
            bone.setRotX(bone.getRotX() + pitch);
            if (yawOnZ) {
                bone.setRotZ(bone.getRotZ() + yaw);
            } else {
                bone.setRotY(bone.getRotY() + yaw);
            }
        }
    }
}
