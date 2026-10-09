package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.mojang.authlib.GameProfile;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class KindredSkins {
    private static final PlayerSkin STEVE = new PlayerSkin(
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"),
            null,
            null,
            null,
            PlayerSkin.Model.WIDE,
            true);

    private static final Map<String, CompletableFuture<Optional<GameProfile>>> PROFILES = new ConcurrentHashMap<>();

    public static PlayerSkin steve() {
        return STEVE;
    }

    public static PlayerSkin skinFor(CompanionEntity companion) {
        String chosen = companion.getSkinName();
        UUID owner = companion.getOwnerUUID();

        if (chosen.isEmpty() && owner == null) {
            return STEVE;
        }

        PlayerSkin fallback = owner == null ? STEVE : DefaultPlayerSkin.get(owner);
        if (!KindredConfig.CLIENT.resolveCompanionSkins.get()) {
            return fallback;
        }

        return profile(chosen, owner)
                .getNow(Optional.empty())
                .map(profile -> Minecraft.getInstance().getSkinManager().getInsecureSkin(profile))
                .orElse(fallback);
    }

    private static CompletableFuture<Optional<GameProfile>> profile(String chosen, @Nullable UUID owner) {
        if (chosen.isEmpty()) {
            return PROFILES.computeIfAbsent(owner.toString(), key -> SkullBlockEntity.fetchGameProfile(owner));
        }
        return PROFILES.computeIfAbsent(
                chosen.toLowerCase(Locale.ROOT), key -> SkullBlockEntity.fetchGameProfile(chosen));
    }

    private KindredSkins() {}
}
