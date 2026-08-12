package com.breakinblocks.kindredspirits.integration.jei;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class KindredJeiPlugin implements IModPlugin {
    private static final Identifier UID = KindredSpirits.id("jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(KindredItems.KINDRED_CHARM.get(),
                Component.translatable("jei.kindredspirits.kindred_charm"));
        registration.addIngredientInfo(KindredItems.SPIRIT_TREAT.get(),
                Component.translatable("jei.kindredspirits.spirit_treat"));
    }
}
