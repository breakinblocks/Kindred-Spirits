package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MeteorEntity extends AbstractHurtingProjectile implements GeoEntity {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
    private static final float DIRECT_DAMAGE = 6.0f;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
    }

    public MeteorEntity(Level level, LivingEntity owner, Vec3 position) {
        super(KindredEntities.METEOR.get(), position.x, position.y, position.z, new Vec3(0.0, -1.0, 0.0), level);
        this.setOwner(owner);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            for (int i = 0; i < 2; i++) {
                this.level()
                        .addParticle(
                                KindredParticles.FORGE_EMBER.get(),
                                this.getRandomX(0.8),
                                this.getY() + 0.5 + this.random.nextDouble(),
                                this.getRandomZ(0.8),
                                0.0,
                                0.04,
                                0.0);
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel level) {
            level.explode(this, this.getX(), this.getY(), this.getZ(), 0.0f, false, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (this.level() instanceof ServerLevel level) {
            Entity victim = hitResult.getEntity();
            DamageSource source = this.damageSources().source(DamageTypes.FIREBALL, this, this.getOwner());
            victim.hurt(source, DIRECT_DAMAGE);
            EnchantmentHelper.doPostAttackEffects(level, victim, source);
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.LARGE_SMOKE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MeteorEntity>(this, "main", 0, test -> test.setAndContinue(SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
