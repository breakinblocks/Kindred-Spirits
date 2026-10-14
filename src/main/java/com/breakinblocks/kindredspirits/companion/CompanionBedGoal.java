package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.block.CompanionBedBlock;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class CompanionBedGoal extends Goal {
    public static final int SEARCH_RADIUS = 16;
    public static final double OWNER_RANGE = 16.0;
    private static final int SEARCH_HEIGHT = 4;
    private static final int SEARCH_INTERVAL = 40;
    private static final int GIVE_UP_TICKS = 400;
    private static final int RETRY_AFTER_GIVING_UP = 200;
    private static final double CENTRED_SQR = 0.3 * 0.3;
    private static final double SPEED = 1.0;

    private final CompanionEntity companion;
    private @Nullable BlockPos bed;
    private int nextSearch;
    private int walking;

    public CompanionBedGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    public static boolean wantsBed(CompanionEntity companion) {
        LivingEntity owner = companion.getOwner();
        return companion.isAlive()
                && companion.isTame()
                && companion.getCommand() != CompanionCommand.STAY
                && companion.getTarget() == null
                && companion.getHealth() < companion.getMaxHealth()
                && !companion.isPassenger()
                && !companion.isVehicle()
                && owner != null
                && owner.level() == companion.level()
                && companion.distanceToSqr(owner) <= OWNER_RANGE * OWNER_RANGE;
    }

    public static @Nullable BlockPos findBed(CompanionEntity companion) {
        Level level = companion.level();
        BlockPos origin = companion.blockPosition();
        Set<BlockPos> taken = new HashSet<>();
        for (CompanionEntity other : level.getEntitiesOfClass(
                CompanionEntity.class,
                new AABB(origin).inflate(SEARCH_RADIUS * 2),
                other -> other != companion && other.isAlive())) {
            if (other.bedPos() != null) {
                taken.add(other.bedPos());
            }
            if (CompanionBedBlock.isBed(level.getBlockState(other.blockPosition()))) {
                taken.add(other.blockPosition());
            }
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RADIUS, -SEARCH_HEIGHT, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS))) {
            if (!CompanionBedBlock.isBed(level.getBlockState(pos)) || taken.contains(pos)) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private boolean inBed() {
        return this.bed != null && this.companion.blockPosition().equals(this.bed);
    }

    @Override
    public boolean canUse() {
        if (this.companion.tickCount < this.nextSearch || !wantsBed(this.companion)) {
            return false;
        }
        this.nextSearch = this.companion.tickCount + SEARCH_INTERVAL;
        this.bed = findBed(this.companion);
        return this.bed != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.bed != null
                && wantsBed(this.companion)
                && CompanionBedBlock.isBed(this.companion.level().getBlockState(this.bed))
                && this.walking < GIVE_UP_TICKS;
    }

    @Override
    public void start() {
        this.walking = 0;
        this.companion.setBedPos(this.bed);
        this.walkToBed();
    }

    @Override
    public void stop() {
        if (this.walking >= GIVE_UP_TICKS) {
            this.nextSearch = this.companion.tickCount + RETRY_AFTER_GIVING_UP;
        }
        this.bed = null;
        this.companion.setBedPos(null);
        this.companion.setInSittingPose(this.companion.isOrderedToSit());
        this.companion.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.bed == null) {
            return;
        }

        if (!this.inBed()) {
            this.walking++;
            this.companion.setInSittingPose(false);
            if (this.companion.getNavigation().isDone()) {
                this.walkToBed();
            }
            return;
        }

        this.walking = 0;
        double x = this.bed.getX() + 0.5;
        double z = this.bed.getZ() + 0.5;
        double dx = this.companion.getX() - x;
        double dz = this.companion.getZ() - z;
        if (dx * dx + dz * dz > CENTRED_SQR) {
            this.companion.getNavigation().stop();
            this.companion.getMoveControl().setWantedPosition(x, this.companion.getY(), z, SPEED);
            return;
        }

        this.companion.getNavigation().stop();
        this.companion.setInSittingPose(true);
    }

    private void walkToBed() {
        if (this.bed != null) {
            this.companion
                    .getNavigation()
                    .moveTo(
                            this.bed.getX() + 0.5,
                            this.bed.getY() + CompanionBedBlock.SLEEP_HEIGHT,
                            this.bed.getZ() + 0.5,
                            SPEED);
        }
    }
}
