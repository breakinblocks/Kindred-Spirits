package com.breakinblocks.kindredspirits.item;

import com.breakinblocks.kindredspirits.KindredMessages;
import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.advancement.CompanionTrigger;
import com.breakinblocks.kindredspirits.companion.CompanionAggression;
import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionSkin;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.companion.CompanionWorldData;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.integration.curios.KindredCurios;
import com.breakinblocks.kindredspirits.net.CharmView;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.BondRecord;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.breakinblocks.kindredspirits.registry.KindredParticles;
import com.breakinblocks.kindredspirits.registry.KindredSounds;
import com.breakinblocks.kindredspirits.registry.KindredTriggers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class KindredCharmItem extends Item {
    public KindredCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            KindredNetworking.sendCharmView(serverPlayer, true);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    public static boolean isCarried(Player player) {
        return player.getInventory().contains(stack -> stack.getItem() instanceof KindredCharmItem)
                || KindredCurios.wearsCharm(player);
    }

    public static void openFromKey(ServerPlayer player) {
        if (isCarried(player)) {
            KindredNetworking.sendCharmView(player, true);
        } else {
            player.displayClientMessage(Component.translatable("message.kindredspirits.charm_not_carried"), true);
        }
    }

    private static void toggleBond(ServerPlayer player, CompanionEntity companion) {
        CompanionWorldData.reconcile(player);
        CompanionBond bond = KindredAttachments.bond(player);

        if (bond.isBound()) {
            if (bond.isBoundTo(companion.getUUID())) {
                release(player, bond, companion);
                KindredNetworking.sendCharmView(player, false);
                return;
            }

            KindredMessages.send(
                    player,
                    "charm_already_bonded",
                    bond.snapshot().orElseThrow().displayName());
            return;
        }

        KindredAttachments.modifyBond(
                player, current -> snapshot(current, companion).withStored(false));
        KindredAttachments.modify(player, record -> record.withBonded(record.companionsBonded() + 1));
        companion.setBonded(true);
        companion.reportProgress(player);

        KindredMessages.send(player, "charm_bound", companion.getDisplayName());
        KindredNetworking.sendCharmView(player, false);
    }

    public static CharmView viewFor(ServerPlayer player) {
        CompanionWorldData.reconcile(player);
        CompanionBond bond = KindredAttachments.bond(player);
        BondRecord record = KindredAttachments.get(player);

        if (!bond.isBound()) {
            return CharmView.unbound(record);
        }

        return CharmView.of(
                bond,
                findCompanion(player.level().getServer(), bond),
                player.level().getGameTime(),
                record);
    }

    public static void handleAction(
            ServerPlayer player, KindredNetworking.CharmActionPayload.Action action, int value, String text) {
        CompanionWorldData.reconcile(player);
        CompanionBond bond = KindredAttachments.bond(player);
        ServerLevel level = player.serverLevel();

        if (!bond.isBound()) {
            if (action != KindredNetworking.CharmActionPayload.Action.REFRESH) {
                KindredMessages.send(player, "charm_empty");
            }
            KindredNetworking.sendCharmView(player, false);
            return;
        }

        CompanionEntity live = findCompanion(player.level().getServer(), bond);

        // An unavailable deployed entity is not a stored snapshot. Never edit or
        // return its equipment until the authoritative entity has been loaded.
        if (!bond.stored()
                && live == null
                && action != KindredNetworking.CharmActionPayload.Action.REFRESH
                && action != KindredNetworking.CharmActionPayload.Action.RECALL
                && action != KindredNetworking.CharmActionPayload.Action.RELEASE) {
            KindredMessages.send(player, "charm_unreachable");
            KindredNetworking.sendCharmView(player, false);
            return;
        }

        switch (action) {
            case SUMMON -> summon(player, level, bond);
            case DISMISS -> dismiss(player, bond, live);
            case RECALL -> recall(player, level, bond, live);
            case RELEASE -> release(player, bond, live);
            case SET_COMMAND ->
                apply(
                        player,
                        live,
                        entity -> entity.setCommand(CompanionCommand.byOrdinal(value)),
                        snap -> snap.withCommand(
                                CompanionCommand.byOrdinal(value).ordinal()));
            case SET_AGGRESSION ->
                apply(
                        player,
                        live,
                        entity -> entity.setAggression(CompanionAggression.byOrdinal(value)),
                        snap -> snap.withAggression(
                                CompanionAggression.byOrdinal(value).ordinal()));
            case PRESTIGE -> prestige(player, live);
            case USE_ABILITY -> useAbility(player, bond, live);
            case UNEQUIP -> unequip(player, live);
            case TOGGLE_ABILITY -> toggleAbility(player, live, text);
            case SET_NAME -> rename(player, bond, live, text, value == 1);
            case SET_SKIN -> setSkin(player, bond, live, text, value == 1);
            case REFRESH -> {}
        }

        KindredNetworking.sendCharmView(player, false);
    }

    public static boolean useAbility(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        if (missingLive(player, bond, live)) {
            return false;
        }
        Optional<CompanionAbility> active = live.activeAbility();
        if (active.isEmpty()) {
            KindredMessages.send(player, "no_active_ability", live.getDisplayName());
            return false;
        }
        CompanionAbility ability = active.get();
        if (!live.isAbilityReady(ability.id())) {
            KindredMessages.send(
                    player,
                    "ability_cooldown",
                    ability.displayName(),
                    CharmView.cooldownSeconds(live.abilityCooldownTicks(ability.id())));
            return false;
        }
        return live.useActiveAbility(player);
    }

    private static void toggleAbility(ServerPlayer player, @Nullable CompanionEntity live, String text) {
        ResourceLocation id = ResourceLocation.tryParse(KindredSpirits.MOD_ID + ":" + text);
        CompanionAbility ability = id == null ? null : CompanionAbilities.get(id);
        if (ability == null) {
            return;
        }

        apply(player, live, entity -> entity.setAbilityEnabled(ability, entity.isAbilityDisabled(ability)), snap -> {
            List<String> names = new ArrayList<>(snap.disabledAbilities());
            if (!names.remove(text)) {
                names.add(text);
            }
            return snap.withDisabledAbilities(names);
        });
    }

    private static void unequip(ServerPlayer player, @Nullable CompanionEntity live) {
        ItemStack taken = live != null
                ? live.takeEquipment()
                : KindredAttachments.bond(player).snapshot().orElseThrow().equipment();
        apply(player, live, entity -> {}, snap -> snap.withEquipment(ItemStack.EMPTY));

        if (!taken.isEmpty()) {
            player.getInventory().placeItemBackInInventory(taken);
        }
    }

    private static void prestige(ServerPlayer player, @Nullable CompanionEntity live) {
        if (live != null) {
            if (live.prestige()) {
                sync(player, live);
            }
            return;
        }

        CompanionSnapshot current = KindredAttachments.bond(player).snapshot().orElseThrow();
        CompanionSnapshot prestiged = current.prestiged();

        if (prestiged != current) {
            updateSnapshot(player, snap -> prestiged);
            KindredAttachments.modify(player, record -> record.withHighestStars(prestiged.stars()));
            KindredMessages.send(player, "prestige", prestiged.displayName(), prestiged.stars());
            prestiged
                    .resolveSpecies()
                    .ifPresent(species -> KindredTriggers.COMPANION
                            .get()
                            .trigger(player, species, CompanionTrigger.Event.STARS, prestiged.stars()));
        }
    }

    private static void updateSnapshot(ServerPlayer player, UnaryOperator<CompanionSnapshot> change) {
        KindredAttachments.modifyBond(
                player,
                current -> current.withSnapshot(
                        current.companion().orElseThrow(),
                        change.apply(current.snapshot().orElseThrow())));
    }

    private static void sync(ServerPlayer player, CompanionEntity live) {
        KindredAttachments.modifyBond(player, current -> snapshot(current, live));
    }

    private static void apply(
            ServerPlayer player,
            @Nullable CompanionEntity live,
            Consumer<CompanionEntity> onLive,
            UnaryOperator<CompanionSnapshot> onResting) {
        if (live != null) {
            onLive.accept(live);
            sync(player, live);
        } else {
            updateSnapshot(player, onResting);
        }
    }

    private static void rename(
            ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live, String text, boolean announce) {
        Optional<String> name = sanitiseName(text);

        apply(
                player,
                live,
                entity -> entity.setCustomName(name.map(Component::literal).orElse(null)),
                snap -> snap.withName(name));

        if (!announce) {
            return;
        }

        name.ifPresentOrElse(
                value -> KindredMessages.send(player, "charm_renamed", value),
                () -> KindredMessages.send(player, "charm_name_cleared"));
    }

    private static void setSkin(
            ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live, String text, boolean announce) {
        boolean wearsSkin = live != null
                ? live.species().usesPlayerSkin()
                : bond.snapshot().orElseThrow().usesPlayerSkin();

        if (!wearsSkin) {
            String skin = text.trim();
            if (skin.isEmpty() || CompanionSkin.isValid(skin)) {
                apply(
                        player,
                        live,
                        entity -> entity.setSkinName(skin),
                        snap -> snap.withSkin(CompanionSnapshot.nonEmpty(skin)));
            }
            return;
        }

        if (!KindredConfig.COMMON.allowSkinChoice.get()) {
            KindredMessages.send(player, "charm_skin_disabled");
            return;
        }

        String cleaned = text.trim();
        if (!cleaned.isEmpty() && !StringUtil.isValidPlayerName(cleaned)) {
            KindredMessages.send(player, "charm_skin_invalid", cleaned);
            return;
        }

        apply(
                player,
                live,
                entity -> entity.setSkinName(cleaned),
                snap -> snap.withSkin(CompanionSnapshot.nonEmpty(cleaned)));

        if (!announce) {
            return;
        }

        if (cleaned.isEmpty()) {
            KindredMessages.send(player, "charm_skin_cleared");
        } else {
            KindredMessages.send(player, "charm_skin_set", cleaned);
        }
    }

    private static Optional<String> sanitiseName(String text) {
        String cleaned = text.replace('§', ' ').trim();

        if (cleaned.length() > KindredNetworking.CharmActionPayload.MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, KindredNetworking.CharmActionPayload.MAX_NAME_LENGTH)
                    .trim();
        }

        return CompanionSnapshot.nonEmpty(cleaned);
    }

    private static void release(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();
        Component name = snapshot.displayName();

        if (live != null) {
            live.setBonded(false);
            live.dropEquipment();
        } else {
            if (!bond.stored()) {
                CompanionWorldData.abandon(
                        player.level().getServer(), bond.companion().orElseThrow());
            }
            if (!snapshot.equipment().isEmpty()) {
                player.getInventory()
                        .placeItemBackInInventory(snapshot.equipment().copy());
            }
        }

        releaseBond(player);
        KindredMessages.send(player, "charm_released", name);
    }

    private static boolean missingLive(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        if (live != null) {
            return false;
        }

        KindredMessages.send(player, bond.stored() ? "charm_already_resting" : "charm_unreachable");
        return true;
    }

    private static void recall(
            ServerPlayer player, ServerLevel level, CompanionBond bond, @Nullable CompanionEntity live) {
        if (bond.stored()) {
            KindredMessages.send(player, "charm_already_resting");
            return;
        }

        if (live == null) {
            reform(player, level, bond);
            return;
        }

        Entity arrived = live.changeDimension(new DimensionTransition(
                level, player.position(), Vec3.ZERO, player.getYRot(), 0.0f, DimensionTransition.DO_NOTHING));
        if (!(arrived instanceof CompanionEntity recalled)) {
            KindredMessages.send(player, "charm_unreachable");
            return;
        }
        KindredAttachments.modifyBond(
                player, current -> snapshot(current, recalled).withStored(false));
        KindredMessages.send(player, "charm_recalled", recalled.getDisplayName());
    }

    private static void reform(ServerPlayer player, ServerLevel level, CompanionBond bond) {
        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();

        if (stillSearching(level.getServer(), bond)) {
            KindredMessages.send(player, "charm_searching", snapshot.displayName());
            return;
        }

        CompanionEntity companion = create(player, level, snapshot, snapshot.experience());
        if (companion == null) {
            return;
        }

        if (!level.tryAddFreshEntityWithPassengers(companion)) {
            KindredMessages.send(player, "charm_missing");
            return;
        }

        CompanionWorldData.abandon(level.getServer(), bond.companion().orElseThrow());
        KindredAttachments.modifyBond(
                player, current -> snapshot(current, companion).withStored(false));
        companion.reportProgress(player);
        companion.burst(KindredParticles.SUMMON_RUNE.get(), 16);
        companion.playSound(KindredSounds.CHARM_SUMMON.get(), 0.6f, 1.0f);
        KindredMessages.send(player, "charm_recalled", companion.getDisplayName());
    }

    private static boolean stillSearching(MinecraftServer server, CompanionBond bond) {
        ServerLevel level = lastKnownLevel(server, bond);
        BlockPos pos = bond.lastPos().orElse(null);
        return level != null && pos != null && !level.areEntitiesLoaded(ChunkPos.asLong(pos));
    }

    private static void dismiss(ServerPlayer player, CompanionBond bond, @Nullable CompanionEntity live) {
        if (missingLive(player, bond, live)) {
            return;
        }

        Component name = live.getDisplayName();
        KindredAttachments.modifyBond(player, current -> snapshot(current, live).withStored(true));
        live.burst(KindredParticles.DISMISS_RUNE.get(), 16);
        live.playSound(KindredSounds.CHARM_DISMISS.get(), 0.6f, 1.0f);
        live.discard();
        KindredMessages.send(player, "charm_dismissed", name);
    }

    private static void summon(ServerPlayer player, ServerLevel level, CompanionBond bond) {
        if (!bond.stored()) {
            KindredMessages.send(player, "charm_unreachable");
            return;
        }

        int reviveSeconds = bond.reviveSecondsLeft(level.getGameTime());
        if (reviveSeconds > 0) {
            KindredMessages.send(player, "charm_recovering", reviveSeconds);
            return;
        }

        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();
        boolean reviving = bond.reviveReadyAt() > 0;
        CompanionEntity companion =
                create(player, level, snapshot, reviving ? penalisedExperience(snapshot) : snapshot.experience());
        if (companion == null) {
            return;
        }

        UUID bound = bond.companion().orElseThrow();
        if (findEntity(level.getServer(), bound) == null) {
            companion.setUUID(bound);
        }

        if (reviving) companion.setHealth(companion.getMaxHealth());

        if (!level.tryAddFreshEntityWithPassengers(companion)) {
            KindredMessages.send(player, "charm_missing");
            return;
        }

        KindredAttachments.modifyBond(
                player,
                current -> snapshot(current, companion).withStored(false).withReviveReadyAt(0L));
        companion.reportProgress(player);
        if (reviving) {
            companion.milestone(player, CompanionTrigger.Event.REVIVED, 0);
        }

        companion.burst(reviving ? KindredParticles.REVIVE_BLOOM.get() : KindredParticles.SUMMON_RUNE.get(), 16);
        companion.playSound(reviving ? KindredSounds.CHARM_REVIVE.get() : KindredSounds.CHARM_SUMMON.get(), 0.6f, 1.0f);
        KindredMessages.send(player, reviving ? "charm_revived" : "charm_summoned", companion.getDisplayName());
    }

    private static @Nullable CompanionEntity create(
            ServerPlayer player, ServerLevel level, CompanionSnapshot snapshot, int experience) {
        Optional<CompanionSpecies> species = snapshot.resolveSpecies();

        if (species.isEmpty()) {
            KindredMessages.send(player, "charm_missing");
            return null;
        }

        CompanionEntity companion = KindredEntities.type(species.get()).create(level);
        if (companion == null) {
            KindredMessages.send(player, "charm_missing");
            return null;
        }

        companion.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        companion.tame(player);
        snapshot.name().ifPresent(name -> companion.setCustomName(Component.literal(name)));
        companion.setSkinName(snapshot.skin().orElse(""));
        companion.setDyeId(snapshot.dye());
        companion.restoreProgress(snapshot, experience);
        companion.setCommand(snapshot.commandValue());
        companion.setAggression(snapshot.aggressionValue());
        companion.setBonded(true);
        return companion;
    }

    private static int penalisedExperience(CompanionSnapshot snapshot) {
        double kept = 1.0 - KindredConfig.COMMON.reviveExperiencePenalty.get();
        return Math.max(0, (int) Math.floor(snapshot.experience() * kept));
    }

    public static void onCompanionDied(CompanionEntity companion) {
        if (!companion.isBonded() || companion.getOwnerUUID() == null) return;
        int cooldownTicks = CompanionBondMath.reviveCooldownTicks(companion.getBondLevel());
        long readyAt = companion.level().getGameTime() + Math.max(1, cooldownTicks);
        if (!(companion.getOwner() instanceof ServerPlayer owner)) {
            if (companion.level() instanceof ServerLevel level) {
                CompanionWorldData.recordDeath(
                        level,
                        new CompanionWorldData.Death(
                                companion.getOwnerUUID(),
                                companion.getUUID(),
                                CompanionSnapshot.of(companion),
                                readyAt));
            }
            return;
        }
        if (!KindredAttachments.bond(owner).isBoundTo(companion.getUUID())) return;

        KindredAttachments.modifyBond(
                owner, current -> snapshot(current, companion).withStored(true).withReviveReadyAt(readyAt));

        KindredMessages.send(
                owner, "charm_companion_lost", companion.getDisplayName(), Math.max(1, cooldownTicks / 20));
    }

    public static CompanionBond snapshot(CompanionBond bond, CompanionEntity companion) {
        return bond.withSnapshot(companion.getUUID(), CompanionSnapshot.of(companion))
                .withLocation(companion.level().dimension().location(), companion.blockPosition());
    }

    private static @Nullable CompanionEntity findCompanion(MinecraftServer server, CompanionBond bond) {
        if (bond.stored()) return null;
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

        // Entity deserialization trails chunk loading. Keep the last-known chunk
        // available for subsequent screen refreshes without blocking the server tick.
        level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(pos), 0, pos);
        return null;
    }

    private static @Nullable ServerLevel lastKnownLevel(MinecraftServer server, CompanionBond bond) {
        ResourceLocation dimension = bond.lastDimension().orElse(null);
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
        CompanionWorldData.reconcile(player);
        CompanionBond bond = KindredAttachments.bond(player);

        if (!bond.isBound()) {
            return false;
        }

        CompanionEntity live = findCompanion(player.level().getServer(), bond);
        release(player, bond, live);
        return true;
    }

    public static void releaseBond(Player player) {
        KindredAttachments.modifyBond(player, current -> CompanionBond.NONE);

        player.getCooldowns().removeCooldown(KindredItems.KINDRED_CHARM.get());
    }
}
