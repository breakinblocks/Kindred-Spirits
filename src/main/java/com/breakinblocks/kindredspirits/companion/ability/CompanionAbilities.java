package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CompanionAbilities {
    private static final Map<Identifier, CompanionAbility> REGISTRY = new LinkedHashMap<>();

    public static final CompanionAbility SWIFT_STEP = register(new SimpleAbility(KindredSpirits.id("swift_step"), 40,
            (companion, owner) -> {
                if (owner != null && companion.distanceToSqr(owner) < 144.0) {
                    owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
                }
            }));

    public static final CompanionAbility MENDING_PRESENCE = register(new SimpleAbility(KindredSpirits.id("mending_presence"), 200,
            (companion, owner) -> {
                if (owner != null && owner.getHealth() < owner.getMaxHealth() && companion.distanceToSqr(owner) < 64.0) {
                    owner.heal(1.0f);
                }
            }));

    public static final CompanionAbility EMBER_WARD = register(new SimpleAbility(KindredSpirits.id("ember_ward"), 40,
            (companion, owner) -> {
                if (owner != null && owner.isOnFire() && companion.distanceToSqr(owner) < 100.0) {
                    owner.clearFire();
                }
            }));

    public static final CompanionAbility KINDLED_VIGOUR = register(new SimpleAbility(KindredSpirits.id("kindled_vigour"), 100,
            (companion, owner) -> {
                if (companion.getHealth() < companion.getMaxHealth()) {
                    companion.heal(1.0f);
                }
            }));

    public static CompanionAbility register(CompanionAbility ability) {
        REGISTRY.put(ability.id(), ability);
        return ability;
    }

    public static @Nullable CompanionAbility get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Map<Identifier, CompanionAbility> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private record SimpleAbility(Identifier id, int intervalTicks, Effect effect) implements CompanionAbility {
        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            this.effect.apply(companion, owner);
        }
    }

    @FunctionalInterface
    private interface Effect {
        void apply(CompanionEntity companion, @Nullable ServerPlayer owner);
    }

    private CompanionAbilities() {
    }
}
