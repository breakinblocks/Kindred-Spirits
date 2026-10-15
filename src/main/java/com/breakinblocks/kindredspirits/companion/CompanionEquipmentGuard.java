package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionEquipmentGuard {
    private static final String COPY_KEY = KindredSpirits.MOD_ID + ":spirit_copy";

    public static ItemStack mark(ItemStack stack) {
        if (!stack.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(COPY_KEY, true));
        }
        return stack;
    }

    public static boolean isCopy(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .contains(COPY_KEY);
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            EntityType<CompanionEntity> type = KindredEntities.type(species);
            event.registerEntity(Capabilities.ItemHandler.ENTITY, type, (entity, ctx) -> EmptyItemHandler.INSTANCE);
            event.registerEntity(
                    Capabilities.ItemHandler.ENTITY_AUTOMATION, type, (entity, ctx) -> EmptyItemHandler.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof ItemEntity item
                && isCopy(item.getItem())) {
            event.setCanceled(true);
        }
    }

    private CompanionEquipmentGuard() {}
}
