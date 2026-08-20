package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAnimations;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
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

    public static final CompanionAbility SHADOW_BALL = register(new ShadowBallAbility(KindredSpirits.id("shadow_ball")));

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

    private record ShadowBallAbility(Identifier id) implements CompanionAbility {
        private static final double RANGE_SQR = 256.0;
        private static final int TRAIL_STEPS = 12;

        @Override
        public int intervalTicks() {
            return 60;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)) {
                return;
            }

            LivingEntity target = companion.getTarget();
            if (target == null || !target.isAlive()
                    || companion.distanceToSqr(target) > RANGE_SQR
                    || !companion.hasLineOfSight(target)) {
                return;
            }

            companion.playCompanionAnim(CompanionAnimations.SPECIAL_ATTACK);
            companion.playSound(companion.species().sounds().specialAttack(), 0.8f, 1.6f);

            Vec3 from = companion.getEyePosition();
            Vec3 step = target.getBoundingBox().getCenter().subtract(from).scale(1.0 / TRAIL_STEPS);
            for (int i = 1; i <= TRAIL_STEPS; i++) {
                Vec3 point = from.add(step.scale(i));
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 2, 0.06, 0.06, 0.06, 0.0);
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(0.5), target.getZ(),
                    10, 0.25, 0.25, 0.25, 0.02);

            target.hurtServer(level, companion.damageSources().indirectMagic(companion, companion.getOwner()),
                    (float) companion.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5f);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, true, true));
        }
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
