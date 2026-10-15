package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.KindredSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class MiniPlayerRenderer extends CompanionRenderer {
    private static final String RIGHT_HAND_BONE = "right_hand";
    private static final String LEFT_HAND_BONE = "left_hand";
    private static final DataTicket<Boolean> BABY_BODY = DataTicket.create("kindredspirits:baby_body", Boolean.class);
    private static final float BABY_SCALE = 0.5f;
    private static final float BABY_BODY_DROP = 12.0f;
    private static final float BABY_LEG_DROP = 6.0f;
    private static final float BABY_LEG_INSET = 0.95f;

    public MiniPlayerRenderer(EntityRendererProvider.Context context, CompanionSpecies species) {
        super(context, species, new MiniPlayerGeoModel(KindredSpirits.id(species.getSerializedName())));
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this, RIGHT_HAND_BONE, LEFT_HAND_BONE));
    }

    @Override
    public void addRenderData(
            CompanionEntity entity, Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        super.addRenderData(entity, relatedObject, renderState, partialTick);

        PlayerSkin skin = KindredSkins.skinFor(entity);
        renderState.addGeckolibData(MiniPlayerGeoModel.SKIN_TEXTURE, skin.body().texturePath());
        renderState.addGeckolibData(MiniPlayerGeoModel.SLIM_ARMS, skin.model() == PlayerModelType.SLIM);
        renderState.addGeckolibData(BABY_BODY, entity.isBabyBody());
    }

    @Override
    public void adjustModelBonesForRender(
            RenderPassInfo<LivingEntityRenderState> renderPass, BoneSnapshots boneSnapshots) {
        super.adjustModelBonesForRender(renderPass, boneSnapshots);

        if (!renderPass.renderState().getOrDefaultGeckolibData(BABY_BODY, false)) {
            return;
        }

        boneSnapshots.ifPresent("root", root -> root.setTranslateY(root.getTranslateY() * BABY_SCALE));
        boneSnapshots.ifPresent(
                "body",
                body -> body.setTranslateY(body.getTranslateY() - BABY_BODY_DROP)
                        .setScale(BABY_SCALE, BABY_SCALE, BABY_SCALE));
        boneSnapshots.ifPresent("head", head -> head.setScale(1.0f / BABY_SCALE, 1.0f / BABY_SCALE, 1.0f / BABY_SCALE));
        boneSnapshots.ifPresent("right_leg", leg -> this.shrinkLeg(leg, BABY_LEG_INSET));
        boneSnapshots.ifPresent("left_leg", leg -> this.shrinkLeg(leg, -BABY_LEG_INSET));
    }

    private void shrinkLeg(BoneSnapshot leg, float inset) {
        leg.setTranslation(leg.getTranslateX() + inset, leg.getTranslateY() - BABY_LEG_DROP, leg.getTranslateZ())
                .setScale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
