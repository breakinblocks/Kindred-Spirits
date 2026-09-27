package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class KindredTags {
    public static final TagKey<Item> COMPANION_FOOD =
            TagKey.create(Registries.ITEM, KindredSpirits.id("companion_food"));

    public static final TagKey<Item> COMPANION_HEALING =
            TagKey.create(Registries.ITEM, KindredSpirits.id("companion_healing"));

    public static final TagKey<Item> CRUSHING_BLACKLIST =
            TagKey.create(Registries.ITEM, KindredSpirits.id("crushing_blacklist"));

    public static final TagKey<EntityType<?>> DANGEROUS_PREY =
            TagKey.create(Registries.ENTITY_TYPE, KindredSpirits.id("dangerous_prey"));

    public static final TagKey<Block> DIREWOLF_DIGGABLE =
            TagKey.create(Registries.BLOCK, KindredSpirits.id("direwolf_diggable"));

    private KindredTags() {}
}
