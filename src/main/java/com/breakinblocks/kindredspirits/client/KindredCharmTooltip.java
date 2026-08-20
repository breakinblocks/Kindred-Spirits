package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID, value = Dist.CLIENT)
public final class KindredCharmTooltip {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof KindredCharmItem) || event.getEntity() == null) {
            return;
        }

        CompanionBond bond = KindredAttachments.bond(event.getEntity());

        if (!bond.isBound()) {
            event.getToolTip().add(Component.translatable("tooltip.kindredspirits.charm_unbound")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();

        event.getToolTip().add(Component.translatable("tooltip.kindredspirits.charm_species",
                snapshot.displayName()).withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("tooltip.kindredspirits.charm_level",
                snapshot.level(), CompanionLevels.experienceToNext(snapshot.level()))
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.kindredspirits.charm_bond",
                snapshot.bond()).withStyle(ChatFormatting.GRAY));

        long gameTime = event.getEntity().level().getGameTime();
        if (bond.reviveReadyAt() > gameTime) {
            event.getToolTip().add(Component.translatable("tooltip.kindredspirits.charm_recovering",
                    Math.max(1, (bond.reviveReadyAt() - gameTime) / 20)).withStyle(ChatFormatting.RED));
        } else {
            event.getToolTip().add(Component.translatable(bond.stored()
                    ? "tooltip.kindredspirits.charm_resting"
                    : "tooltip.kindredspirits.charm_out").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private KindredCharmTooltip() {
    }
}
