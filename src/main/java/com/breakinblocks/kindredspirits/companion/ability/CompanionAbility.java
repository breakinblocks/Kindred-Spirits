package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public interface CompanionAbility {
    Identifier id();

    default Component displayName() {
        return Component.translatable("ability." + id().getNamespace() + "." + id().getPath());
    }

    default Component description() {
        return Component.translatable("ability." + id().getNamespace() + "." + id().getPath() + ".desc");
    }

    default int intervalTicks() {
        return 20;
    }

    void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner);
}
