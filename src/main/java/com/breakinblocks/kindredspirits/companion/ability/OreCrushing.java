package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.registry.KindredTags;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public final class OreCrushing {
    private static final String COMMON = "c";
    private static final List<String> ORE_PREFIXES = List.of("raw_materials/", "ores/");
    private static final String RAW_MATERIALS = "raw_materials/";
    private static final String DUSTS = "dusts/";

    public static int crushAround(ServerLevel level, Entity crusher, double radius, int dustPerOre) {
        return crushAround(level, crusher, radius, dustPerOre, OreCrushing::registryItems);
    }

    public static int crushAround(ServerLevel level, Entity crusher, double radius, int dustPerOre,
                                  Function<TagKey<Item>, List<Item>> itemTags) {
        int crushed = 0;
        double radiusSqr = radius * radius;
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, crusher.getBoundingBox().inflate(radius),
                item -> item.isAlive() && item.distanceToSqr(crusher) <= radiusSqr);
        for (ItemEntity item : items) {
            ItemStack ore = item.getItem();
            Item dust = dustFor(ore, itemTags);
            if (dust == null) {
                continue;
            }

            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ore.getItem()), item.getX(), item.getY() + 0.2, item.getZ(),
                    8, 0.15, 0.1, 0.15, 0.05);
            int total = ore.getCount() * dustPerOre;
            int stackSize = new ItemStack(dust).getMaxStackSize();
            item.setItem(new ItemStack(dust, Math.min(total, stackSize)));
            for (int left = total - stackSize; left > 0; left -= stackSize) {
                ItemEntity extra = new ItemEntity(level, item.getX(), item.getY(), item.getZ(),
                        new ItemStack(dust, Math.min(left, stackSize)), 0.0, 0.1, 0.0);
                extra.setDefaultPickUpDelay();
                level.addFreshEntity(extra);
            }
            crushed += ore.getCount();
        }
        return crushed;
    }

    public static @Nullable Item dustFor(ItemStack ore, Function<TagKey<Item>, List<Item>> itemTags) {
        if (ore.isEmpty() || ore.is(KindredTags.CRUSHING_BLACKLIST)) {
            return null;
        }

        String namespace = BuiltInRegistries.ITEM.getKey(ore.getItem()).getNamespace();
        return ore.typeHolder().tags()
                .map(TagKey::location)
                .filter(tag -> tag.getNamespace().equals(COMMON))
                .map(tag -> material(tag.getPath()))
                .filter(material -> material != null && !itemTags.apply(itemTag(RAW_MATERIALS + material)).isEmpty())
                .map(material -> itemTags.apply(itemTag(DUSTS + material)))
                .filter(dusts -> !dusts.isEmpty())
                .findFirst()
                .map(dusts -> preferred(dusts, namespace))
                .orElse(null);
    }

    public static List<Item> registryItems(TagKey<Item> tag) {
        return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tag).spliterator(), false)
                .map(Holder::value)
                .toList();
    }

    private static @Nullable String material(String path) {
        return ORE_PREFIXES.stream()
                .filter(path::startsWith)
                .map(prefix -> path.substring(prefix.length()))
                .findFirst()
                .orElse(null);
    }

    private static Item preferred(List<Item> dusts, String namespace) {
        return dusts.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(namespace))
                .findFirst()
                .orElse(dusts.getFirst());
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(COMMON, path));
    }

    private OreCrushing() {
    }
}
