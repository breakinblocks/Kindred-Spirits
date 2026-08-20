package com.breakinblocks.kindredspirits.companion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CompanionRangedAttackGoal extends Goal {
    private static final double PREFERRED_RANGE = 8.0;
    private static final double MINIMUM_RANGE = 4.0;
    private static final double MAXIMUM_RANGE = 15.0;
    private static final int STRAFE_SWITCH_TICKS = 40;

    private final CompanionEntity companion;
    private final double speedModifier;
    private int seeTime;
    private int strafeTicks;
    private boolean strafeClockwise;

    public CompanionRangedAttackGoal(CompanionEntity companion, double speedModifier) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.companion.getTarget();
        return target != null && target.isAlive() && !this.companion.isInSittingPose();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() && this.companion.distanceToSqr(this.companion.getTarget()) < MAXIMUM_RANGE * MAXIMUM_RANGE;
    }

    @Override
    public void start() {
        this.seeTime = 0;
        this.strafeTicks = 0;
    }

    @Override
    public void stop() {
        this.companion.getNavigation().stop();
        this.companion.setAggressive(false);
        this.seeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.companion.getTarget();
        if (target == null) {
            return;
        }

        double distanceSqr = this.companion.distanceToSqr(target);
        boolean canSee = this.companion.hasLineOfSight(target);
        this.seeTime = canSee ? this.seeTime + 1 : 0;

        this.companion.getLookControl().setLookAt(target, 30.0f, 30.0f);
        this.companion.setAggressive(true);

        if (distanceSqr > PREFERRED_RANGE * PREFERRED_RANGE || this.seeTime < 5) {
            this.companion.getNavigation().moveTo(target, this.speedModifier);
            return;
        }

        this.companion.getNavigation().stop();

        if (++this.strafeTicks >= STRAFE_SWITCH_TICKS) {
            this.strafeTicks = 0;
            this.strafeClockwise = !this.strafeClockwise;
        }

        float backwards = distanceSqr < MINIMUM_RANGE * MINIMUM_RANGE ? -0.5f : 0.0f;
        this.companion.getMoveControl().strafe(backwards, this.strafeClockwise ? 0.5f : -0.5f);
    }
}
