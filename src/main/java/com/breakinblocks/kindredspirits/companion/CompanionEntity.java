package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CompanionEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPERIENCE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_COMMAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");

    private final CompanionSpecies species;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level, CompanionSpecies species) {
        super(type, level);
        this.species = species;
        this.setTame(false, false);
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
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_LEVEL, CompanionLevels.MIN_LEVEL);
        entityData.define(DATA_EXPERIENCE, 0);
        entityData.define(DATA_BOND, 0);
        entityData.define(DATA_COMMAND, (byte) CompanionCommand.FOLLOW.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Level", this.getLevel());
        output.putInt("Experience", this.getExperience());
        output.putInt("Bond", this.getBond());
        output.putString("Command", this.getCommand().getSerializedName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_LEVEL, Math.max(CompanionLevels.MIN_LEVEL, input.getIntOr("Level", CompanionLevels.MIN_LEVEL)));
        this.entityData.set(DATA_EXPERIENCE, input.getIntOr("Experience", 0));
        this.entityData.set(DATA_BOND, input.getIntOr("Bond", 0));

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
        if (this.level().isClientSide() || amount <= 0) {
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

        if (this.level().isClientSide() || !this.isTame() || !KindredConfig.COMMON.abilitiesEnabled.get()) {
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CompanionEntity>("main", 5, this::animateMain));
    }

    private PlayState animateMain(AnimationTest<CompanionEntity> test) {
        if (test.animatable().isInSittingPose()) {
            return test.setAndContinue(SIT);
        }
        return test.setAndContinue(test.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
