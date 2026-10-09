package com.breakinblocks.kindredspirits.item;

import com.breakinblocks.kindredspirits.companion.CompanionLevels.AttributeBonus;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class KindredEquipmentItem extends Item {
    private final List<AttributeBonus> bonuses;

    public KindredEquipmentItem(Properties properties, List<AttributeBonus> bonuses) {
        super(properties);
        this.bonuses = bonuses;
    }

    public List<AttributeBonus> bonuses() {
        return this.bonuses;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc"));
    }
}
