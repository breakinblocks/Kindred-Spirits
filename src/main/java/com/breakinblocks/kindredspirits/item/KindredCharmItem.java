package com.breakinblocks.kindredspirits.item;

import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredDataComponents;
import com.breakinblocks.kindredspirits.registry.KindredDataComponents.CompanionSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Consumer;

public class KindredCharmItem extends Item {
    public KindredCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof CompanionEntity companion) || !companion.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide()) {
            boolean newBond = !companion.getUUID().equals(stack.get(KindredDataComponents.BOUND_COMPANION.get()));
            bind(stack, companion);

            if (newBond) {
                KindredAttachments.modify(player, record -> record.withBonded(record.companionsBonded() + 1));
            }

            player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_bound", companion.getDisplayName()));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID bound = stack.get(KindredDataComponents.BOUND_COMPANION.get());

        if (bound == null) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_empty"));
            }
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(bound);

            if (entity instanceof CompanionEntity companion) {
                companion.teleportTo(player.getX(), player.getY(), player.getZ());
                bind(stack, companion);
                player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_recalled", companion.getDisplayName()));
            } else {
                player.sendSystemMessage(Component.translatable("message.kindredspirits.charm_missing"));
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        CompanionSnapshot snapshot = stack.get(KindredDataComponents.COMPANION_SNAPSHOT.get());

        if (snapshot == null) {
            lines.accept(Component.translatable("tooltip.kindredspirits.charm_unbound").withStyle(ChatFormatting.GRAY));
            return;
        }

        snapshot.resolveSpecies().ifPresent(species ->
                lines.accept(Component.translatable("tooltip.kindredspirits.charm_species",
                        Component.translatable(species.translationKey())).withStyle(ChatFormatting.GOLD)));

        lines.accept(Component.translatable("tooltip.kindredspirits.charm_level", snapshot.level()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("tooltip.kindredspirits.charm_bond", snapshot.bond()).withStyle(ChatFormatting.GRAY));
    }

    private static void bind(ItemStack stack, CompanionEntity companion) {
        stack.set(KindredDataComponents.BOUND_COMPANION.get(), companion.getUUID());
        stack.set(KindredDataComponents.COMPANION_SNAPSHOT.get(), new CompanionSnapshot(
                companion.species().getSerializedName(), companion.getLevel(), companion.getBond()));
    }
}
