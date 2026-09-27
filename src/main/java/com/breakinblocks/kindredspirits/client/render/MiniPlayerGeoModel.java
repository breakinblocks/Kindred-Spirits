package com.breakinblocks.kindredspirits.client.render;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.client.KindredSkins;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class MiniPlayerGeoModel extends DefaultedEntityGeoModel<CompanionEntity> {
    public static final DataTicket<Identifier> SKIN_TEXTURE =
            DataTicket.create("kindredspirits:skin_texture", Identifier.class);
    public static final DataTicket<Boolean> SLIM_ARMS = DataTicket.create("kindredspirits:slim_arms", Boolean.class);

    private final Identifier slimModel;

    public MiniPlayerGeoModel(Identifier assetSubpath) {
        super(assetSubpath);
        this.slimModel = this.buildFormattedModelPath(KindredSpirits.id(assetSubpath.getPath() + "_slim"));
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(SLIM_ARMS, false)
                ? this.slimModel
                : super.getModelResource(renderState);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(
                SKIN_TEXTURE, KindredSkins.steve().body().texturePath());
    }
}
