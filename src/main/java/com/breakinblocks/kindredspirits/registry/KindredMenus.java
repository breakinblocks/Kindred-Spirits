package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.menu.KindredStorageMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KindredMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KindredSpirits.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<KindredStorageMenu>> COMPANION_STORAGE =
            MENUS.register("companion_storage", () -> IMenuTypeExtension.create(KindredStorageMenu::client));

    private KindredMenus() {}
}
