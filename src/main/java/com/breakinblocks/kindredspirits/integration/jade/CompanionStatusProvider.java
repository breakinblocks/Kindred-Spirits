package com.breakinblocks.kindredspirits.integration.jade;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CompanionStatusProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final Identifier UID = KindredSpirits.id("companion_status");

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof CompanionEntity companion)) {
            return;
        }

        tooltip.add(Component.translatable("jade.kindredspirits.level",
                companion.getLevel(), CompanionLevels.maxLevel()).withStyle(ChatFormatting.GOLD));

        if (companion.getLevel() < CompanionLevels.maxLevel()) {
            tooltip.add(Component.translatable("jade.kindredspirits.experience",
                    companion.getExperience(), companion.experienceToNextLevel()));
        }

        tooltip.add(Component.translatable("jade.kindredspirits.bond",
                companion.getBond(), CompanionLevels.bondCap()));

        tooltip.add(Component.translatable("jade.kindredspirits.command", companion.getCommand().displayName()));

        if (KindredConfig.CLIENT.showAbilityTooltips.get()) {
            for (CompanionAbility ability : companion.unlockedAbilities()) {
                tooltip.add(Component.literal("- ").append(ability.displayName()).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
