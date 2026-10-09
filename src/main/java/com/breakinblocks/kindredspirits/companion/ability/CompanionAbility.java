package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionLevels.AttributeBonus;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface CompanionAbility {
    ResourceLocation id();

    default Component displayName() {
        return Component.translatable("ability." + id().getNamespace() + "." + id().getPath());
    }

    default Component description() {
        return Component.translatable("ability." + id().getNamespace() + "." + id().getPath() + ".desc");
    }

    default int intervalTicks() {
        return 20;
    }

    default boolean scalesWithBond() {
        return true;
    }

    default List<AttributeBonus> attributeBonuses() {
        return List.of();
    }

    default boolean isActive() {
        return false;
    }

    default boolean activate(CompanionEntity companion, ServerPlayer owner) {
        return false;
    }

    void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner);
}
