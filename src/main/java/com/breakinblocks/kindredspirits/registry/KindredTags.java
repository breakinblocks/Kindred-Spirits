package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class KindredTags {
    public static final TagKey<Item> COMPANION_FOOD = TagKey.create(Registries.ITEM, KindredSpirits.id("companion_food"));

    private KindredTags() {
    }
}
