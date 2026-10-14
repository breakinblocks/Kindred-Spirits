package com.breakinblocks.kindredspirits.command;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionProgressEvents;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class KindredCommands {
    private static final double SEARCH_RANGE = 32.0;
    private static final int MAX_MINIME_NAME = 32;
    private static final String FALLBACK_SKIN = "MHF_Steve";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher()
                .register(Commands.literal(KindredSpirits.MOD_ID)
                        .then(Commands.literal("info").executes(context -> reportCompanion(context.getSource())))
                        .then(Commands.literal("release").executes(context -> releaseBond(context.getSource())))
                        .then(Commands.literal("smelt").executes(context -> useAbility(context.getSource())))
                        .then(Commands.literal("skin")
                                .executes(context -> setSkin(context.getSource(), ""))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> setSkin(
                                                context.getSource(), StringArgumentType.getString(context, "player")))))
                        .then(Commands.literal("minime")
                                .requires(KindredCommands::canUseMinime)
                                .executes(context -> spawnMinime(context.getSource(), "", ""))
                                .then(Commands.argument("mc_name", StringArgumentType.string())
                                        .executes(context -> spawnMinime(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "mc_name"),
                                                ""))
                                        .then(Commands.argument("custom_name", StringArgumentType.string())
                                                .executes(context -> spawnMinime(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "mc_name"),
                                                        StringArgumentType.getString(context, "custom_name"))))))
                        .then(Commands.literal("xp")
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> grantExperience(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "amount")))))
                        .then(Commands.literal("bond")
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("points", IntegerArgumentType.integer(1))
                                        .executes(context -> grantBond(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "points")))))
                        .then(Commands.literal("resetrevive")
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> resetRevive(
                                                context.getSource(), EntityArgument.getPlayers(context, "players"))))));
    }

    private static boolean canUseMinime(CommandSourceStack source) {
        int level = KindredConfig.COMMON.minimePermissionLevel.get();
        return level <= 0 || source.hasPermission(level);
    }

    private static int spawnMinime(CommandSourceStack source, String mcName, String customName) {
        return withPlayer(source, player -> {
            String skinName = mcName.trim().isEmpty() ? player.getGameProfile().getName() : mcName.trim();
            String displayName = customName.replace('§', ' ').trim();
            if (displayName.isEmpty()) {
                displayName = skinName;
            }
            if (skinName.length() > MAX_MINIME_NAME || displayName.length() > MAX_MINIME_NAME) {
                source.sendFailure(Component.translatable("command.kindredspirits.minime_invalid_name"));
                return 0;
            }

            ServerLevel level = player.serverLevel();
            CompanionEntity minime =
                    KindredEntities.type(CompanionSpecies.MINI_PLAYER).create(level);
            if (minime == null) {
                source.sendFailure(Component.translatable("command.kindredspirits.minime_failed"));
                return 0;
            }

            minime.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
            minime.tame(player);
            minime.setSkinName(StringUtil.isValidPlayerName(skinName) ? skinName : FALLBACK_SKIN);
            minime.setCustomName(Component.literal(displayName));
            minime.setCustomNameVisible(true);
            minime.setCommand(CompanionCommand.WANDER);
            minime.setPersistenceRequired();

            if (!level.tryAddFreshEntityWithPassengers(minime)) {
                source.sendFailure(Component.translatable("command.kindredspirits.minime_failed"));
                return 0;
            }

            source.sendSuccess(
                    () -> Component.translatable("command.kindredspirits.minime_spawned", minime.getDisplayName()),
                    false);
            return 1;
        });
    }

    private static int resetRevive(CommandSourceStack source, Collection<ServerPlayer> players) {
        int cleared = 0;

        for (ServerPlayer player : players) {
            CompanionBond bond = KindredAttachments.bond(player);

            if (!bond.isBound() || bond.reviveReadyAt() <= 0L) {
                continue;
            }

            KindredAttachments.modifyBond(player, current -> current.withReviveReadyAt(0L));
            KindredNetworking.sendCharmView(player, false);
            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_revive_ready"));
            cleared++;
        }

        if (cleared == 0) {
            source.sendFailure(Component.translatable("command.kindredspirits.revive_not_waiting"));
            return 0;
        }

        int count = cleared;
        source.sendSuccess(() -> Component.translatable("command.kindredspirits.revive_reset", count), true);
        return cleared;
    }

    private static int withPlayer(CommandSourceStack source, ToIntFunction<ServerPlayer> body) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }
        return body.applyAsInt(player);
    }

    private static int withCompanion(CommandSourceStack source, ToIntBiFunction<ServerPlayer, CompanionEntity> body) {
        return withPlayer(
                source,
                player -> CompanionEntity.bondedNear(player, SEARCH_RANGE)
                        .map(companion -> body.applyAsInt(player, companion))
                        .orElseGet(() -> {
                            source.sendFailure(Component.translatable("command.kindredspirits.no_companion"));
                            return 0;
                        }));
    }

    private static int releaseBond(CommandSourceStack source) {
        return withPlayer(source, player -> {
            if (!KindredCharmItem.releaseFully(player)) {
                source.sendFailure(Component.translatable("message.kindredspirits.charm_empty"));
                return 0;
            }
            return 1;
        });
    }

    private static int setSkin(CommandSourceStack source, String name) {
        return withPlayer(source, player -> {
            CompanionBond bond = KindredAttachments.bond(player);
            if (!bond.isBound()) {
                source.sendFailure(Component.translatable("message.kindredspirits.charm_empty"));
                return 0;
            }
            if (!bond.snapshot().orElseThrow().usesPlayerSkin()) {
                source.sendFailure(Component.translatable("command.kindredspirits.not_mini_player"));
                return 0;
            }

            KindredCharmItem.handleAction(player, KindredNetworking.CharmActionPayload.Action.SET_SKIN, 1, name);
            return 1;
        });
    }

    private static int useAbility(CommandSourceStack source) {
        return withPlayer(source, player -> {
            CompanionBond bond = KindredAttachments.bond(player);
            if (!bond.isBound()) {
                source.sendFailure(Component.translatable("message.kindredspirits.charm_empty"));
                return 0;
            }

            CompanionEntity nearest = CompanionProgressEvents.bondedCompanionNear(player, SEARCH_RANGE)
                    .orElse(null);
            return KindredCharmItem.useAbility(player, bond, nearest) ? 1 : 0;
        });
    }

    private static int reportCompanion(CommandSourceStack source) {
        return withCompanion(source, (player, companion) -> reportCompanion(source, companion));
    }

    private static int reportCompanion(CommandSourceStack source, CompanionEntity companion) {
        List<CompanionAbility> unlocked = companion.unlockedAbilities();
        String abilities = companion.species().unlocks().stream()
                .map(unlock -> !unlocked.contains(unlock.ability())
                        ? unlock.label(
                                        Component.literal(unlock.ability().id().getPath()),
                                        companion.getLevel(),
                                        companion.getBondLevel())
                                .getString()
                        : companion.isAbilityDisabled(unlock.ability())
                                ? unlock.ability().id().getPath() + " ("
                                        + Component.translatable("command.kindredspirits.ability_off")
                                                .getString() + ")"
                                : unlock.ability().id().getPath())
                .collect(Collectors.joining(", "));

        int bondPoints = companion.getBondPoints();
        source.sendSuccess(
                () -> Component.translatable(
                        "command.kindredspirits.info",
                        companion.getDisplayName(),
                        companion.getLevel(),
                        companion.getStars(),
                        companion.getExperience(),
                        companion.experienceToNextLevel(),
                        companion.getBondLevel(),
                        CompanionBondMath.pointsIntoLevel(bondPoints),
                        CompanionBondMath.costToNext(bondPoints),
                        companion.progress().saturation(),
                        companion.progress().rested(),
                        companion.equipment().isEmpty()
                                ? Component.translatable("command.kindredspirits.no_equipment")
                                : companion.equipment().getHoverName(),
                        abilities.isEmpty() ? "-" : abilities),
                false);
        return companion.getLevel();
    }

    private static int grantExperience(CommandSourceStack source, int amount) {
        return grant(
                source,
                amount,
                CompanionEntity::addExperience,
                "command.kindredspirits.xp_granted",
                CompanionEntity::getLevel);
    }

    private static int grantBond(CommandSourceStack source, int points) {
        return grant(
                source,
                points,
                CompanionEntity::addBondPoints,
                "command.kindredspirits.bond_granted",
                CompanionEntity::getBondLevel);
    }

    private static int grant(
            CommandSourceStack source,
            int amount,
            BiConsumer<CompanionEntity, Integer> apply,
            String key,
            Function<CompanionEntity, Integer> result) {
        return withCompanion(source, (player, companion) -> {
            apply.accept(companion, amount);
            source.sendSuccess(
                    () -> Component.translatable(key, amount, companion.getDisplayName(), result.apply(companion)),
                    true);
            return amount;
        });
    }

    private KindredCommands() {}
}
