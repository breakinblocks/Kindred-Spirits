package com.breakinblocks.kindredspirits.command;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.Optional;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class KindredCommands {
    private static final double SEARCH_RANGE = 32.0;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(KindredSpirits.MOD_ID)
                .then(Commands.literal("info")
                        .executes(context -> reportCompanion(context.getSource())))
                .then(Commands.literal("release")
                        .executes(context -> releaseBond(context.getSource())))
                .then(Commands.literal("smelt")
                        .executes(context -> useAbility(context.getSource())))
                .then(Commands.literal("xp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> grantExperience(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount")))))
                .then(Commands.literal("bond")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("points", IntegerArgumentType.integer(1))
                                .executes(context -> grantBond(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "points")))))
                .then(Commands.literal("resetrevive")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> resetRevive(context.getSource(),
                                        EntityArgument.getPlayers(context, "players"))))));
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

    private static int releaseBond(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }

        if (!KindredCharmItem.releaseFully(player)) {
            source.sendFailure(Component.translatable("message.kindredspirits.charm_empty"));
            return 0;
        }

        return 1;
    }

    private static int useAbility(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }

        CompanionBond bond = KindredAttachments.bond(player);
        if (!bond.isBound()) {
            source.sendFailure(Component.translatable("message.kindredspirits.charm_empty"));
            return 0;
        }

        Optional<CompanionEntity> nearest = CompanionEntity.nearestOwned(player, SEARCH_RANGE)
                .filter(CompanionEntity::isBonded);
        return KindredCharmItem.useAbility(player, bond, nearest.orElse(null)) ? 1 : 0;
    }

    private static int reportCompanion(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }

        Optional<CompanionEntity> nearest = CompanionEntity.nearestOwned(player, SEARCH_RANGE);
        if (nearest.isEmpty()) {
            source.sendFailure(Component.translatable("command.kindredspirits.no_companion"));
            return 0;
        }

        CompanionEntity companion = nearest.get();
        List<CompanionAbility> unlocked = companion.unlockedAbilities();
        String abilities = companion.species().unlocks().stream()
                .map(unlock -> unlocked.contains(unlock.ability())
                        ? unlock.ability().id().getPath()
                        : unlock.ability().id().getPath() + " (" + unlock.requirement().getString() + ")")
                .collect(Collectors.joining(", "));

        int bondPoints = companion.getBondPoints();
        source.sendSuccess(() -> Component.translatable("command.kindredspirits.info",
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
                abilities.isEmpty() ? "-" : abilities), false);
        return companion.getLevel();
    }

    private static int grantExperience(CommandSourceStack source, int amount) {
        return grant(source, amount, CompanionEntity::addExperience, "command.kindredspirits.xp_granted",
                CompanionEntity::getLevel);
    }

    private static int grantBond(CommandSourceStack source, int points) {
        return grant(source, points, CompanionEntity::addBondPoints, "command.kindredspirits.bond_granted",
                CompanionEntity::getBondLevel);
    }

    private static int grant(CommandSourceStack source, int amount, BiConsumer<CompanionEntity, Integer> apply,
                             String key, Function<CompanionEntity, Integer> result) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }

        Optional<CompanionEntity> nearest = CompanionEntity.nearestOwned(player, SEARCH_RANGE);
        if (nearest.isEmpty()) {
            source.sendFailure(Component.translatable("command.kindredspirits.no_companion"));
            return 0;
        }

        CompanionEntity companion = nearest.get();
        apply.accept(companion, amount);
        source.sendSuccess(() -> Component.translatable(key,
                amount, companion.getDisplayName(), result.apply(companion)), true);
        return amount;
    }

    private KindredCommands() {
    }
}
