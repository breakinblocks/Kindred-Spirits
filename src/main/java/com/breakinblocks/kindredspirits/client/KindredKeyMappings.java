package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class KindredKeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(KindredSpirits.id("companion"));

    public static final KeyMapping CYCLE_COMMAND =
            new KeyMapping("key.kindredspirits.cycle_command", InputConstants.KEY_G, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(CYCLE_COMMAND);
    }

    private KindredKeyMappings() {}
}
