package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionProgressEvents {
    private static final int GUARD_INTERVAL = 20;
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
        }
    }

    private static void updateGuard(ServerPlayer player) {
        AttributeInstance armour = player.getAttribute(Attributes.ARMOR);
        if (armour == null) {
            return;
        }

        boolean guarded = bondedCompanionNear(player, CompanionBondMath.GUARD_RANGE)
                .map(companion -> CompanionBondMath.grantsGuard(companion.getBondLevel()))
                .orElse(false);

        if (guarded && !armour.hasModifier(BOND_GUARD.id())) {
            armour.addTransientModifier(BOND_GUARD);
        } else if (!guarded && armour.hasModifier(BOND_GUARD.id())) {
            armour.removeModifier(BOND_GUARD.id());
        }
    }

    @SubscribeEvent
    public static void onExperienceChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) {
            return;
        }

        int share = (int) Math.floor(event.getAmount() * KindredConfig.COMMON.xpShare.get());
        if (share <= 0) {
            return;
        }

        bondedCompanionNear(player, CompanionBondMath.PASSIVE_RANGE)
                .ifPresent(companion -> companion.gainExperience(share));
    }

    public static Optional<CompanionEntity> bondedCompanionNear(ServerPlayer player, double range) {
        return CompanionEntity.nearestOwned(player, range).filter(CompanionEntity::isBonded);
    }

    public static boolean isAfk(ServerPlayer player) {
        return KindredAttachments.activity(player).isAfk(player.level().getGameTime(), CompanionBondMath.afkTicks());
    }

    private CompanionProgressEvents() {
    }
}
