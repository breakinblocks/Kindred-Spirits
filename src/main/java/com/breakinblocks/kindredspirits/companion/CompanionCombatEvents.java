package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionCombatEvents {
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().getOwner() instanceof CompanionEntity companion)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }

        LivingEntity owner = companion.getOwner();
        if (owner == null) {
            return;
        }

        Entity struck = hit.getEntity();
        if (struck == owner || (struck instanceof CompanionEntity other && other.isOwnedBy(owner))) {
            event.setCanceled(true);
        }
    }

    private CompanionCombatEvents() {}
}
