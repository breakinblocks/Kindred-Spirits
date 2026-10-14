package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.client.screen.CompanionAbilitiesScreen;
import com.breakinblocks.kindredspirits.client.screen.KindredCharmScreen;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.net.CharmView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

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

    public static void openAbilities(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(entityId) instanceof CompanionEntity companion) {
            minecraft.setScreen(new CompanionAbilitiesScreen(companion));
        }
    }

    public static void acceptOreReveal(List<BlockPos> ores, int ticks) {
        OreRevealOverlay.show(ores, ticks);
        ScreenShake.start();
    }

    public static void acceptPackGlow(List<Integer> entityIds, int ticks) {
        PackGlowOverlay.show(entityIds, ticks);
    }

    private KindredClientHooks() {}
}
