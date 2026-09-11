package com.breakinblocks.kindredspirits.companion;

import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Replans away from the attacker throughout the ten-second escape window. */
final class QuokkaFleeGoal extends PanicGoal {
    private final CompanionEntity companion;

    QuokkaFleeGoal(CompanionEntity companion) {
        super(companion, 1.5);
        this.companion = companion;
    }

    @Override
    protected boolean shouldPanic() { return this.companion.isQuokkaFleeing(); }

    @Override
    public boolean canContinueToUse() { return this.shouldPanic() && super.canContinueToUse(); }

    @Override
    protected boolean findRandomPosition() {
        var attacker = this.companion.getLastHurtByMob();
        Vec3 away = attacker == null ? null
                : DefaultRandomPos.getPosAway(this.companion, 10, 4, attacker.position());
        if (away == null) return super.findRandomPosition();
        this.posX = away.x;
        this.posY = away.y;
        this.posZ = away.z;
        return true;
    }
}
