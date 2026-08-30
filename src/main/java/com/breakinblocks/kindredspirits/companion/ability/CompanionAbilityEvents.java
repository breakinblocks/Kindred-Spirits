package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionAbilityEvents {
    private static final double ABILITY_RANGE = 16.0;
    private static final double ALPHA_BASE_RADIUS = 4.0;
    private static final int ALPHA_LEVELS_PER_BLOCK = 3;
    private static final double ALPHA_SEARCH = 32.0;
    private static final double DRAGONFIRE_CHANCE = 0.25;
    private static final float DRAGONFIRE_SECONDS = 4.0f;
    private static final double HELPING_HAND_CHANCE = 0.05;
    private static final double EAT_THAT_BASE_SHARE = 0.10;

    public static double alphaRadius(int level) {
        return ALPHA_BASE_RADIUS + level / ALPHA_LEVELS_PER_BLOCK;
    }

    private static Optional<CompanionEntity> companionWith(ServerPlayer owner, CompanionAbility ability) {
        return CompanionEntity.bondedWithAbility(owner, ABILITY_RANGE, ability);
    }

    @SubscribeEvent
    public static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL || !(event.getEntity() instanceof Enemy)
                || !CompanionAbilities.enabled()) {
            return;
        }

        ServerLevel level = event.getLevel().getLevel();
        AABB search = AABB.ofSize(new Vec3(event.getX(), event.getY(), event.getZ()),
                ALPHA_SEARCH * 2, ALPHA_SEARCH * 2, ALPHA_SEARCH * 2);

        boolean denied = !level.getEntitiesOfClass(CompanionEntity.class, search,
                companion -> companion.isAlive() && companion.isBonded()
                        && companion.hasAbility(CompanionAbilities.ALPHA)
                        && companion.distanceToSqr(event.getX(), event.getY(), event.getZ())
                        <= square(alphaRadius(companion.getLevel()))).isEmpty();

        if (denied) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onCompanionDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof CompanionEntity companion)
                || !companion.hasAbility(CompanionAbilities.DRAGONFIRE)
                || !CompanionAbilities.enabled()
                || event.getHealthDamage() <= 0.0f) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target.isAlive() && companion.getRandom().nextDouble() < DRAGONFIRE_CHANCE) {
            target.igniteForSeconds(target.getRemainingFireTicks() / 20.0f + DRAGONFIRE_SECONDS);
            if (target.level() instanceof ServerLevel level) {
                CompanionAbilities.hitParticles(level, target, ParticleTypes.FLAME, 8, 0.5, 0.3, 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || !crafted.isStackable()) {
            return;
        }

        companionWith(owner, CompanionAbilities.HELPING_HAND).ifPresent(companion -> {
            if (owner.getRandom().nextDouble() < companion.scaledChance(HELPING_HAND_CHANCE)) {
                owner.getInventory().placeItemBackInInventory(crafted.copy());
                companion.burst(ParticleTypes.HAPPY_VILLAGER, 6);
                companion.playSound(companion.species().sounds().interact().get(), 0.8f, 1.2f);
            }
        });
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }

        FoodProperties food = event.getItem().get(DataComponents.FOOD);
        if (food == null) {
            return;
        }

        companionWith(owner, CompanionAbilities.EAT_THAT).ifPresent(companion -> {
            float share = (float) (EAT_THAT_BASE_SHARE + companion.getLevel() / 100.0);
            companion.heal(food.nutrition() * share);
            float saturation = owner.getFoodData().getSaturationLevel() + food.saturation() * companion.getLevel() / 100.0f;
            owner.getFoodData().setSaturation(Math.min(saturation, owner.getFoodData().getFoodLevel()));
            companion.burst(ParticleTypes.HEART, 2);
        });
    }

    private static double square(double value) {
        return value * value;
    }

    private CompanionAbilityEvents() {
    }
}
