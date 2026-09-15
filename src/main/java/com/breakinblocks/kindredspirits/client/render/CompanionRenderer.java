package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.breakinblocks.kindredspirits.companion.CompanionAnimations;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.breakinblocks.kindredspirits.net.CharmView;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class CompanionRenderer extends GeoEntityRenderer<CompanionEntity, LivingEntityRenderState> {
    private final CompanionSpecies species;

    public CompanionRenderer(EntityRendererProvider.Context context, CompanionSpecies species) {
        this(context, species, species == CompanionSpecies.QUOKKA ? new QuokkaGeoModel()
                : new DefaultedEntityGeoModel<CompanionEntity>(KindredSpirits.id(species.getSerializedName())));
        if (species.hasGlowMask()) this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    protected CompanionRenderer(EntityRendererProvider.Context context, CompanionSpecies species,
                                GeoModel<CompanionEntity> model) {
        super(context, model);
        this.species = species;
        this.withScale(species.renderScale());
        this.shadowRadius = species.width() * 0.6f;
    }

    @Override
    public void addRenderData(CompanionEntity entity, Void relatedObject,
                              LivingEntityRenderState renderState, float partialTick) {
        super.addRenderData(entity, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(QuokkaGeoModel.SMILING, entity.isQuokkaSmiling());
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPass, float width, float height) {
        float ageScale = renderPass.renderState().ageScale;
        super.scaleModelForRender(renderPass, width * ageScale, height * ageScale);
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return this.species.hasAnimation(CompanionAnimations.DEATH)
                ? 0.0f
                : super.getDeathMaxRotation(renderState);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPass,
                                          BoneSnapshots boneSnapshots) {
        super.adjustModelBonesForRender(renderPass, boneSnapshots);

        List<String> headBones = this.species.headBones();
        if (headBones.isEmpty()) {
            return;
        }

        LivingEntityRenderState state = renderPass.renderState();
        float limit = this.species.maxHeadYaw();
        float yaw = Mth.clamp(state.yRot, -limit, limit) * Mth.DEG_TO_RAD / headBones.size();
        float pitch = Mth.clamp(state.xRot, -limit, limit) * Mth.DEG_TO_RAD / headBones.size();

        boolean yawOnZ = this.species.headYawAxis() == Direction.Axis.Z;

        for (String bone : headBones) {
            boneSnapshots.ifPresent(bone, snapshot -> {
                snapshot.setRotX(snapshot.getRotX() + pitch);
                if (yawOnZ) {
                    snapshot.setRotZ(snapshot.getRotZ() + yaw);
                } else {
                    snapshot.setRotY(snapshot.getRotY() + yaw);
                }
            });
        }
    }

    @Override
    public void extractRenderState(CompanionEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        if (entity.isTame() && KindredConfig.CLIENT.showLevelInName.get()) {
            int stars = entity.getStars();
            state.nameTag = stars > 0
                    ? Component.translatable("entity.kindredspirits.name_with_stars",
                            entity.getDisplayName(), entity.getLevel(), CharmView.starText(stars))
                            .withStyle(stars >= entity.configuredMaxStars() ? ChatFormatting.GOLD : ChatFormatting.WHITE)
                    : Component.translatable("entity.kindredspirits.name_with_level",
                            entity.getDisplayName(), entity.getLevel());
        }
    }
}
