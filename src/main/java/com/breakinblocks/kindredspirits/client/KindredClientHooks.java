package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.client.screen.KindredCharmScreen;
import com.breakinblocks.kindredspirits.net.CharmView;
import net.minecraft.client.Minecraft;

public final class KindredClientHooks {

    public static void acceptCharmView(CharmView view, boolean open) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof KindredCharmScreen screen) {
            screen.updateView(view);
            return;
        }

        if (open) {
            minecraft.setScreen(new KindredCharmScreen(view));
        }
    }

    private KindredClientHooks() {
    }
}
