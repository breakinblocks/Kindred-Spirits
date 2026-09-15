package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KindredCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KindredSpirits.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KINDRED_TAB =
            CREATIVE_TABS.register("kindredspirits", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kindredspirits"))
                    .icon(() -> new ItemStack(KindredItems.KINDRED_CHARM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(KindredItems.KINDRED_CHARM.get());
                        output.accept(KindredItems.TREX_EGG.get());
                        output.accept(KindredItems.GOLDEN_BONE.get());
                        KindredItems.equipment().forEach(item -> output.accept(item.get()));
                        KindredItems.spawnEggs().values().forEach(egg -> output.accept(egg.get()));
                    })
                    .build());

    private KindredCreativeTabs() {
    }
}
