package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.breakinblocks.kindredspirits.registry.KindredTags;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

public class CompanionEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPERIENCE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_COMMAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_AGGRESSION =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_BONDED =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    public static final String MAIN_CONTROLLER = "main";
    public static final String ACTION_CONTROLLER = "action";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(CompanionAnimations.IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(CompanionAnimations.WALK);
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(CompanionAnimations.RUN);
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop(CompanionAnimations.SIT);
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.ATTACK);
    private static final RawAnimation SPECIAL_ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.SPECIAL_ATTACK);
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay(CompanionAnimations.HURT);
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(CompanionAnimations.DEATH);
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay(CompanionAnimations.SPAWN);
    private static final RawAnimation INTERACT = RawAnimation.begin().thenPlay(CompanionAnimations.INTERACT);

    private static final int SPAWN_ANIMATION_TICK = 3;
    private static final int LOCATION_UPDATE_INTERVAL = 100;

    private final CompanionSpecies species;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private boolean playedSpawnAnimation;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level, CompanionSpecies species) {
        super(type, level);
        this.species = species;
        this.setTame(false, false);

        if (level instanceof ServerLevel) {
            Goal attack = species.combatStyle() == CompanionSpecies.CombatStyle.RANGED
                    ? new CompanionRangedAttackGoal(this, 1.1)
                    : new MeleeAttackGoal(this, 1.2, true);
            this.goalSelector.addGoal(3, this.gated(attack, () -> true));
        }
    }

    public static AttributeSupplier.Builder createCompanionAttributes(CompanionSpecies species) {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, species.baseHealth())
                .add(Attributes.MOVEMENT_SPEED, species.moveSpeed())
                .add(Attributes.ATTACK_DAMAGE, species.attackDamage())
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public CompanionSpecies species() {
        return this.species;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.1, 8.0f, 3.0f) {
            @Override
            public boolean canUse() {
                return CompanionEntity.this.getCommand() == CompanionCommand.FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return CompanionEntity.this.getCommand() == CompanionCommand.FOLLOW && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, gated(new OwnerHurtByTargetGoal(this),
                () -> this.getAggression().defendsOwner()));
        this.targetSelector.addGoal(2, gated(new OwnerHurtTargetGoal(this),
                () -> this.getAggression().joinsOwnerAttacks()));
        this.targetSelector.addGoal(3, gated(new HurtByTargetGoal(this).setAlertOthers(),
                () -> this.getAggression().defendsSelf()));
        this.targetSelector.addGoal(4, gated(
                new NearestAttackableTargetGoal<>(this, Mob.class, true, this::isHuntable),
                () -> this.getAggression().huntsMonsters()));
    }

    private boolean isHuntable(LivingEntity target, ServerLevel level) {
        if (!(target instanceof Enemy) || target instanceof CompanionEntity) {
            return false;
        }

        return this.species().huntsDangerousPrey()
                || !target.getType().builtInRegistryHolder().is(KindredTags.DANGEROUS_PREY);
    }

    private Goal gated(Goal goal, BooleanSupplier allowed) {
        return new Goal() {
            {
                this.setFlags(goal.getFlags());
            }

            @Override
            public boolean canUse() {
                return CompanionEntity.this.isBonded() && allowed.getAsBoolean() && goal.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return CompanionEntity.this.isBonded() && allowed.getAsBoolean() && goal.canContinueToUse();
            }

            @Override
            public boolean isInterruptable() {
                return goal.isInterruptable();
            }

            @Override
            public void start() {
                goal.start();
            }

            @Override
            public void stop() {
                goal.stop();
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return goal.requiresUpdateEveryTick();
            }

            @Override
            public void tick() {
                goal.tick();
            }
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_LEVEL, CompanionLevels.MIN_LEVEL);
        entityData.define(DATA_EXPERIENCE, 0);
        entityData.define(DATA_BOND, 0);
        entityData.define(DATA_COMMAND, (byte) CompanionCommand.FOLLOW.ordinal());
        entityData.define(DATA_AGGRESSION, (byte) CompanionAggression.NEUTRAL.ordinal());
        entityData.define(DATA_BONDED, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Level", this.getLevel());
        output.putInt("Experience", this.getExperience());
        output.putInt("Bond", this.getBond());
        output.putString("Command", this.getCommand().getSerializedName());
        output.putString("Aggression", this.getAggression().getSerializedName());
        output.putBoolean("PlayedSpawnAnimation", this.playedSpawnAnimation);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_LEVEL, Math.max(CompanionLevels.MIN_LEVEL, input.getIntOr("Level", CompanionLevels.MIN_LEVEL)));
        this.entityData.set(DATA_EXPERIENCE, input.getIntOr("Experience", 0));
        this.entityData.set(DATA_BOND, input.getIntOr("Bond", 0));
        this.playedSpawnAnimation = input.getBooleanOr("PlayedSpawnAnimation", true);
        this.entityData.set(DATA_AGGRESSION, (byte) CompanionAggression
                .byName(input.getStringOr("Aggression", CompanionAggression.NEUTRAL.getSerializedName())).ordinal());

        String command = input.getStringOr("Command", CompanionCommand.FOLLOW.getSerializedName());
        for (CompanionCommand value : CompanionCommand.values()) {
            if (value.getSerializedName().equals(command)) {
                this.entityData.set(DATA_COMMAND, (byte) value.ordinal());
                break;
            }
        }

        this.applyLevelScaling(false);
    }

    public int getLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public int getExperience() {
        return this.entityData.get(DATA_EXPERIENCE);
    }

    public int getBond() {
        return this.entityData.get(DATA_BOND);
    }

    public CompanionCommand getCommand() {
        return CompanionCommand.byOrdinal(this.entityData.get(DATA_COMMAND));
    }

    public void setCommand(CompanionCommand command) {
        this.entityData.set(DATA_COMMAND, (byte) command.ordinal());
        this.setOrderedToSit(command == CompanionCommand.STAY);
    }

    public CompanionAggression getAggression() {
        return CompanionAggression.byOrdinal(this.entityData.get(DATA_AGGRESSION));
    }

    public void setAggression(CompanionAggression aggression) {
        this.entityData.set(DATA_AGGRESSION, (byte) aggression.ordinal());

        if (!aggression.defendsSelf()) {
            this.setTarget(null);
        }
    }

    public boolean isBonded() {
        return this.entityData.get(DATA_BONDED);
    }

    public void setBonded(boolean bonded) {
        this.entityData.set(DATA_BONDED, bonded);

        if (!bonded) {
            this.setTarget(null);
        }
    }

    public void addBond(int amount) {
        this.entityData.set(DATA_BOND, Math.clamp(this.getBond() + amount, 0, CompanionLevels.bondCap()));
    }

    public List<CompanionAbility> unlockedAbilities() {
        return this.species.abilitiesAt(this.getLevel());
    }

    public int experienceToNextLevel() {
        return CompanionLevels.experienceToNext(this.getLevel());
    }

    public void addExperience(int amount) {
        if (this.level().isClientSide() || amount <= 0 || !this.isBonded()) {
            return;
        }

        int level = this.getLevel();
        int experience = this.getExperience() + amount;

        while (level < CompanionLevels.maxLevel() && experience >= CompanionLevels.experienceToNext(level)) {
            experience -= CompanionLevels.experienceToNext(level);
            level++;
        }

        if (level >= CompanionLevels.maxLevel()) {
            level = CompanionLevels.maxLevel();
            experience = 0;
        }

        boolean levelledUp = level > this.getLevel();
        this.entityData.set(DATA_LEVEL, level);
        this.entityData.set(DATA_EXPERIENCE, experience);

        if (levelledUp) {
            this.applyLevelScaling(true);
            this.playSound(SoundEvents.PLAYER_LEVELUP, 0.7f, 1.4f);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
            }

            if (this.getOwner() instanceof ServerPlayer owner) {
                owner.sendSystemMessage(Component.translatable("message.kindredspirits.level_up",
                        this.getDisplayName(), level));
                KindredNetworking.sendLevelUp(owner, this.getId(), level);
            }
        }
    }

    public void refreshBondState() {
        if (!(this.getOwner() instanceof Player owner)) {
            this.setBonded(false);
            return;
        }

        CompanionBond bond = KindredAttachments.bond(owner);
        boolean bonded = bond.isBound() && bond.companion().get().equals(this.getUUID());
        this.setBonded(bonded);

        if (bonded) {
            KindredAttachments.modifyBond(owner, current -> KindredCharmItem.snapshot(current, this).withStored(false));
        }
    }

    public void restoreProgress(int level, int experience, int bond) {
        this.entityData.set(DATA_LEVEL, Math.clamp(level, CompanionLevels.MIN_LEVEL, CompanionLevels.maxLevel()));
        this.entityData.set(DATA_EXPERIENCE, Math.max(0, experience));
        this.entityData.set(DATA_BOND, Math.clamp(bond, 0, CompanionLevels.bondCap()));
        this.applyLevelScaling(true);
    }

    public void applyLevelScaling(boolean healToFull) {
        int level = this.getLevel();

        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(CompanionLevels.healthAt(this.species, level));
        }

        var attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(CompanionLevels.attackDamageAt(this.species, level));
        }

        if (healToFull) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        if (!this.playedSpawnAnimation && this.tickCount >= SPAWN_ANIMATION_TICK) {
            this.playedSpawnAnimation = true;
            this.playCompanionAnim(CompanionAnimations.SPAWN);
        }

        this.setSprinting(this.getTarget() != null);

        if (this.isAlive() && this.tickCount % LOCATION_UPDATE_INTERVAL == 0) {
            this.refreshBondState();
        }

        if (!this.isBonded() || !KindredConfig.COMMON.abilitiesEnabled.get()) {
            return;
        }

        ServerPlayer owner = this.getOwner() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        for (CompanionAbility ability : this.unlockedAbilities()) {
            if (this.tickCount % ability.intervalTicks() == 0) {
                ability.serverTick(this, owner);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof KindredCharmItem) {
            return InteractionResult.PASS;
        }

        if (this.isTame() && this.isOwnedBy(player)) {
            if (player.isSecondaryUseActive()) {
                if (!this.level().isClientSide()) {
                    CompanionCommand command = this.getCommand().next();
                    this.setCommand(command);
                    player.sendSystemMessage(Component.translatable("message.kindredspirits.command_set",
                            this.getDisplayName(), command.displayName()));
                }
                return InteractionResult.SUCCESS;
            }

            if (this.isFood(stack)) {
                if (!this.level().isClientSide()) {
                    stack.consume(1, player);
                    this.addExperience(KindredConfig.COMMON.experiencePerFeed.get());
                    this.addBond(1);
                    this.heal(2.0f);
                    this.playCompanionAnim(CompanionAnimations.INTERACT);
                    this.playSound(this.species.sounds().interact(), 0.8f, 1.0f);
                }
                return InteractionResult.SUCCESS;
            }
        } else if (!this.isTame() && this.isFood(stack)) {
            if (!this.level().isClientSide()) {
                stack.consume(1, player);

                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setCommand(CompanionCommand.FOLLOW);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(KindredTags.COMPANION_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return !(target instanceof CompanionEntity companion && companion.isOwnedBy(owner));
    }

    public void playCompanionAnim(String animation) {
        if (this.species.hasAnimation(animation)) {
            this.triggerAnim(ACTION_CONTROLLER, animation);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.species.combatStyle() == CompanionSpecies.CombatStyle.RANGED) {
            return false;
        }

        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            this.playCompanionAnim(CompanionAnimations.ATTACK);
            this.playSound(this.species.sounds().attack(), 0.9f, 1.0f);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.playCompanionAnim(CompanionAnimations.HURT);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            this.playCompanionAnim(CompanionAnimations.DEATH);
            KindredCharmItem.onCompanionDied(this);
        }
        super.die(source);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.species.sounds().ambient();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return this.species.sounds().hurt();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return this.species.sounds().death();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CompanionEntity> action =
                new AnimationController<CompanionEntity>(ACTION_CONTROLLER, 3, test -> PlayState.STOP);

        addTriggerable(action, CompanionAnimations.ATTACK, ATTACK);
        addTriggerable(action, CompanionAnimations.SPECIAL_ATTACK, SPECIAL_ATTACK);
        addTriggerable(action, CompanionAnimations.HURT, HURT);
        addTriggerable(action, CompanionAnimations.DEATH, DEATH);
        addTriggerable(action, CompanionAnimations.SPAWN, SPAWN);
        addTriggerable(action, CompanionAnimations.INTERACT, INTERACT);

        controllers.add(new AnimationController<CompanionEntity>(MAIN_CONTROLLER, 5, this::animateMain));
        controllers.add(action.receiveTriggeredAnimations());
    }

    private void addTriggerable(AnimationController<CompanionEntity> controller, String name, RawAnimation animation) {
        if (this.species.hasAnimation(name)) {
            controller.triggerableAnim(name, animation);
        }
    }

    private PlayState animateMain(AnimationTest<CompanionEntity> test) {
        CompanionEntity companion = test.animatable();
        CompanionSpecies species = companion.species();

        if (companion.isInSittingPose() && species.hasAnimation(CompanionAnimations.SIT)) {
            return test.setAndContinue(SIT);
        }
        if (!test.isMoving()) {
            return test.setAndContinue(IDLE);
        }
        if (companion.isSprinting() && species.hasAnimation(CompanionAnimations.RUN)) {
            return test.setAndContinue(RUN);
        }
        return test.setAndContinue(species.hasAnimation(CompanionAnimations.WALK) ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
