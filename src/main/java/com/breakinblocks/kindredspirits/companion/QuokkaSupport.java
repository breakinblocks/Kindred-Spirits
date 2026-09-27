package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.registry.KindredParticles;
import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Stateless support mechanics; timers belong to companions or persisted mob attachments. */
@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class QuokkaSupport {
    private static final Identifier SNACK = KindredSpirits.id("quokka_snack");
    private static final List<Holder<MobEffect>> SMILE_EFFECTS =
            List.of(MobEffects.REGENERATION, MobEffects.HASTE, MobEffects.SPEED, MobEffects.RESISTANCE, MobEffects.ABSORPTION);

    private QuokkaSupport() {}

    public static void smile(CompanionEntity companion, @Nullable ServerPlayer owner) {
        Identifier id = CompanionAbilities.SMILE.id();
        if (!companion.ownerWithin(owner, 256) || !companion.isAbilityReady(id)) return;
        owner.addEffect(new MobEffectInstance(SMILE_EFFECTS.get(companion.getRandom().nextInt(SMILE_EFFECTS.size())), 600));
        companion.setAbilityCooldown(id, (int) Math.round(Math.max(30, 120 - 3 * companion.getLevel()) * 20
                * companion.bondRateMultiplier()));
        companion.playSpecialAttack(0.7f, 1.2f);
        companion.burst(KindredParticles.HAPPY_BLOOM.get(), 5);
    }

    public static void breed(CompanionEntity companion, @Nullable ServerPlayer owner) {
        Identifier id = CompanionAbilities.ALWAYS_HAPPY.id();
        if (!companion.isAbilityReady(id)) return;
        companion.setAbilityCooldown(id, 2400);
        Map<EntityType<?>, List<Animal>> groups = nearbyAnimals(companion).stream()
                .collect(Collectors.groupingBy(Animal::getType));
        boolean bred = false;
        for (List<Animal> group : groups.values()) {
            if (group.size() < 2 || group.size() >= 16) continue;
            List<Animal> adults = group.stream().filter(a -> a.getAge() == 0 && a.canFallInLove()).toList();
            if (adults.size() < 2) continue;
            // Use vanilla compatibility checks, including species-specific restrictions (e.g. mules).
            for (Animal animal : adults) animal.setInLove(owner);
            for (Animal animal : adults) {
                if (adults.stream().noneMatch(other -> other != animal && animal.canMate(other))) animal.resetLove();
                else bred = true;
            }
        }
        if (bred) { companion.playSpecialAttack(0.7f, 1.2f); companion.burst(KindredParticles.HAPPY_BLOOM.get(), 6); }
    }

    private static List<Animal> nearbyAnimals(CompanionEntity companion) {
        return companion.level().getEntitiesOfClass(Animal.class, companion.getBoundingBox().inflate(8),
                a -> a.isAlive() && !(a instanceof CompanionEntity) && a.distanceToSqr(companion) <= 64);
    }

    public static void tick(CompanionEntity companion) {
        if (companion.species() != CompanionSpecies.QUOKKA || !companion.isBonded() || !CompanionAbilities.enabled()) return;
        if (companion.hasAbility(CompanionAbilities.ALWAYS_HAPPY)) {
            // Exactly two extra age ticks; a per-animal stamp prevents overlapping companions stacking.
            long now = companion.level().getGameTime();
            for (Animal animal : nearbyAnimals(companion)) {
                if (animal.isBaby() && !animal.isAgeLocked() && animal.getData(KindredAttachments.QUOKKA_AGED_AT) != now) {
                    animal.setData(KindredAttachments.QUOKKA_AGED_AT, now);
                    animal.setAge(Math.min(0, animal.getAge() + 2));
                }
            }
        }
        if (!companion.hasEquipment(KindredItems.QUOKKA_SNACK.get()) || !companion.isAbilityReady(SNACK)) return;
        companion.setAbilityCooldown(SNACK, 600);
        if (companion.getRandom().nextFloat() >= 0.2f) return;
        companion.level().getEntitiesOfClass(Mob.class, companion.getBoundingBox().inflate(8),
                        mob -> mob.isAlive() && mob instanceof Enemy && mob.distanceToSqr(companion) <= 64 && !isCharmed(mob))
                .stream().min(Comparator.comparingDouble(companion::distanceToSqr)).ifPresent(mob -> {
                    charm(mob, 400);
                    companion.playSpecialAttack(0.7f, 1.2f);
                    companion.burst(KindredParticles.BOND_HEART.get(), 4);
                });
    }

    public static boolean isCharmed(Mob mob) {
        return CompanionAbilities.enabled() && mob.hasData(KindredAttachments.CHARMED)
                && mob.getData(KindredAttachments.CHARMED) > mob.level().getGameTime();
    }

    public static void charm(Mob mob, int ticks) {
        mob.setData(KindredAttachments.CHARMED, mob.level().getGameTime() + ticks);
        redirect(mob);
    }

    private static void redirect(Mob mob) {
        Mob target = nearestHostile(mob);
        mob.setTarget(target);
        if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }

    private static @Nullable Mob nearestHostile(Mob mob) {
        return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16),
                        other -> other != mob && other.isAlive() && other instanceof Enemy && mob.distanceToSqr(other) <= 256)
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }

    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && isCharmed(mob)
                && !(event.getNewAboutToBeSetTarget() instanceof Enemy)) {
            event.setNewAboutToBeSetTarget(nearestHostile(mob));
        }
    }

    @SubscribeEvent
    public static void protectNonHostiles(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Mob mob && isCharmed(mob)
                && !(event.getEntity() instanceof Enemy)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tickCharmed(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !mob.hasData(KindredAttachments.CHARMED)) return;
        if (!isCharmed(mob)) {
            mob.removeData(KindredAttachments.CHARMED);
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return;
        }
        // Brain-based mobs may bypass Mob#setTarget when choosing a retaliation target.
        LivingEntity target = mob.getTarget();
        if (mob.tickCount % 10 == 0 || target == null || !target.isAlive() || !(target instanceof Enemy)
                || target.distanceToSqr(mob) > 256) redirect(mob);
        if (mob.tickCount % 10 == 0) {
            level.sendParticles(KindredParticles.BOND_HEART.get(), mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    1, 0.2, 0.1, 0.2, 0);
        }
    }

    public static void throwBaby(CompanionEntity parent, LivingEntity attacker) {
        ServerLevel level = (ServerLevel) parent.level();
        CompanionEntity baby = KindredEntities.type(CompanionSpecies.QUOKKA).create(level, EntitySpawnReason.EVENT);
        if (baby == null) return;
        baby.makeQuokkaDecoy(1200);
        baby.snapTo(parent.getX(), parent.getY() + 0.3, parent.getZ(), parent.getYRot(), 0);
        Vec3 direction = attacker.position().subtract(parent.position()).normalize();
        baby.setDeltaMovement(direction.x * 0.7, 0.4, direction.z * 0.7);
        if (level.addFreshEntity(baby)) parent.playSpecialAttack(0.7f, 1.2f);
    }
}
