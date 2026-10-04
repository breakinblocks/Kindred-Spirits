package com.breakinblocks.kindredspirits.integration.curios;

import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

public final class KindredCurios {
    public static final String MOD_ID = "curios";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean wearsCharm(Player player) {
        return isLoaded() && Lookup.wearsCharm(player);
    }

    private static final class Lookup {
        private static boolean wearsCharm(Player player) {
            return CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.isEquipped(stack -> stack.getItem() instanceof KindredCharmItem))
                    .orElse(false);
        }
    }

    private KindredCurios() {}
}
