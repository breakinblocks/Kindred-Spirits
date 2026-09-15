package com.breakinblocks.kindredspirits.integration.jei;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

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
        registration.addIngredientInfo(KindredItems.TREX_EGG.get(),
                Component.translatable("jei.kindredspirits.trex_egg"));
        registration.addIngredientInfo(KindredItems.GOLDEN_BONE.get(),
                Component.translatable("jei.kindredspirits.golden_bone"));
        registration.addIngredientInfo(Items.WOLF_ARMOR,
                Component.translatable("jei.kindredspirits.wolf_armor_direwolf"));
        KindredItems.equipment().forEach(item -> registration.addIngredientInfo(item.get(),
                Component.translatable("jei.kindredspirits." + item.getId().getPath())));
    }
}
