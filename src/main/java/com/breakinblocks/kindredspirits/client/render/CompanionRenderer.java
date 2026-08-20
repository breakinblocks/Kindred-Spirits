package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;

public class CompanionRenderer extends GeoEntityRenderer<CompanionEntity, LivingEntityRenderState> {
    public CompanionRenderer(EntityRendererProvider.Context context, CompanionSpecies species) {
        super(context, new DefaultedEntityGeoModel<CompanionEntity>(KindredSpirits.id(species.getSerializedName())));
        this.withScale(species.renderScale());
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = species.width() * 0.6f;
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
