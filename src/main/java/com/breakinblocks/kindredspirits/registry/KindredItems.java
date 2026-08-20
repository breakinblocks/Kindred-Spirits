package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class KindredItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KindredSpirits.MOD_ID);

    public static final DeferredItem<KindredCharmItem> KINDRED_CHARM =
            ITEMS.registerItem("kindred_charm", props -> new KindredCharmItem(props.stacksTo(1)));

    private static final Map<CompanionSpecies, DeferredItem<SpawnEggItem>> SPAWN_EGGS =
            new EnumMap<>(CompanionSpecies.class);

    static {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            SPAWN_EGGS.put(species, ITEMS.registerItem(species.getSerializedName() + "_spawn_egg",
                    props -> new SpawnEggItem(props.spawnEgg(KindredEntities.type(species)))));
        }
    }

    public static Map<CompanionSpecies, DeferredItem<SpawnEggItem>> spawnEggs() {
        return Collections.unmodifiableMap(SPAWN_EGGS);
    }

    private KindredItems() {
    }
}
