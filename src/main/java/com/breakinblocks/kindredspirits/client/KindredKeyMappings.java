package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class KindredKeyMappings {
    public static final String CATEGORY = "key.category." + KindredSpirits.MOD_ID + ".companion";

    public static final KeyMapping CYCLE_COMMAND =
            new KeyMapping("key.kindredspirits.cycle_command", InputConstants.KEY_G, CATEGORY);

    public static final KeyMapping OPEN_CHARM =
            new KeyMapping("key.kindredspirits.open_charm", InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_COMMAND);
        event.register(OPEN_CHARM);
    }

    private KindredKeyMappings() {}
}
