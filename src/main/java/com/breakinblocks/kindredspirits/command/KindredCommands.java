package com.breakinblocks.kindredspirits.command;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
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
                .then(Commands.literal("xp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> grantExperience(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount"))))));
    }

    private static int reportCompanion(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }

        Optional<CompanionEntity> nearest = nearestCompanion(player);
        if (nearest.isEmpty()) {
            source.sendFailure(Component.translatable("command.kindredspirits.no_companion"));
            return 0;
        }

        CompanionEntity companion = nearest.get();
        String abilities = companion.unlockedAbilities().stream()
                .map(CompanionAbility::id)
                .map(id -> id.getPath())
                .collect(Collectors.joining(", "));

        source.sendSuccess(() -> Component.translatable("command.kindredspirits.info",
                companion.getDisplayName(),
                companion.getLevel(),
                companion.getExperience(),
                companion.experienceToNextLevel(),
                companion.getBond(),
                abilities.isEmpty() ? "-" : abilities), false);
        return companion.getLevel();
    }

    private static int grantExperience(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.kindredspirits.player_only"));
            return 0;
        }

        Optional<CompanionEntity> nearest = nearestCompanion(player);
        if (nearest.isEmpty()) {
            source.sendFailure(Component.translatable("command.kindredspirits.no_companion"));
            return 0;
        }

        CompanionEntity companion = nearest.get();
        companion.addExperience(amount);
        source.sendSuccess(() -> Component.translatable("command.kindredspirits.xp_granted",
                amount, companion.getDisplayName(), companion.getLevel()), true);
        return amount;
    }

    private static Optional<CompanionEntity> nearestCompanion(ServerPlayer player) {
        AABB search = player.getBoundingBox().inflate(SEARCH_RANGE);
        return player.level().getEntitiesOfClass(CompanionEntity.class, search,
                        companion -> companion.isTame() && companion.isOwnedBy(player)).stream()
                .min(Comparator.comparingDouble(companion -> companion.distanceToSqr(player)));
    }

    private KindredCommands() {
    }
}
