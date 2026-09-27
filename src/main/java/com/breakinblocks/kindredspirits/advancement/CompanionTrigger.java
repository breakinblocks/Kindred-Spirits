package com.breakinblocks.kindredspirits.advancement;

import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

public class CompanionTrigger extends SimpleCriterionTrigger<CompanionTrigger.TriggerInstance> {
    private static final Codec<CompanionSpecies> SPECIES_CODEC = StringRepresentable.fromEnum(CompanionSpecies::values);

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, CompanionSpecies species, Event event, int value) {
        this.trigger(player, instance -> instance.matches(species, event, value));
    }

    public enum Event implements StringRepresentable {
        BONDED("bonded"),
        LEVEL("level"),
        BOND_LEVEL("bond_level"),
        STARS("stars"),
        REVIVED("revived"),
        EQUIPPED("equipped"),
        DYED("dyed"),
        STORAGE_OPENED("storage_opened");

        public static final Codec<Event> CODEC = StringRepresentable.fromEnum(Event::values);

        private final String name;

        Event(String name) {
            this.name = name;
        }

        public int cap() {
            return switch (this) {
                case LEVEL -> CompanionLevels.maxLevel();
                case BOND_LEVEL -> CompanionBondMath.maxLevel();
                case STARS -> CompanionLevels.maxStars();
                default -> Integer.MAX_VALUE;
            };
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            Event event,
            Optional<CompanionSpecies> species,
            MinMaxBounds.Ints value,
            Optional<Boolean> atMax)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
                        EntityPredicate.ADVANCEMENT_CODEC
                                .optionalFieldOf("player")
                                .forGetter(TriggerInstance::player),
                        Event.CODEC.fieldOf("event").forGetter(TriggerInstance::event),
                        SPECIES_CODEC.optionalFieldOf("species").forGetter(TriggerInstance::species),
                        MinMaxBounds.Ints.CODEC
                                .optionalFieldOf("value", MinMaxBounds.Ints.ANY)
                                .forGetter(TriggerInstance::value),
                        Codec.BOOL.optionalFieldOf("at_max").forGetter(TriggerInstance::atMax))
                .apply(i, TriggerInstance::new));

        public boolean matches(CompanionSpecies species, Event event, int value) {
            return this.event == event
                    && this.species.map(wanted -> wanted == species).orElse(true)
                    && this.value.matches(value)
                    && this.atMax
                            .map(wanted -> wanted == (value >= event.cap()))
                            .orElse(true);
        }
    }
}
