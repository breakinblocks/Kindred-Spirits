package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAnimations;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    public static final CompanionAbility NIGHT_WARD = register(new SimpleAbility(KindredSpirits.id("night_ward"), 40,
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

    public static final CompanionAbility SAVAGE_LEAP = register(new SavageLeapAbility(KindredSpirits.id("savage_leap")));

    public static final CompanionAbility CRUSHING_MIGHT = register(new SimpleAbility(KindredSpirits.id("crushing_might"), 40,
            (companion, owner) -> {
                if (owner != null && companion.distanceToSqr(owner) < 144.0) {
                    owner.addEffect(new MobEffectInstance(MobEffects.HASTE, 60, 0, true, false, true));
                }
            }));

    public static final CompanionAbility DRAGON_BREATH = register(new DragonBreathAbility(KindredSpirits.id("dragon_breath")));

    public static final CompanionAbility FORGE_DRAFT = register(new ForgeDraftAbility(KindredSpirits.id("forge_draft")));

    public static final CompanionAbility MIRROR_STRIKE =
            register(new MirrorStrikeAbility(KindredSpirits.id("mirror_strike")));

    private static @Nullable LivingEntity attackableTarget(CompanionEntity companion, double maxRangeSqr) {
        LivingEntity target = companion.getTarget();

        if (target == null || !target.isAlive()
                || companion.distanceToSqr(target) > maxRangeSqr
                || !companion.hasLineOfSight(target)) {
            return null;
        }

        return target;
    }

    private static void breathTrail(ServerLevel level, CompanionEntity companion, LivingEntity target,
                                    ParticleOptions particle, int steps, int count, double spread, double speed) {
        Vec3 from = companion.getEyePosition();
        Vec3 step = target.getBoundingBox().getCenter().subtract(from).scale(1.0 / steps);

        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(step.scale(i));
            level.sendParticles(particle, point.x, point.y, point.z, count, spread, spread, spread, speed);
        }
    }

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

    private record SavageLeapAbility(Identifier id) implements CompanionAbility {
        private static final int COOLDOWN_TICKS = 200;
        private static final double MIN_RANGE_SQR = 9.0;
        private static final double MAX_RANGE_SQR = CompanionEntity.RUN_DISTANCE * CompanionEntity.RUN_DISTANCE;
        private static final double SLAM_RADIUS = 2.5;
        private static final float DAMAGE_MULTIPLIER = 1.5f;
        private static final double KNOCKBACK = 0.5;

        @Override
        public int intervalTicks() {
            return 10;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!companion.isAbilityReady(this.id)) {
                return;
            }

            LivingEntity target = attackableTarget(companion, MAX_RANGE_SQR);

            if (target == null || !companion.onGround()
                    || companion.distanceToSqr(target) < MIN_RANGE_SQR) {
                return;
            }

            companion.setAbilityCooldown(this.id, COOLDOWN_TICKS);
            companion.faceInstantly(target);
            companion.playCompanionAnim(CompanionAnimations.JUMP_ATTACK);
            companion.playSound(companion.species().sounds().specialAttack().get(), 1.0f, 1.0f);

            Vec3 leap = target.position().subtract(companion.position()).normalize();
            companion.setDeltaMovement(new Vec3(leap.x * 0.85, 0.45, leap.z * 0.85));
            companion.hurtMarked = true;

            companion.scheduleLeapImpact(
                    (float) companion.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER,
                    SLAM_RADIUS, KNOCKBACK);
        }
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

            LivingEntity target = attackableTarget(companion, RANGE_SQR);
            if (target == null) {
                return;
            }

            companion.playCompanionAnim(CompanionAnimations.SPECIAL_ATTACK);
            companion.playSound(companion.species().sounds().specialAttack().get(), 0.8f, 1.6f);

            breathTrail(level, companion, target, ParticleTypes.SOUL_FIRE_FLAME, TRAIL_STEPS, 2, 0.06, 0.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(0.5), target.getZ(),
                    10, 0.25, 0.25, 0.25, 0.02);

            target.hurtServer(level, companion.damageSources().indirectMagic(companion, companion),
                    (float) companion.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5f);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, true, true));
        }
    }

    private record DragonBreathAbility(Identifier id) implements CompanionAbility {
        private static final double RANGE_SQR = 100.0;
        private static final int COOLDOWN_TICKS = 60;
        private static final float DAMAGE_MULTIPLIER = 0.75f;
        private static final float FIRE_SECONDS = 4.0f;
        private static final int TRAIL_STEPS = 14;
        private static final float CLOUD_RADIUS = 1.5f;
        private static final int CLOUD_TICKS = 100;
        private static final int CLOUD_WAIT = 5;
        private static final PowerParticleOption BREATH_PARTICLE =
                PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0f);

        @Override
        public int intervalTicks() {
            return 10;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level) || !companion.isAbilityReady(this.id)) {
                return;
            }

            LivingEntity target = attackableTarget(companion, RANGE_SQR);
            if (target == null) {
                return;
            }

            companion.setAbilityCooldown(this.id, COOLDOWN_TICKS);
            companion.playCompanionAnim(CompanionAnimations.SPECIAL_ATTACK);
            companion.playSound(companion.species().sounds().specialAttack().get(), 1.0f, 1.2f);

            breathTrail(level, companion, target, BREATH_PARTICLE, TRAIL_STEPS, 3, 0.08, 0.01);
            level.sendParticles(BREATH_PARTICLE, target.getX(), target.getY(0.5), target.getZ(),
                    8, 0.25, 0.25, 0.25, 0.02);

            target.hurtServer(level, companion.damageSources().indirectMagic(companion, companion),
                    (float) companion.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER);
            target.igniteForSeconds(FIRE_SECONDS);

            spawnBreathCloud(level, companion, target);
        }

        private static void spawnBreathCloud(ServerLevel level, CompanionEntity companion, LivingEntity target) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, target.getX(), target.getY(), target.getZ());

            cloud.setOwner(companion);
            cloud.setCustomParticle(BREATH_PARTICLE);
            cloud.setRadius(CLOUD_RADIUS);
            cloud.setDuration(CLOUD_TICKS);
            cloud.setWaitTime(CLOUD_WAIT);
            cloud.setRadiusPerTick(-CLOUD_RADIUS / CLOUD_TICKS);

            level.addFreshEntity(cloud);
        }
    }

    private record ForgeDraftAbility(Identifier id) implements CompanionAbility {
        private static final int RADIUS = 4;
        private static final int EXTRA_TICKS = 20;

        @Override
        public int intervalTicks() {
            return EXTRA_TICKS;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)) {
                return;
            }

            BlockPos center = companion.blockPosition();

            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS),
                    center.offset(RADIUS, RADIUS, RADIUS))) {
                BlockState state = level.getBlockState(pos);

                if (!(state.getBlock() instanceof AbstractFurnaceBlock)
                        || !state.getOptionalValue(AbstractFurnaceBlock.LIT).orElse(false)
                        || !(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) {
                    continue;
                }

                for (int i = 0; i < EXTRA_TICKS; i++) {
                    AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
                }

                level.sendParticles(ParticleTypes.SMALL_FLAME,
                        pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.15, 0.02, 0.15, 0.0);
            }
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
