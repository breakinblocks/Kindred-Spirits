package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionProgressEvents {
    private static final int GUARD_INTERVAL = 20;
    private static final double BOTTLE_REACH = 2.0;
    private static final AttributeModifier BOND_GUARD = new AttributeModifier(
            KindredSpirits.id("bond_guard"), CompanionBondMath.GUARD_ARMOUR, AttributeModifier.Operation.ADD_VALUE);

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        KindredAttachments.activity(player).update(player, gameTime);

        if (player.tickCount % GUARD_INTERVAL == 0) {
            updateGuard(player);
            KindredAttachments.syncTooltip(player);
            CompanionAbilities.expireSnackScale(player);
        }
    }

    private static void updateGuard(ServerPlayer player) {
        AttributeInstance armour = player.getAttribute(Attributes.ARMOR);
        if (armour == null) {
            return;
        }

        boolean guarded = KindredConfig.COMMON.abilitiesEnabled.get()
                && bondedCompanionNear(player, CompanionBondMath.GUARD_RANGE)
                        .map(companion -> CompanionBondMath.grantsGuard(companion.getBondLevel()))
                        .orElse(false);

        if (guarded && !armour.hasModifier(BOND_GUARD.id())) {
            armour.addTransientModifier(BOND_GUARD);
        } else if (!guarded && armour.hasModifier(BOND_GUARD.id())) {
            armour.removeModifier(BOND_GUARD.id());
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ExperienceOrb orb)) {
            return;
        }

        if (!event.getLevel()
                .getEntitiesOfClass(
                        ThrownExperienceBottle.class,
                        orb.getBoundingBox().inflate(BOTTLE_REACH),
                        bottle -> !bottle.isRemoved())
                .isEmpty()) {
            orb.setData(KindredAttachments.BOTTLE_XP, true);
        }
    }

    @SubscribeEvent
    public static void onPickupExperience(PlayerXpEvent.PickupXp event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getOrb().getData(KindredAttachments.BOTTLE_XP)) {
            player.setData(KindredAttachments.BOTTLE_XP_PICKUP, player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onExperienceChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }

        if (player.getData(KindredAttachments.BOTTLE_XP_PICKUP)
                == player.level().getGameTime()) {
            player.setData(KindredAttachments.BOTTLE_XP_PICKUP, -1L);
            return;
        }

        bondedCompanionNear(player, CompanionBondMath.PASSIVE_RANGE).ifPresent(companion -> {
            int share = companion.progress().shareExperience(event.getAmount(), KindredConfig.COMMON.xpShare.get());
            if (share > 0) companion.gainExperience(share);
        });
    }

    public static Optional<CompanionEntity> bondedCompanionNear(ServerPlayer player, double range) {
        return CompanionEntity.bondedNear(player, range);
    }

    public static boolean isAfk(ServerPlayer player) {
        return KindredAttachments.activity(player).isAfk(player.level().getGameTime(), CompanionBondMath.afkTicks());
    }

    private CompanionProgressEvents() {}
}
