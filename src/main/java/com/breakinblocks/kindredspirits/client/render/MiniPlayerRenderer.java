package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class MiniPlayerRenderer extends CompanionRenderer {
    private static final String RIGHT_HAND_BONE = "right_hand";
    private static final String LEFT_HAND_BONE = "left_hand";

    public MiniPlayerRenderer(EntityRendererProvider.Context context, CompanionSpecies species) {
        super(context, species, new MiniPlayerGeoModel(KindredSpirits.id(species.getSerializedName())));
        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected @Nullable ItemStack getStackForBone(GeoBone bone, CompanionEntity companion) {
                return switch (bone.getName()) {
                    case RIGHT_HAND_BONE -> handItem(companion, HumanoidArm.RIGHT);
                    case LEFT_HAND_BONE -> handItem(companion, HumanoidArm.LEFT);
                    default -> null;
                };
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(
                    GeoBone bone, ItemStack stack, CompanionEntity companion) {
                return bone.getName().equals(LEFT_HAND_BONE)
                        ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(
                    PoseStack poseStack,
                    GeoBone bone,
                    ItemStack stack,
                    CompanionEntity companion,
                    MultiBufferSource bufferSource,
                    float partialTick,
                    int packedLight,
                    int packedOverlay) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0f));
                poseStack.translate(0.0f, 0.125f, -0.0625f);
                super.renderStackForBone(
                        poseStack, bone, stack, companion, bufferSource, partialTick, packedLight, packedOverlay);
                poseStack.popPose();
            }
        });
    }

    private static @Nullable ItemStack handItem(CompanionEntity companion, HumanoidArm arm) {
        ItemStack stack = companion.getMainArm() == arm ? companion.getMainHandItem() : companion.getOffhandItem();
        return stack.isEmpty() ? null : stack;
    }

    @Override
    public RenderType getRenderType(
            CompanionEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
