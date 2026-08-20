package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAnimations;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class MirrorStrikeAbility implements CompanionAbility {
    private static final float SHARE = 0.25f;
    private static final int COOLDOWN_TICKS = 20;
    private static final double OWNER_RANGE = 16.0;
    private static final double TARGET_RANGE_SQR = 144.0;

    private final Identifier id;

    public MirrorStrikeAbility(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier id() {
        return this.id;
    }

    @Override
    public int intervalTicks() {
        return 200;
    }

    @Override
    public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
    }

    @SubscribeEvent
    public static void onLivingDamaged(LivingDamageEvent.Post event) {
        if (!KindredConfig.COMMON.abilitiesEnabled.get()) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer owner)) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target == owner || target instanceof CompanionEntity
                || !target.isAlive() || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        float damage = event.getHealthDamage();
        if (damage <= 0.0f) {
            return;
        }

        List<CompanionEntity> nearby = level.getEntitiesOfClass(CompanionEntity.class,
                owner.getBoundingBox().inflate(OWNER_RANGE),
                companion -> companion.isAlive() && companion.isBonded() && companion.isOwnedBy(owner));

        for (CompanionEntity companion : nearby) {
            if (!companion.hasAbility(CompanionAbilities.MIRROR_STRIKE)
                    || !companion.isAbilityReady(CompanionAbilities.MIRROR_STRIKE.id())
                    || companion.distanceToSqr(target) > TARGET_RANGE_SQR
                    || !companion.hasLineOfSight(target)) {
                continue;
            }

            echo(companion, level, target, damage);
            return;
        }
    }

    private static void echo(CompanionEntity companion, ServerLevel level, LivingEntity target, float damage) {
        companion.setAbilityCooldown(CompanionAbilities.MIRROR_STRIKE.id(), COOLDOWN_TICKS);
        companion.playCompanionAnim(CompanionAnimations.SPECIAL_ATTACK);
        companion.playSound(companion.species().sounds().specialAttack().get(), 0.7f, 1.4f);

        level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(),
                8, 0.25, 0.25, 0.25, 0.05);

        target.hurtServer(level, companion.damageSources().indirectMagic(companion, companion), damage * SHARE);
    }
}
