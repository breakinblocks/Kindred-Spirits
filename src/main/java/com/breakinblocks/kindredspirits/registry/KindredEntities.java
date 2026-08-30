package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class KindredEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KindredSpirits.MOD_ID);

    private static final Map<CompanionSpecies, DeferredHolder<EntityType<?>, EntityType<CompanionEntity>>> BY_SPECIES =
            new EnumMap<>(CompanionSpecies.class);

    static {
        for (CompanionSpecies species : CompanionSpecies.values()) {
            BY_SPECIES.put(species, ENTITY_TYPES.register(species.getSerializedName(), registryName -> {
                EntityType.Builder<CompanionEntity> builder = EntityType.Builder
                        .<CompanionEntity>of((type, level) -> new CompanionEntity(type, level, species), MobCategory.CREATURE)
                        .sized(species.width(), species.height())
                        .clientTrackingRange(10)
                        .updateInterval(2);
                if (species.immunities().fireImmune()) {
                    builder.fireImmune();
                }
                return builder.build(ResourceKey.create(Registries.ENTITY_TYPE, registryName));
            }));
        }
    }

    public static EntityType<CompanionEntity> type(CompanionSpecies species) {
        return BY_SPECIES.get(species).get();
    }

    private KindredEntities() {
    }
}
