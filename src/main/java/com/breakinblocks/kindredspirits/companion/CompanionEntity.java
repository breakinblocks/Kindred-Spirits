package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
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
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;

public class CompanionEntity extends TamableAnimal implements GeoEntity, RangedAttackMob {
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
    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    public static final String MAIN_CONTROLLER = "main";
    public static final String ACTION_CONTROLLER = "action";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(CompanionAnimations.IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(CompanionAnimations.WALK);
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(CompanionAnimations.RUN);
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop(CompanionAnimations.SIT);
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.ATTACK);
    private static final RawAnimation SPECIAL_ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.SPECIAL_ATTACK);
    private static final RawAnimation JUMP_ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.JUMP_ATTACK);
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay(CompanionAnimations.SHOOT);
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay(CompanionAnimations.HURT);
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold(CompanionAnimations.DEATH);
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay(CompanionAnimations.SPAWN);
    private static final RawAnimation INTERACT = RawAnimation.begin().thenPlay(CompanionAnimations.INTERACT);

    private static final int SPAWN_ANIMATION_TICK = 3;
    private static final int LOCATION_UPDATE_INTERVAL = 100;
    private static final int LEAP_IMPACT_TIMEOUT = 40;
    private static final int SHOCKWAVE_TICKS = 8;
    private static final int EQUIPMENT_INTERVAL = 20;
    private static final int BOW_INTERVAL = 35;
    private static final double MELEE_SWITCH_RANGE = 3.0;
    private static final double BOW_SWITCH_RANGE = 6.0;
    private static final double BOW_POWER_DIVISOR = 4.0;

    public static final double RUN_DISTANCE = 10.0;
    private static final float HITBOX_YAW_STEP = 5.0f;
    private static final Identifier VANILLA_SPRINT_MODIFIER = Identifier.withDefaultNamespace("sprinting");
    private static final AttributeModifier RUN_SPEED_MODIFIER = new AttributeModifier(
            KindredSpirits.id("running"),
            0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final double SHOCKWAVE_MAX_RADIUS = 4.0;

    private final CompanionSpecies species;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final Object2IntMap<Identifier> abilityCooldowns = new Object2IntOpenHashMap<>();
    private @Nullable LeapImpact pendingImpact;
    private int shockwaveTicks;
    private Vec3 shockwaveOrigin = Vec3.ZERO;
    private boolean playedSpawnAnimation;
    private boolean rangedMode = true;
    private boolean followingOwner;
    private float hitboxYaw = Float.NaN;
    private int bowCooldown;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level, CompanionSpecies species) {
        super(type, level);
        this.species = species;
        this.setTame(false, false);

        if (level instanceof ServerLevel) {
            switch (species.combatStyle()) {
                case RANGED -> this.goalSelector.addGoal(3,
                        this.gated(new CompanionRangedAttackGoal(this, 1.1), () -> true));
                case MELEE -> this.goalSelector.addGoal(3,
                        this.gated(new MeleeAttackGoal(this, 1.2, true), () -> true));
                case HYBRID -> {
                    this.goalSelector.addGoal(3,
                            this.gated(new CompanionRangedAttackGoal(this, 1.1), this::isRangedMode));
                    this.goalSelector.addGoal(4,
                            this.gated(new MeleeAttackGoal(this, 1.2, true), () -> !this.isRangedMode()));
                }
            }
        }
    }

    public static AttributeSupplier.Builder createCompanionAttributes(CompanionSpecies species) {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, species.baseHealth())
                .add(Attributes.MOVEMENT_SPEED, species.moveSpeed())
                .add(Attributes.ATTACK_DAMAGE, species.attackDamage())
                .add(Attributes.ARMOR, species.armour())
                .add(Attributes.KNOCKBACK_RESISTANCE, species.knockbackResistance())
                .add(Attributes.FOLLOW_RANGE, species.followRange());
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

            @Override
            public void start() {
                super.start();
                CompanionEntity.this.followingOwner = true;
            }

            @Override
            public void stop() {
                super.stop();
                CompanionEntity.this.followingOwner = false;
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
        entityData.define(DATA_SKIN, "");
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
        output.putString("Skin", this.getSkinName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_LEVEL, Math.max(CompanionLevels.MIN_LEVEL, input.getIntOr("Level", CompanionLevels.MIN_LEVEL)));
        this.entityData.set(DATA_EXPERIENCE, input.getIntOr("Experience", 0));
        this.entityData.set(DATA_BOND, input.getIntOr("Bond", 0));
        this.playedSpawnAnimation = input.getBooleanOr("PlayedSpawnAnimation", true);
        this.entityData.set(DATA_SKIN, input.getStringOr("Skin", ""));
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

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkinName(String name) {
        this.entityData.set(DATA_SKIN, name);
    }

    public List<CompanionAbility> unlockedAbilities() {
        return this.species.abilitiesAt(this.getLevel());
    }

    public boolean hasAbility(CompanionAbility ability) {
        return this.unlockedAbilities().contains(ability);
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
        this.tickHitboxOffset();

        if (this.level().isClientSide()) {
            return;
        }

        if (!this.playedSpawnAnimation && this.tickCount >= SPAWN_ANIMATION_TICK) {
            this.playedSpawnAnimation = true;
            this.playCompanionAnim(CompanionAnimations.SPAWN);
        }

        this.updateRunning();

        if (this.isAlive() && this.tickCount % LOCATION_UPDATE_INTERVAL == 0) {
            this.refreshBondState();
        }

        if (this.bowCooldown > 0) {
            this.bowCooldown--;
        }

        this.updateCombatMode();

        if (this.tickCount % EQUIPMENT_INTERVAL == 0) {
            this.refreshEquipment();
        }

        this.tickAbilityCooldowns();
        this.tickLeapImpact();
        this.tickShockwave();

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

    @Override
    public int getMaxHeadYRot() {
        if (this.species == null || this.species.headBones().isEmpty()) {
            return super.getMaxHeadYRot();
        }

        return (int) this.species.maxHeadYaw();
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        double offset = this.species == null ? 0.0 : this.species.hitboxOffset();

        if (offset == 0.0) {
            return super.makeBoundingBox(position);
        }

        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return super.makeBoundingBox(position.add(Mth.sin(yaw) * offset, 0.0, -Mth.cos(yaw) * offset));
    }

    private void tickHitboxOffset() {
        if (this.species.hitboxOffset() == 0.0) {
            return;
        }

        if (Float.isNaN(this.hitboxYaw)
                || Math.abs(Mth.wrapDegrees(this.yBodyRot - this.hitboxYaw)) >= HITBOX_YAW_STEP) {
            this.hitboxYaw = this.yBodyRot;
            this.setPos(this.getX(), this.getY(), this.getZ());
        }
    }

    private void updateRunning() {
        boolean running = this.shouldRun();

        if (running != this.isSprinting()) {
            this.setSprinting(running);
        }
    }

    private boolean shouldRun() {
        if (!this.species.hasAnimation(CompanionAnimations.RUN) || this.isInSittingPose()) {
            return false;
        }

        if (this.followingOwner) {
            return true;
        }

        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && this.distanceTo(target) > RUN_DISTANCE;
    }

    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);

        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }

        speed.removeModifier(VANILLA_SPRINT_MODIFIER);
        speed.removeModifier(RUN_SPEED_MODIFIER.id());

        if (sprinting) {
            speed.addTransientModifier(RUN_SPEED_MODIFIER);
        }
    }

    public boolean isRangedMode() {
        return this.rangedMode;
    }

    private void updateCombatMode() {
        if (this.species.combatStyle() != CompanionSpecies.CombatStyle.HYBRID) {
            return;
        }

        boolean previous = this.rangedMode;
        LivingEntity target = this.getTarget();

        if (target == null || target.getType().builtInRegistryHolder().is(KindredTags.DANGEROUS_PREY)) {
            this.rangedMode = true;
        } else {
            double distance = this.distanceTo(target);

            if (distance <= MELEE_SWITCH_RANGE) {
                this.rangedMode = false;
            } else if (distance >= BOW_SWITCH_RANGE) {
                this.rangedMode = true;
            }
        }

        if (previous != this.rangedMode) {
            this.refreshEquipment();
        }
    }

    private void refreshEquipment() {
        if (!this.species.carriesWeapons()) {
            return;
        }

        this.equip(EquipmentSlot.MAINHAND, this.rangedMode ? new ItemStack(Items.BOW) : this.meleeWeapon());
        this.equip(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private ItemStack meleeWeapon() {
        if (KindredConfig.COMMON.mimicOwnerWeapon.get() && this.getOwner() instanceof Player owner
                && isMeleeWeapon(owner.getMainHandItem())) {
            return owner.getMainHandItem().copy();
        }

        return new ItemStack(Items.IRON_SWORD);
    }

    private void equip(EquipmentSlot slot, ItemStack stack) {
        if (!ItemStack.matches(this.getItemBySlot(slot), stack)) {
            this.setItemSlot(slot, stack);
        }

        this.setDropChance(slot, 0.0f);
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) && entry.slot().test(EquipmentSlot.MAINHAND)) {
                return true;
            }
        }

        return false;
    }

    public void faceInstantly(Entity target) {
        float yaw = (float) (Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX())
                * Mth.RAD_TO_DEG) - 90.0f;

        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
    }

    public void tryRangedAttack(LivingEntity target) {
        if (this.bowCooldown > 0 || !this.species.carriesWeapons() || !this.isBonded()) {
            return;
        }

        if (!(this.getMainHandItem().getItem() instanceof BowItem)) {
            this.refreshEquipment();
        }

        this.bowCooldown = BOW_INTERVAL;
        this.playCompanionAnim(CompanionAnimations.SHOOT);
        this.performRangedAttack(target,
                (float) (CompanionLevels.attackDamageAt(this.species, this.getLevel()) / BOW_POWER_DIVISOR));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        if (!(bow.getItem() instanceof BowItem)) {
            bow = new ItemStack(Items.BOW);
        }

        ItemStack ammo = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, power, bow);

        double x = target.getX() - this.getX();
        double y = target.getY(0.333) - arrow.getY();
        double z = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(x * x + z * z);

        Projectile.spawnProjectileUsingShoot(arrow, level, ammo, x, y + horizontal * 0.2, z, 1.6f, 6.0f);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    public boolean isAbilityReady(Identifier ability) {
        return this.abilityCooldowns.getInt(ability) <= 0;
    }

    public void setAbilityCooldown(Identifier ability, int ticks) {
        this.abilityCooldowns.put(ability, ticks);
    }

    private void tickAbilityCooldowns() {
        this.abilityCooldowns.replaceAll((ability, remaining) -> Math.max(0, remaining - 1));
    }

    public void scheduleLeapImpact(float damage, double radius, double knockback) {
        this.pendingImpact = new LeapImpact(damage, radius, knockback, LEAP_IMPACT_TIMEOUT);
    }

    private void tickLeapImpact() {
        LeapImpact impact = this.pendingImpact;
        if (impact == null) {
            return;
        }

        if (!this.onGround()) {
            impact.airborne = true;
        } else if (impact.airborne) {
            this.resolveLeapImpact(impact);
            return;
        }

        if (--impact.remaining <= 0) {
            this.resolveLeapImpact(impact);
        }
    }

    private void resolveLeapImpact(LeapImpact impact) {
        this.pendingImpact = null;

        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = this.getTarget();
        DamageSource source = this.damageSources().mobAttack(this);

        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(impact.radius),
                entity -> entity.isAlive() && entity != this
                        && (entity == target
                        || (entity instanceof Enemy && !(entity instanceof CompanionEntity))))) {
            victim.hurtServer(level, source, victim == target ? impact.damage : impact.damage * 0.5f);
            victim.knockback(impact.knockback, this.getX() - victim.getX(), this.getZ() - victim.getZ());
        }

        this.shockwaveOrigin = this.position();
        this.shockwaveTicks = SHOCKWAVE_TICKS;

        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground),
                    this.getX(), this.getY(), this.getZ(), 24, 0.5, 0.1, 0.5, 0.2);
        }

        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.6f);
    }

    private void tickShockwave() {
        if (this.shockwaveTicks <= 0 || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        int elapsed = SHOCKWAVE_TICKS - this.shockwaveTicks;
        this.shockwaveTicks--;

        double radius = SHOCKWAVE_MAX_RADIUS * (elapsed + 1) / SHOCKWAVE_TICKS;
        double lift = 0.35 * (1.0 - (double) elapsed / SHOCKWAVE_TICKS);
        int points = Math.max(10, (int) (radius * 10));

        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            double x = this.shockwaveOrigin.x + Math.cos(angle) * radius;
            double z = this.shockwaveOrigin.z + Math.sin(angle) * radius;

            BlockState state = level.getBlockState(BlockPos.containing(x, this.shockwaveOrigin.y - 0.2, z));
            if (state.isAir()) {
                continue;
            }

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, this.shockwaveOrigin.y + 0.1, z, 2, 0.04, 0.02, 0.04, lift);
        }
    }

    private static final class LeapImpact {
        private final float damage;
        private final double radius;
        private final double knockback;
        private int remaining;
        private boolean airborne;

        private LeapImpact(float damage, double radius, double knockback, int remaining) {
            this.damage = damage;
            this.radius = radius;
            this.knockback = knockback;
            this.remaining = remaining;
        }
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
        addTriggerable(action, CompanionAnimations.JUMP_ATTACK, JUMP_ATTACK);
        addTriggerable(action, CompanionAnimations.SHOOT, SHOOT);
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

        AnimationController<CompanionEntity> action =
                test.manager().getAnimationControllers().get(ACTION_CONTROLLER);
        if (action != null && action.isPlayingTriggeredAnimation() && !action.hasAnimationFinished()) {
            return PlayState.STOP;
        }

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
