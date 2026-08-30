package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jspecify.annotations.Nullable;

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

        CompanionEntity.bondedWithAbility(owner, OWNER_RANGE, CompanionAbilities.MIRROR_STRIKE)
                .filter(companion -> companion.isAbilityReady(CompanionAbilities.MIRROR_STRIKE.id())
                        && companion.distanceToSqr(target) <= TARGET_RANGE_SQR
                        && companion.hasLineOfSight(target))
                .ifPresent(companion -> echo(companion, level, target, damage));
    }

    private static void echo(CompanionEntity companion, ServerLevel level, LivingEntity target, float damage) {
        companion.setAbilityCooldown(CompanionAbilities.MIRROR_STRIKE.id(), COOLDOWN_TICKS);
        companion.playSpecialAttack(0.7f, 1.4f);
        CompanionAbilities.hitParticles(level, target, ParticleTypes.ENCHANTED_HIT, 8, 0.6, 0.25, 0.05);
        companion.magicHurt(level, target, damage * SHARE);
    }
}
