package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.companion.CompanionLevels.AttributeBonus;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.item.KindredEquipmentItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class KindredItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KindredSpirits.MOD_ID);

    public static final DeferredItem<KindredCharmItem> KINDRED_CHARM =
            ITEMS.registerItem("kindred_charm", props -> new KindredCharmItem(props.stacksTo(1)));

    public static final DeferredItem<BlockItem> TREX_EGG =
            ITEMS.registerSimpleBlockItem("trex_egg", KindredBlocks.TREX_EGG);

    public static final DeferredItem<KindredEquipmentItem> BOXING_GLOVES = equipment("boxing_gloves", List.of(
            new AttributeBonus(Attributes.ATTACK_DAMAGE, new AttributeModifier(KindredSpirits.id("boxing_gloves"),
                    2.0, AttributeModifier.Operation.ADD_VALUE)),
            new AttributeBonus(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(KindredSpirits.id("boxing_gloves"),
                    3.0, AttributeModifier.Operation.ADD_VALUE))));

    public static final DeferredItem<KindredEquipmentItem> DRAGON_TABLET = equipment("dragon_tablet", List.of());

    public static final DeferredItem<KindredEquipmentItem> RUNNING_SHOES = equipment("running_shoes", List.of(
            new AttributeBonus(Attributes.MOVEMENT_SPEED, new AttributeModifier(KindredSpirits.id("running_shoes"),
                    0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))));

    public static final DeferredItem<KindredEquipmentItem> BATTERY = equipment("battery", List.of());

    private static final List<DeferredItem<KindredEquipmentItem>> EQUIPMENT =
            List.of(BOXING_GLOVES, DRAGON_TABLET, RUNNING_SHOES, BATTERY);

    private static final Map<CompanionSpecies, DeferredItem<SpawnEggItem>> SPAWN_EGGS =
            new EnumMap<>(CompanionSpecies.class);

    static {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            SPAWN_EGGS.put(species, ITEMS.registerItem(species.getSerializedName() + "_spawn_egg",
                    props -> new SpawnEggItem(props.spawnEgg(KindredEntities.type(species)))));
        }
    }

    private static DeferredItem<KindredEquipmentItem> equipment(String name, List<AttributeBonus> bonuses) {
        return ITEMS.registerItem(name, props -> new KindredEquipmentItem(props.stacksTo(1).rarity(Rarity.UNCOMMON), bonuses));
    }

    public static List<DeferredItem<KindredEquipmentItem>> equipment() {
        return EQUIPMENT;
    }

    public static Map<CompanionSpecies, DeferredItem<SpawnEggItem>> spawnEggs() {
        return Collections.unmodifiableMap(SPAWN_EGGS);
    }

    private KindredItems() {
    }
}
