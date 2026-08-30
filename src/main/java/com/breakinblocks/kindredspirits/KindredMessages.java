package com.breakinblocks.kindredspirits;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class KindredMessages {
    public static void send(Player player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.kindredspirits." + key, args));
    }

    private KindredMessages() {
    }
}
