package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.KindredSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
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
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
