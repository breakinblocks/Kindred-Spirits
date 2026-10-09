package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAnimations;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.net.CharmView;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class CompanionRenderer extends GeoEntityRenderer<CompanionEntity> {
    private final CompanionSpecies species;

    public CompanionRenderer(EntityRendererProvider.Context context, CompanionSpecies species) {
        this(context, species, modelFor(species));
        this.addRenderLayer(new DyeMaskGeoLayer(this));
        if (species.hasGlowMask()) this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private static GeoModel<CompanionEntity> modelFor(CompanionSpecies species) {
        return switch (species) {
            case QUOKKA -> new EquippedVariantGeoModel(species, KindredItems.QUOKKA_SNACK, "quokka_snack");
            case GREMLIN -> new EquippedVariantGeoModel(species, KindredItems.BATTERY, "gremlin_battery");
            case NIGHTFOX -> new EquippedVariantGeoModel(species, KindredItems.RUNNING_SHOES, "nightfox_boots");
            case TREX -> new EquippedVariantGeoModel(species, KindredItems.BOXING_GLOVES, "trex_gloves");
            case BABY_DRAGON -> new EquippedVariantGeoModel(species, KindredItems.DRAGON_TABLET, "baby_dragon_tablet");
            default -> new CompanionGeoModel(KindredSpirits.id(species.getSerializedName()));
        };
    }

    protected CompanionRenderer(
            EntityRendererProvider.Context context, CompanionSpecies species, GeoModel<CompanionEntity> model) {
        super(context, model);
        this.species = species;
        this.withScale(species.renderScale());
        this.shadowRadius = species.width() * 0.6f;
    }

    @Override
    public void scaleModelForRender(
            float widthScale,
            float heightScale,
            PoseStack poseStack,
            CompanionEntity animatable,
            BakedGeoModel model,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay) {
        float ageScale = animatable.getAgeScale();
        super.scaleModelForRender(
                widthScale * ageScale,
                heightScale * ageScale,
                poseStack,
                animatable,
                model,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay);
    }

    @Override
    protected float getDeathMaxRotation(CompanionEntity animatable) {
        return this.species.hasAnimation(CompanionAnimations.DEATH) ? 0.0f : super.getDeathMaxRotation(animatable);
    }

    @Override
    public boolean shouldShowName(CompanionEntity entity) {
        return (entity.isTame() && KindredConfig.CLIENT.showLevelInName.get()) || super.shouldShowName(entity);
    }

    @Override
    protected void renderNameTag(
            CompanionEntity entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick) {
        super.renderNameTag(
                entity, this.nameTag(entity, displayName), poseStack, bufferSource, packedLight, partialTick);
    }

    private Component nameTag(CompanionEntity entity, Component displayName) {
        if (!entity.isTame() || !KindredConfig.CLIENT.showLevelInName.get()) {
            return displayName;
        }

        int stars = entity.getStars();
        return stars > 0
                ? Component.translatable(
                                "entity.kindredspirits.name_with_stars",
                                entity.getDisplayName(),
                                entity.getLevel(),
                                CharmView.starText(stars))
                        .withStyle(stars >= entity.configuredMaxStars() ? ChatFormatting.GOLD : ChatFormatting.WHITE)
                : Component.translatable(
                        "entity.kindredspirits.name_with_level", entity.getDisplayName(), entity.getLevel());
    }
}
