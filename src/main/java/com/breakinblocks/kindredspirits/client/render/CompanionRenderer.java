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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class CompanionRenderer extends GeoEntityRenderer<CompanionEntity, LivingEntityRenderState> {
    private final CompanionSpecies species;

    public CompanionRenderer(EntityRendererProvider.Context context, CompanionSpecies species) {
        this(context, species, new DefaultedEntityGeoModel<CompanionEntity>(
                KindredSpirits.id(species.getSerializedName())));
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    protected CompanionRenderer(EntityRendererProvider.Context context, CompanionSpecies species,
                                GeoModel<CompanionEntity> model) {
        super(context, model);
        this.species = species;
        this.withScale(species.renderScale());
        this.shadowRadius = species.width() * 0.6f;
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

        for (String bone : headBones) {
            boneSnapshots.ifPresent(bone, snapshot -> snapshot
                    .setRotY(snapshot.getRotY() + yaw)
                    .setRotX(snapshot.getRotX() + pitch));
        }
    }

    @Override
    public void extractRenderState(CompanionEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        if (entity.isTame() && KindredConfig.CLIENT.showLevelInName.get()) {
            state.nameTag = Component.translatable("entity.kindredspirits.name_with_level",
                    entity.getDisplayName(), entity.getLevel());
        }
    }
}
