package com.breakinblocks.kindredspirits.item;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAggression;
import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.BondRecord;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.breakinblocks.kindredspirits.net.CharmView;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class KindredCharmItem extends Item {
    public KindredCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof CompanionEntity companion) || !companion.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) {
            toggleBond(serverPlayer, companion);
        } else {
            KindredNetworking.sendCharmView(serverPlayer, true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            KindredNetworking.sendCharmView(serverPlayer, true);
        }

        return InteractionResult.SUCCESS;
    }

    private static void toggleBond(ServerPlayer player, CompanionEntity companion) {
        CompanionBond bond = KindredAttachments.bond(player);

        if (bond.isBound()) {
            if (bond.companion().get().equals(companion.getUUID())) {
                release(player, bond, companion);
                KindredNetworking.sendCharmView(player, false);
                return;
            }

            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_already_bonded",
                    bond.snapshot().orElseThrow().displayName()));
            return;
        }

        KindredAttachments.modifyBond(player, current -> snapshot(current, companion).withStored(false));
        KindredAttachments.modify(player, record -> record.withBonded(record.companionsBonded() + 1));
        companion.setBonded(true);

        player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_bound", companion.getDisplayName()));
        KindredNetworking.sendCharmView(player, false);
    }

    public static CharmView viewFor(ServerPlayer player) {
        CompanionBond bond = KindredAttachments.bond(player);
        BondRecord record = KindredAttachments.get(player);

        if (!bond.isBound()) {
            return CharmView.unbound(record);
        }

        return CharmView.of(bond, findCompanion(player.level().getServer(), bond), player.level().getGameTime(), record);
    }

    public static void handleAction(ServerPlayer player, KindredNetworking.CharmActionPayload.Action action,
                                    int value, String text) {
        CompanionBond bond = KindredAttachments.bond(player);
        ServerLevel level = player.level();

        if (!bond.isBound()) {
            if (action != KindredNetworking.CharmActionPayload.Action.REFRESH) {
                player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_empty"));
            }
            KindredNetworking.sendCharmView(player, false);
            return;
        }

        CompanionEntity live = findCompanion(player.level().getServer(), bond);

        switch (action) {
            case SUMMON -> summon(player, level, bond);
            case DISMISS -> dismiss(player, bond, live);
            case RECALL -> recall(player, level, bond, live);
            case RELEASE -> release(player, bond, live);
            case SET_COMMAND -> {
                if (live != null) {
                    live.setCommand(CompanionCommand.byOrdinal(value));
                } else {
                    updateSnapshot(player, snap -> snap.withCommand(CompanionCommand.byOrdinal(value).ordinal()));
                }
            }
            case SET_AGGRESSION -> {
                if (live != null) {
                    live.setAggression(CompanionAggression.byOrdinal(value));
                } else {
                    updateSnapshot(player, snap -> snap.withAggression(CompanionAggression.byOrdinal(value).ordinal()));
                }
            }
            case PRESTIGE -> prestige(player, live);
            case USE_ABILITY -> useAbility(player, bond, live);
            case UNEQUIP -> unequip(player, live);
            case TOGGLE_ABILITY -> toggleAbility(player, live, text);
            case SET_NAME -> rename(player, bond, live, text, value == 1);
            case SET_SKIN -> setSkin(player, bond, live, text, value == 1);
            case REFRESH -> {
            }
        }

        KindredNetworking.sendCharmView(player, false);
    }

    public static boolean useAbility(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        if (missingLive(player, bond, live)) {
            return false;
        }
        if (live.activeAbility().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.no_active_ability",
                    live.getDisplayName()));
            return false;
        }
        if (!live.isAbilityReady(live.activeAbility().get().id())) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.ability_cooldown",
                    live.activeAbility().get().displayName(),
                    Math.ceilDiv(live.abilityCooldownTicks(live.activeAbility().get().id()), 20)));
            return false;
        }
        return live.useActiveAbility(player);
    }

    private static void toggleAbility(ServerPlayer player, @Nullable CompanionEntity live, String text) {
        CompanionAbility ability = CompanionAbilities.get(KindredSpirits.id(text));
        if (ability == null) {
            return;
        }

        if (live != null) {
            live.setAbilityEnabled(ability, live.isAbilityDisabled(ability));
            KindredAttachments.modifyBond(player, current -> snapshot(current, live));
            return;
        }

        updateSnapshot(player, snap -> {
            List<String> names = new ArrayList<>(snap.disabledAbilities());
            if (!names.remove(text)) {
                names.add(text);
            }
            return snap.withDisabledAbilities(names);
        });
    }

    private static void unequip(ServerPlayer player, @Nullable CompanionEntity live) {
        ItemStack taken;
        if (live != null) {
            taken = live.takeEquipment();
            KindredAttachments.modifyBond(player, current -> snapshot(current, live));
        } else {
            taken = KindredAttachments.bond(player).snapshot().orElseThrow().equipment();
            updateSnapshot(player, snap -> snap.withEquipment(ItemStack.EMPTY));
        }

        if (!taken.isEmpty()) {
            player.getInventory().placeItemBackInInventory(taken);
        }
    }

    private static void prestige(ServerPlayer player, @Nullable CompanionEntity live) {
        if (live != null) {
            if (live.prestige()) {
                KindredAttachments.modifyBond(player, current -> snapshot(current, live));
            }
            return;
        }

        CompanionSnapshot current = KindredAttachments.bond(player).snapshot().orElseThrow();
        CompanionSnapshot prestiged = current.prestiged();

        if (prestiged != current) {
            updateSnapshot(player, snap -> prestiged);
            KindredAttachments.modify(player, record -> record.withHighestStars(prestiged.stars()));
            player.sendSystemMessage(Component.translatable("message.kindredspirits.prestige",
                    prestiged.displayName(), prestiged.stars()));
        }
    }

    private static void updateSnapshot(ServerPlayer player, UnaryOperator<CompanionSnapshot> change) {
        KindredAttachments.modifyBond(player, current ->
                current.withSnapshot(current.companion().orElseThrow(),
                        change.apply(current.snapshot().orElseThrow())));
    }

    private static void rename(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live,
                               String text, boolean announce) {
        Optional<String> name = sanitiseName(text);

        if (live != null) {
            live.setCustomName(name.map(Component::literal).orElse(null));
            KindredAttachments.modifyBond(player, current -> snapshot(current, live));
        } else {
            updateSnapshot(player, snap -> snap.withName(name));
        }

        if (!announce) {
            return;
        }

        player.sendSystemMessage(name
                .map(value -> Component.translatable("message.kindredspirits.charm_renamed", value))
                .orElseGet(() -> Component.translatable("message.kindredspirits.charm_name_cleared")));
    }

    private static void setSkin(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live,
                                String text, boolean announce) {
        CompanionSnapshot current = bond.snapshot().orElseThrow();
        boolean wearsSkin = live != null
                ? live.species().usesPlayerSkin()
                : current.resolveSpecies().map(CompanionSpecies::usesPlayerSkin).orElse(false);

        if (!wearsSkin) {
            return;
        }

        if (!KindredConfig.COMMON.allowSkinChoice.get()) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_skin_disabled"));
            return;
        }

        String cleaned = text.trim();
        if (!cleaned.isEmpty() && !StringUtil.isValidPlayerName(cleaned)) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_skin_invalid", cleaned));
            return;
        }

        if (live != null) {
            live.setSkinName(cleaned);
            KindredAttachments.modifyBond(player, bondState -> snapshot(bondState, live));
        } else {
            updateSnapshot(player, snap ->
                    snap.withSkin(cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned)));
        }

        if (!announce) {
            return;
        }

        player.sendSystemMessage(cleaned.isEmpty()
                ? Component.translatable("message.kindredspirits.charm_skin_cleared")
                : Component.translatable("message.kindredspirits.charm_skin_set", cleaned));
    }

    private static Optional<String> sanitiseName(String text) {
        String cleaned = text.replace('§', ' ').trim();

        if (cleaned.length() > KindredNetworking.CharmActionPayload.MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, KindredNetworking.CharmActionPayload.MAX_NAME_LENGTH).trim();
        }

        return cleaned.isEmpty() ? Optional.empty() : Optional.of(cleaned);
    }

    private static void release(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();
        Component name = snapshot.displayName();

        if (live != null) {
            live.setBonded(false);
            live.dropEquipment();
        } else if (!snapshot.equipment().isEmpty()) {
            player.getInventory().placeItemBackInInventory(snapshot.equipment().copy());
        }

        releaseBond(player);
        player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_released", name));
    }

    private static boolean missingLive(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        if (live != null) {
            return false;
        }

        player.sendSystemMessage(Component.translatable(bond.stored()
                ? "message.kindredspirits.charm_already_resting"
                : "message.kindredspirits.charm_unreachable"));
        return true;
    }

    private static void recall(ServerPlayer player, ServerLevel level, CompanionBond bond, @Nullable CompanionEntity live) {
        if (missingLive(player, bond, live)) {
            return;
        }

        live.teleportTo(level, player.getX(), player.getY(), player.getZ(),
                Set.of(), player.getYRot(), 0.0f, false);
        KindredAttachments.modifyBond(player, current -> snapshot(current, live).withStored(false));
        player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_recalled", live.getDisplayName()));
    }

    private static void dismiss(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        if (missingLive(player, bond, live)) {
            return;
        }

        Component name = live.getDisplayName();
        KindredAttachments.modifyBond(player, current -> snapshot(current, live).withStored(true));
        live.discard();
        player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_dismissed", name));
    }

    private static void summon(ServerPlayer player, ServerLevel level, CompanionBond bond) {
        if (!bond.stored()) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_unreachable"));
            return;
        }

        long remaining = bond.reviveReadyAt() - level.getGameTime();
        if (remaining > 0) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_recovering",
                    Math.max(1, remaining / 20)));
            return;
        }

        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();
        Optional<CompanionSpecies> species = snapshot.resolveSpecies();

        if (species.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_missing"));
            return;
        }

        CompanionEntity companion = KindredEntities.type(species.get()).create(level, EntitySpawnReason.COMMAND);
        if (companion == null) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_missing"));
            return;
        }

        UUID bound = bond.companion().orElseThrow();
        if (findEntity(level.getServer(), bound) == null) {
            companion.setUUID(bound);
        }

        companion.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        companion.tame(player);
        snapshot.name().ifPresent(name -> companion.setCustomName(Component.literal(name)));
        companion.setSkinName(snapshot.skin().orElse(""));

        boolean reviving = bond.reviveReadyAt() > 0;
        companion.restoreProgress(snapshot, reviving ? penalisedExperience(snapshot) : snapshot.experience());
        companion.setCommand(snapshot.commandValue());
        companion.setAggression(snapshot.aggressionValue());
        companion.setBonded(true);

        if (!level.tryAddFreshEntityWithPassengers(companion)) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_missing"));
            return;
        }

        KindredAttachments.modifyBond(player, current ->
                snapshot(current, companion).withStored(false).withReviveReadyAt(0L));

        player.sendSystemMessage(Component.translatable(
                reviving ? "message.kindredspirits.charm_revived" : "message.kindredspirits.charm_summoned",
                companion.getDisplayName()));
    }

    private static int penalisedExperience(CompanionSnapshot snapshot) {
        double kept = 1.0 - KindredConfig.COMMON.reviveExperiencePenalty.get();
        return Math.max(0, (int) Math.floor(snapshot.experience() * kept));
    }

    public static void onCompanionDied(CompanionEntity companion) {
        if (!(companion.getOwner() instanceof ServerPlayer owner)) {
            return;
        }

        CompanionBond bond = KindredAttachments.bond(owner);
        if (!bond.isBound() || !bond.companion().get().equals(companion.getUUID())) {
            return;
        }

        int cooldownTicks = CompanionBondMath.reviveCooldownTicks(companion.getBondLevel());
        long readyAt = companion.level().getGameTime() + cooldownTicks;

        KindredAttachments.modifyBond(owner, current ->
                snapshot(current, companion).withStored(true).withReviveReadyAt(readyAt));

        owner.sendSystemMessage(Component.translatable("message.kindredspirits.charm_companion_lost",
                companion.getDisplayName(), Math.max(1, cooldownTicks / 20)));
    }

    public static CompanionBond snapshot(CompanionBond bond, CompanionEntity companion) {
        return bond.withSnapshot(companion.getUUID(), CompanionSnapshot.of(companion))
                .withLocation(companion.level().dimension().identifier(), companion.blockPosition());
    }

    private static @Nullable CompanionEntity findCompanion(MinecraftServer server, CompanionBond bond) {
        UUID bound = bond.companion().orElse(null);
        if (bound == null) {
            return null;
        }

        if (findEntity(server, bound) instanceof CompanionEntity loaded && loaded.isAlive()) {
            return loaded;
        }

        ServerLevel level = lastKnownLevel(server, bond);
        BlockPos pos = bond.lastPos().orElse(null);

        if (level == null || pos == null) {
            return null;
        }

        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        return level.getEntity(bound) instanceof CompanionEntity fetched && fetched.isAlive() ? fetched : null;
    }

    private static @Nullable ServerLevel lastKnownLevel(MinecraftServer server, CompanionBond bond) {
        Identifier dimension = bond.lastDimension().orElse(null);
        return dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
    }

    private static @Nullable Entity findEntity(MinecraftServer server, UUID id) {
        for (ServerLevel candidate : server.getAllLevels()) {
            Entity entity = candidate.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    public static boolean releaseFully(ServerPlayer player) {
        CompanionBond bond = KindredAttachments.bond(player);

        if (!bond.isBound()) {
            return false;
        }

        release(player, bond, findCompanion(player.level().getServer(), bond));
        return true;
    }

    public static void releaseBond(Player player) {
        KindredAttachments.modifyBond(player, current -> CompanionBond.NONE);

        ItemStack charm = new ItemStack(KindredItems.KINDRED_CHARM.get());
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(charm));
    }
}
