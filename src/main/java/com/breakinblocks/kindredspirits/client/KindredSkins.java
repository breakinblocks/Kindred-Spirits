package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class KindredSkins {
    private static final PlayerSkin STEVE = new PlayerSkin(
            new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace("entity/player/wide/steve")),
            null, null, PlayerModelType.WIDE, true);

    public static PlayerSkin steve() {
        return STEVE;
    }

    public static PlayerSkin skinFor(CompanionEntity companion) {
        String chosen = companion.getSkinName();
        UUID owner = ownerId(companion);

        if (chosen.isEmpty() && owner == null) {
            return STEVE;
        }

        if (!KindredConfig.CLIENT.resolveCompanionSkins.get()) {
            return owner == null ? STEVE : DefaultPlayerSkin.get(owner);
        }

        ResolvableProfile profile = chosen.isEmpty()
                ? ResolvableProfile.createUnresolved(owner)
                : ResolvableProfile.createUnresolved(chosen);

        return Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).playerSkin();
    }

    private static @Nullable UUID ownerId(CompanionEntity companion) {
        EntityReference<LivingEntity> owner = companion.getOwnerReference();
        return owner == null ? null : owner.getUUID();
    }

    private KindredSkins() {
    }
}
