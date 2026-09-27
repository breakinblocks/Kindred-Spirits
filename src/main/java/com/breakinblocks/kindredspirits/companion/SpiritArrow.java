package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.registry.KindredEntities;
import com.breakinblocks.kindredspirits.registry.KindredParticles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class SpiritArrow extends AbstractArrow {
    private static final int FADE_TICKS = 40;

    public SpiritArrow(EntityType<? extends SpiritArrow> type, Level level) {
        super(type, level);
    }

    public SpiritArrow(Level level, LivingEntity owner, ItemStack weapon) {
        super(KindredEntities.SPIRIT_ARROW.get(), owner, level, new ItemStack(Items.ARROW), weapon);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (!this.isInGround()) {
                this.level().addParticle(KindredParticles.CRAFT_SPARK.get(), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        } else if (this.isInGround() && this.inGroundTime >= FADE_TICKS) {
            this.discard();
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}
