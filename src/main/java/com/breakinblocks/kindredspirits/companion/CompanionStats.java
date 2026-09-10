package com.breakinblocks.kindredspirits.companion;

import net.minecraft.world.entity.EquipmentSlot;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.companion.CompanionLevels.AttributeBonus;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.item.KindredEquipmentItem;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.function.ToDoubleFunction;

public record CompanionStats(List<Float> base, List<Float> modified,
                             float experienceMultiplier, int storageSlots, int storageMax, int reviveSeconds) {
    public static final List<Holder<Attribute>> ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED,
            Attributes.KNOCKBACK_RESISTANCE, Attributes.ENTITY_INTERACTION_RANGE);

    public static final List<String> KEYS = List.of("health", "attack", "armour", "speed", "knockback", "reach");

    public static final CompanionStats EMPTY = new CompanionStats(
            List.of(0f, 0f, 0f, 0f, 0f, 0f), List.of(0f, 0f, 0f, 0f, 0f, 0f), 1.0f, 0, 0, 0);

    public static final StreamCodec<FriendlyByteBuf, CompanionStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), CompanionStats::base,
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), CompanionStats::modified,
            ByteBufCodecs.FLOAT, CompanionStats::experienceMultiplier,
            ByteBufCodecs.VAR_INT, CompanionStats::storageSlots,
            ByteBufCodecs.VAR_INT, CompanionStats::storageMax,
            ByteBufCodecs.VAR_INT, CompanionStats::reviveSeconds,
            CompanionStats::new);

    public static CompanionStats of(CompanionEntity companion) {
        return build(companion.species(), companion.getBondLevel(), companion::getAttributeValue);
    }

    public static CompanionStats of(CompanionSpecies species, CompanionSnapshot snapshot) {
        AttributeMap attributes = new AttributeMap(CompanionEntity.createCompanionAttributes(species).build());
        int level = CompanionLevels.clampLevel(snapshot.level());
        int stars = CompanionLevels.clampStars(snapshot.stars());

        CompanionLevels.applyBaseStats(attributes, species, level, stars);
        apply(attributes, CompanionLevels.starBonusesAt(stars));
        for (CompanionAbility ability : KindredConfig.COMMON.abilitiesEnabled.get()
                ? snapshot.activeAbilities(species) : List.<CompanionAbility>of()) {
            apply(attributes, ability.attributeBonuses());
        }
        if (snapshot.equipment().getItem() instanceof KindredEquipmentItem item) {
            apply(attributes, item.bonuses());
        }

        snapshot.equipment().forEachModifier(EquipmentSlot.CHEST, (attribute, modifier) -> {
            var instance = attributes.getInstance(attribute);
            if (instance != null) instance.addOrReplacePermanentModifier(modifier);
        });
        return build(species, snapshot.bondLevel(), attributes::getValue);
    }

    private static CompanionStats build(CompanionSpecies species, int bondLevel,
                                        ToDoubleFunction<Holder<Attribute>> value) {
        AttributeSupplier defaults = CompanionEntity.createCompanionAttributes(species).build();
        List<Float> base = ATTRIBUTES.stream().map(attribute -> (float) defaults.getBaseValue(attribute)).toList();
        List<Float> modified = ATTRIBUTES.stream().map(attribute -> (float) value.applyAsDouble(attribute)).toList();

        return new CompanionStats(base, modified,
                (float) CompanionBondMath.experienceMultiplier(bondLevel),
                CompanionLevels.storageSlots(species, bondLevel),
                species.storageSlots(),
                CompanionBondMath.reviveCooldownTicks(bondLevel) / 20);
    }

    private static void apply(AttributeMap attributes, List<AttributeBonus> bonuses) {
        CompanionLevels.applyBonuses(attributes, bonuses, bonuses, true);
    }
}
