package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.registry.KindredParticles;
import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.item.KindredEquipmentItem;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.breakinblocks.kindredspirits.menu.KindredStorageMenu;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.breakinblocks.kindredspirits.registry.KindredTags;
import com.breakinblocks.kindredspirits.util.BlockPosUtil;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.breakinblocks.kindredspirits.registry.KindredSounds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.level.portal.TeleportTransition;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class CompanionEntity extends TamableAnimal implements GeoEntity, RangedAttackMob {
    private long quokkaDecoyUntil;
    private long quokkaFleeUntil;
    private static final EntityDataAccessor<Boolean> DATA_QUOKKA_FLEEING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean isQuokkaDecoy() { return this.quokkaDecoyUntil > 0; }
    public boolean isQuokkaFleeing() {
        if (this.level().isClientSide()) return this.entityData.get(DATA_QUOKKA_FLEEING);
        return this.species == CompanionSpecies.QUOKKA && !this.isTame()
                && this.quokkaFleeUntil > this.level().getGameTime();
    }

    public void makeQuokkaDecoy(int lifetimeTicks) {
        this.quokkaDecoyUntil = this.level().getGameTime() + Math.max(1, lifetimeTicks);
        this.setBaby(true);
    }

    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPERIENCE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOND_LEVEL =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STARS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_COMMAND =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_AGGRESSION =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_BONDED =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_DISABLED_ABILITIES =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_EQUIPMENT_ID =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> DATA_LEVEL_LIMIT = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAR_LIMIT = SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);

    public static final String MAIN_CONTROLLER = "main";
    public static final String ACTION_CONTROLLER = "action";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(CompanionAnimations.IDLE);
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop(CompanionAnimations.WALK);
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop(CompanionAnimations.RUN);
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop(CompanionAnimations.FLY);
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop(CompanionAnimations.SIT);
    private static final RawAnimation SIT_STILL = RawAnimation.begin().thenLoop(CompanionAnimations.SIT_STILL);
    private static final RawAnimation SIT_RARE = RawAnimation.begin().thenLoop(CompanionAnimations.SIT_RARE);
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.ATTACK);
    private static final RawAnimation SPECIAL_ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.SPECIAL_ATTACK);
    private static final RawAnimation JUMP_ATTACK = RawAnimation.begin().thenPlay(CompanionAnimations.JUMP_ATTACK);
    private static final RawAnimation DIG = RawAnimation.begin().thenPlayXTimes(CompanionAnimations.DIG, 4);
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
    private static final int PROGRESS_INTERVAL = 20;
    private static final int FEED_HEARTS = 7;
    private static final int FOOD_BOND_POINTS = 1;
    private static final int BOW_INTERVAL = 35;
    private static final double MELEE_SWITCH_RANGE = 3.0;
    private static final double BOW_SWITCH_RANGE = 6.0;
    private static final double BOW_POWER_DIVISOR = 4.0;
    private static final double FACE_TARGET_RANGE_SQR = 16.0;
    private static final float FACE_TARGET_STEP = 25.0f;

    public static final double RUN_DISTANCE = 10.0;
    private static final int SIT_VARIANT_HOLD = 60;
    private static final int SIT_RARE_HOLD = 31;
    private static final float SIT_RARE_CHANCE = 0.05f;
    private static final float SIT_RESTLESS_CHANCE = 0.25f;
    private static final float HITBOX_YAW_STEP = 5.0f;
    private static final Identifier VANILLA_SPRINT_MODIFIER = Identifier.withDefaultNamespace("sprinting");
    private static final AttributeModifier RUN_SPEED_MODIFIER = new AttributeModifier(
            KindredSpirits.id("running"),
            0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final double SHOCKWAVE_MAX_RADIUS = 4.0;
    private static final int METEOR_TIMEOUT = 100;
    private static final int METEOR_HEIGHT = 12;
    private static final float METEOR_DAMAGE = 8.0f;
    private static final double METEOR_RADIUS = 2.0;
    private static final int METEOR_FIRE_TICKS = 20;
    private static final int TABLET_WANDER_INTERVAL = 2400;
    private static final int TABLET_WANDER_TICKS = 600;
    private static final float TABLET_WANDER_CHANCE = 0.25f;
    private static final Identifier METEOR_FIRE = KindredSpirits.id("meteor_fire");
    private static final int BATTERY_INTERVAL = 20;
    private static final int BATTERY_RADIUS = 4;
    private static final int BATTERY_EXTRA_TICKS = 8;
    private static final float BATTERY_CHANCE = 0.1f;
    private static final double BATTERY_SHOCK_RADIUS = 3.0;
    private static final float BATTERY_SHOCK_DAMAGE = 2.0f;
    private static final int LEVEL_CHIME_DELAY = 17;
    private static final int DIG_TICKS = 40;
    private static final int DIG_EFFECT_INTERVAL = 5;
    private static final double DIG_DRIFT_SQR = 2.25;
    public static final ResourceKey<LootTable> DIG_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, KindredSpirits.id("gameplay/direwolf_dig"));

    private final CompanionSpecies species;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<Identifier, Long> abilityCooldowns = new HashMap<>();
    private boolean abilitiesWereEnabled = true;
    private @Nullable LeapImpact pendingImpact;
    private int shockwaveTicks;
    private Vec3 shockwaveOrigin = Vec3.ZERO;
    private boolean playedSpawnAnimation;
    private boolean rangedMode = true;
    private boolean followingOwner;
    private float hitboxYaw = Float.NaN;
    private RawAnimation sitVariant = SIT;
    private long sitVariantUntil;
    private static final Identifier BOW_COOLDOWN = KindredSpirits.id("bow_attack");
    private List<CompanionAbility> cachedAbilities = List.of();
    private CompanionProgress progress = new CompanionProgress();
    private CompanionLights lights = new CompanionLights();
    private int cachedAbilityLevel = -1;
    private int cachedAbilityBond = -1;
    private ItemStack equipment = ItemStack.EMPTY;
    private final List<Meteor> meteors = new ArrayList<>();
    private @Nullable CompanionCommand tabletWanderRestore;
    private long tabletWanderUntil;
    private @Nullable BlockPos digPos;
    private int digTicks;
    private int levelChimeTicks;

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
                .add(Attributes.FOLLOW_RANGE, species.followRange())
                .add(Attributes.ENTITY_INTERACTION_RANGE, 0.0);
    }

    public CompanionSpecies species() {
        return this.species;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new QuokkaFleeGoal(this));
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
        if (!isHostile(target)) {
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
                return CompanionEntity.this.isAlive() && CompanionEntity.this.isBonded()
                        && CompanionAbilities.enabled() && allowed.getAsBoolean() && goal.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return CompanionEntity.this.isAlive() && CompanionEntity.this.isBonded()
                        && CompanionAbilities.enabled() && allowed.getAsBoolean() && goal.canContinueToUse();
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
        entityData.define(DATA_QUOKKA_FLEEING, false);
        entityData.define(DATA_LEVEL, CompanionLevels.MIN_LEVEL);
        entityData.define(DATA_LEVEL_LIMIT, CompanionLevels.maxLevel());
        entityData.define(DATA_STAR_LIMIT, CompanionLevels.maxStars());
        entityData.define(DATA_EXPERIENCE, 0);
        entityData.define(DATA_BOND, 0);
        entityData.define(DATA_BOND_LEVEL, 0);
        entityData.define(DATA_STARS, 0);
        entityData.define(DATA_COMMAND, (byte) CompanionCommand.FOLLOW.ordinal());
        entityData.define(DATA_AGGRESSION, (byte) CompanionAggression.NEUTRAL.ordinal());
        entityData.define(DATA_BONDED, false);
        entityData.define(DATA_SKIN, "");
        entityData.define(DATA_EQUIPMENT_ID, "");
        entityData.define(DATA_DISABLED_ABILITIES, "");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("QuokkaDecoyUntil", this.quokkaDecoyUntil);
        output.putLong("QuokkaFleeUntil", this.quokkaFleeUntil);
        output.putInt("Level", this.getLevel());
        output.putInt("Experience", this.getExperience());
        output.putInt("BondPoints", this.getBondPoints());
        output.putInt("Stars", this.getStars());
        output.store("Progress", CompanionProgress.CODEC, this.progress);
        if (!this.lights.isEmpty()) {
            output.store("Lights", CompanionLights.CODEC, this.lights);
        }
        output.putString("Command", this.commandForStorage().getSerializedName());
        output.store("CompanionState", CompanionState.CODEC, this.savedState());
        output.putBoolean("Bonded", this.isBonded());
        output.putString("Aggression", this.getAggression().getSerializedName());
        output.putBoolean("PlayedSpawnAnimation", this.playedSpawnAnimation);
        output.putString("Skin", this.getSkinName());
        if (!this.disabledAbilityNames().isEmpty()) {
            output.store("DisabledAbilities", Codec.STRING.listOf(), this.disabledAbilityNames());
        }
        if (!this.equipment.isEmpty()) {
            output.store("Equipment", ItemStack.CODEC, this.equipment);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.quokkaDecoyUntil = input.getLongOr("QuokkaDecoyUntil", 0L);
        this.quokkaFleeUntil = input.getLongOr("QuokkaFleeUntil", 0L);
        this.entityData.set(DATA_LEVEL, CompanionLevels.clampLevel(input.getIntOr("Level", CompanionLevels.MIN_LEVEL)));
        this.entityData.set(DATA_EXPERIENCE, input.getIntOr("Experience", 0));
        this.setBondPoints(input.read("BondPoints", Codec.INT)
                .orElseGet(() -> CompanionBondMath.migrateLegacy(input.getIntOr("Bond", 0))));
        this.entityData.set(DATA_STARS, CompanionLevels.clampStars(input.getIntOr("Stars", 0)));
        this.progress = input.read("Progress", CompanionProgress.CODEC).orElseGet(CompanionProgress::new);
        this.entityData.set(DATA_BONDED, input.getBooleanOr("Bonded", false));
        this.abilityCooldowns.clear();
        this.abilityCooldowns.putAll(input.read("CompanionState", CompanionState.CODEC)
                .orElse(CompanionState.LEGACY).cooldowns());
        this.lights = input.read("Lights", CompanionLights.CODEC).orElseGet(CompanionLights::new);
        this.playedSpawnAnimation = input.getBooleanOr("PlayedSpawnAnimation", true);
        this.entityData.set(DATA_SKIN, input.getStringOr("Skin", ""));
        this.setDisabledAbilityNames(input.read("DisabledAbilities", Codec.STRING.listOf()).orElse(List.of()));
        this.equipment = input.read("Equipment", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.entityData.set(DATA_AGGRESSION, (byte) CompanionAggression
                .byName(input.getStringOr("Aggression", CompanionAggression.NEUTRAL.getSerializedName())).ordinal());

        this.entityData.set(DATA_COMMAND, (byte) CompanionCommand
                .byName(input.getStringOr("Command", CompanionCommand.FOLLOW.getSerializedName())).ordinal());

        this.applyLevelScaling(false);
    }

    public int getLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public int configuredMaxLevel() {
        return this.entityData.get(DATA_LEVEL_LIMIT);
    }

    public int configuredMaxStars() {
        return this.entityData.get(DATA_STAR_LIMIT);
    }

    public int getExperience() {
        return this.entityData.get(DATA_EXPERIENCE);
    }

    public int getBondPoints() {
        return this.entityData.get(DATA_BOND);
    }

    public int getBondLevel() {
        return this.entityData.get(DATA_BOND_LEVEL);
    }

    public int getStars() {
        return this.entityData.get(DATA_STARS);
    }

    public CompanionProgress progress() {
        return this.progress;
    }

    public void setBondPoints(int points) {
        int clamped = CompanionBondMath.clampPoints(points);
        this.entityData.set(DATA_BOND, clamped);
        this.entityData.set(DATA_BOND_LEVEL, CompanionBondMath.levelFromPoints(clamped));
    }

    public void addBondPoints(int amount) {
        int before = this.getBondLevel();
        this.setBondPoints((int) Math.clamp((long) this.getBondPoints() + amount, 0L, Integer.MAX_VALUE));
        int after = this.getBondLevel();

        if (after > before && !this.level().isClientSide()) {
            this.announce("bond_up", after);
            this.burst(KindredParticles.BOND_HEART.get(), FEED_HEARTS);
        }
    }

    public double bondRateMultiplier() {
        return CompanionBondMath.rateMultiplier(this.getBondLevel());
    }

    public int bondTier() {
        return CompanionBondMath.tier(this.getBondLevel());
    }

    public int scaledInterval(CompanionAbility ability) {
        if (!ability.scalesWithBond()) {
            return ability.intervalTicks();
        }
        return (int) Math.max(1L, Math.round(ability.intervalTicks() * this.bondRateMultiplier()));
    }

    public double scaledChance(double chance) {
        return Math.min(1.0, chance / this.bondRateMultiplier());
    }

    public CompanionLights lights() {
        return this.lights;
    }

    public void burst(ParticleOptions particle, int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    this.getX(), this.getY() + this.getBbHeight(), this.getZ(), count, 0.4, 0.4, 0.4, 0.02);
        }
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
            if (this.level() instanceof ServerLevel level) this.lights.clear(level);
            this.setTarget(null);
        }
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkinName(String name) {
        this.entityData.set(DATA_SKIN, name);
    }

    public void openStorage(ServerPlayer player) {
        int accessible = CompanionLevels.storageSlots(this.species, this.getBondLevel());
        SimpleContainer storage = KindredAttachments.storageContainer(player);

        int highestFilled = -1;
        for (int i = 0; i < storage.getContainerSize(); i++) {
            if (!storage.getItem(i).isEmpty()) {
                highestFilled = i;
            }
        }

        int rows = Math.clamp(Math.max(Mth.positiveCeilDiv(accessible, 9),
                Mth.positiveCeilDiv(highestFilled + 1, 9)), 1, CompanionLevels.MAX_STORAGE_ROWS);

        player.openMenu(new SimpleMenuProvider(
                        (id, playerInventory, unused) ->
                                new KindredStorageMenu(id, playerInventory, storage, rows, accessible, this),
                        this.getDisplayName()),
                buffer -> {
                    buffer.writeVarInt(rows);
                    buffer.writeVarInt(accessible);
                });
    }

    public List<CompanionAbility> unlockedAbilities() {
        int level = this.getLevel();
        int bondLevel = this.getBondLevel();

        if (level != this.cachedAbilityLevel || bondLevel != this.cachedAbilityBond) {
            List<CompanionAbility> previous = this.cachedAbilities;
            boolean announce = this.cachedAbilityLevel >= 0;
            this.cachedAbilities = this.species.abilitiesAt(level, bondLevel);
            this.cachedAbilityLevel = level;
            this.cachedAbilityBond = bondLevel;
            this.applyAbilityBonuses();

            if (announce && !this.level().isClientSide() && this.isBonded()) {
                for (CompanionAbility ability : this.cachedAbilities) {
                    if (!previous.contains(ability)) {
                        this.announce("ability_unlocked", ability.displayName());
                    }
                }
            }
        }

        return this.cachedAbilities;
    }

    public List<CompanionAbility> activeAbilities() {
        return this.unlockedAbilities().stream().filter(ability -> !this.isAbilityDisabled(ability)).toList();
    }

    public boolean hasAbility(CompanionAbility ability) {
        return this.unlockedAbilities().contains(ability) && !this.isAbilityDisabled(ability);
    }

    public List<String> disabledAbilityNames() {
        String joined = this.entityData.get(DATA_DISABLED_ABILITIES);
        return joined.isEmpty() ? List.of() : List.of(joined.split(","));
    }

    public boolean isAbilityDisabled(CompanionAbility ability) {
        return this.disabledAbilityNames().contains(ability.id().getPath());
    }

    public void setDisabledAbilityNames(List<String> names) {
        this.entityData.set(DATA_DISABLED_ABILITIES, String.join(",", names));
        if (this.cachedAbilityLevel >= 0) {
            this.applyAbilityBonuses();
        }
    }

    public void setAbilityEnabled(CompanionAbility ability, boolean enabled) {
        List<String> names = new ArrayList<>(this.disabledAbilityNames());
        String name = ability.id().getPath();
        if (enabled) {
            names.remove(name);
        } else if (!names.contains(name)) {
            names.add(name);
            if (this.level() instanceof ServerLevel serverLevel) {
                this.lights.clear(serverLevel, ability.id());
            }
        }
        this.setDisabledAbilityNames(names);
    }

    private void applyAbilityBonuses() {
        this.applyBonuses(
                CompanionAbilities.all().values().stream().flatMap(ability -> ability.attributeBonuses().stream()).toList(),
                CompanionAbilities.enabled() ? this.activeAbilities().stream().flatMap(ability -> ability.attributeBonuses().stream()).toList() : List.of(),
                false);
    }

    private void applyEquipmentBonuses() {
        this.applyBonuses(
                KindredItems.equipment().stream().flatMap(item -> item.get().bonuses().stream()).toList(),
                this.equipment.getItem() instanceof KindredEquipmentItem item ? item.bonuses() : List.of(),
                false);
    }

    private void applyBonuses(Collection<CompanionLevels.AttributeBonus> all,
                              Collection<CompanionLevels.AttributeBonus> active, boolean permanent) {
        CompanionLevels.applyBonuses(this.getAttributes(), all, active, permanent);
    }

    public ItemStack equipment() {
        return this.equipment;
    }

    public boolean hasEquipment(Item item) {
        if (this.level().isClientSide()) {
            return this.equipmentId().equals(equipmentId(new ItemStack(item)));
        }
        return this.equipment.is(item);
    }

    public String equipmentId() {
        return this.entityData.get(DATA_EQUIPMENT_ID);
    }

    public static String equipmentId(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public void showEquipment(ItemStack stack) {
        this.entityData.set(DATA_EQUIPMENT_ID, equipmentId(stack));
    }

    public void setEquipment(ItemStack stack) {
        this.equipment = stack;
        this.showEquipment(stack);
        this.applyEquipmentBonuses();
        this.refreshEquipment();
        if (!this.hasEquipment(KindredItems.DRAGON_TABLET.get())) {
            this.endTabletWander();
        }
    }

    private void tryEquip(Player player, ItemStack stack) {
        ItemStack previous = this.equipment;
        this.setEquipment(stack.copyWithCount(1));
        this.consumeWithFeedback(player, stack);

        if (!previous.isEmpty()) {
            player.getInventory().placeItemBackInInventory(previous);
        }

        player.sendSystemMessage(Component.translatable("message.kindredspirits.equipped",
                this.getDisplayName(), this.equipment.getHoverName()));
    }

    public ItemStack takeEquipment() {
        ItemStack taken = this.equipment;
        this.setEquipment(ItemStack.EMPTY);
        return taken;
    }

    public void dropEquipment() {
        ItemStack dropped = this.takeEquipment();
        if (!dropped.isEmpty() && this.level() instanceof ServerLevel level) {
            this.spawnAtLocation(level, dropped);
        }
    }

    @Override
    protected AABB getAttackBoundingBox(double horizontalExpansion) {
        return super.getAttackBoundingBox(horizontalExpansion + this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return super.isInvulnerableTo(level, source) || this.species.immunities().blocks(source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return super.canBeAffected(effect) && !this.species.immunities().blocks(effect.getEffect());
    }

    public Optional<CompanionAbility> activeAbility() {
        return this.activeAbilities().stream().filter(CompanionAbility::isActive).findFirst();
    }

    public int abilityCooldownTicks(Identifier ability) {
        return (int) Math.clamp(this.abilityCooldowns.getOrDefault(ability, 0L) - this.level().getGameTime(), 0L, Integer.MAX_VALUE);
    }

    public boolean useActiveAbility(ServerPlayer owner) {
        if (!this.isAlive() || !this.isBonded() || !this.isOwnedBy(owner) || !CompanionAbilities.enabled()) {
            return false;
        }
        return this.activeAbility().map(ability -> ability.activate(this, owner)).orElse(false);
    }

    public int experienceToNextLevel() {
        return CompanionLevels.experienceToNext(this.getLevel());
    }

    public void gainExperience(int amount) {
        if (this.level().isClientSide() || amount <= 0 || !this.isBonded()) {
            return;
        }

        int scaled = (int) Math.floor(amount * CompanionBondMath.experienceMultiplier(this.getBondLevel()));
        this.addExperience(this.progress.applyGain(scaled, this.level().getGameTime(), this.ownerChunk()));
    }

    private @Nullable ChunkPos ownerChunk() {
        ServerPlayer owner = this.serverOwner();
        return owner != null && owner.level() == this.level() ? owner.chunkPosition() : null;
    }

    public @Nullable ServerPlayer serverOwner() {
        return this.getOwner() instanceof ServerPlayer owner ? owner : null;
    }

    public boolean ownerWithin(@Nullable ServerPlayer owner, double rangeSqr) {
        return owner != null && owner.level() == this.level() && this.distanceToSqr(owner) <= rangeSqr;
    }

    public void announce(String key, Object... args) {
        ServerPlayer owner = this.serverOwner();
        if (owner == null) {
            return;
        }

        Object[] withName = new Object[args.length + 1];
        withName[0] = this.getDisplayName();
        System.arraycopy(args, 0, withName, 1, args.length);
        owner.sendSystemMessage(Component.translatable("message.kindredspirits." + key, withName));
    }

    private void celebrate(SoundEvent sound, float volume, float pitch, ParticleOptions particle, int count) {
        this.applyLevelScaling(true);
        this.playSound(sound, volume, pitch);
        this.burst(particle, count);
    }

    public boolean prestige() {
        if (this.level().isClientSide() || !CompanionLevels.canPrestige(this.getLevel(), this.getStars())) {
            return false;
        }

        int stars = this.getStars() + 1;
        this.entityData.set(DATA_STARS, stars);
        this.entityData.set(DATA_LEVEL, CompanionLevels.MIN_LEVEL);
        this.entityData.set(DATA_EXPERIENCE, 0);
        this.celebrate(KindredSounds.PRESTIGE.get(), 0.6f, 1.0f, KindredParticles.LEVEL_STAR.get(), 24);

        ServerPlayer owner = this.serverOwner();
        if (owner != null) {
            KindredAttachments.modify(owner, record -> record.withHighestStars(stars));
            this.announce("prestige", stars);
        }

        return true;
    }

    public void addExperience(int amount) {
        if (this.level().isClientSide() || amount <= 0 || !this.isBonded()) {
            return;
        }

        int level = this.getLevel();
        int experience = (int) Math.min(Integer.MAX_VALUE, (long) this.getExperience() + amount);

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
            this.celebrate(KindredSounds.LEVEL_UP.get(), 0.7f, 1.0f, KindredParticles.LEVEL_STAR.get(), 12);
            this.levelChimeTicks = LEVEL_CHIME_DELAY;

            ServerPlayer owner = this.serverOwner();
            if (owner != null) {
                int reached = level;
                KindredAttachments.modify(owner, record -> record.withHighestLevel(reached));
                this.announce("level_up", level);
            }
        }
    }

    public void refreshBondState() {
        if (!(this.getOwner() instanceof Player owner)) {
            return; // An offline owner is not a released bond.
        }

        CompanionBond bond = KindredAttachments.bond(owner);
        boolean bonded = !bond.stored() && bond.isBoundTo(this.getUUID());
        this.setBonded(bonded);

        if (bonded) {
            KindredAttachments.modifyBond(owner, current -> KindredCharmItem.snapshot(current, this).withStored(false));
        }
    }

    public CompanionCommand commandForStorage() {
        return this.tabletWanderRestore != null && this.getCommand() == CompanionCommand.WANDER
                ? this.tabletWanderRestore : this.getCommand();
    }

    public CompanionState savedState() {
        return new CompanionState(this.getHealth(), this.abilityCooldowns);
    }

    public void restoreProgress(CompanionSnapshot snapshot, int experience) {
        this.entityData.set(DATA_LEVEL, CompanionLevels.clampLevel(snapshot.level()));
        this.entityData.set(DATA_EXPERIENCE, Math.max(0, experience));
        this.entityData.set(DATA_STARS, CompanionLevels.clampStars(snapshot.stars()));
        this.progress = snapshot.progress().copy();
        this.progress.creditStoredRest(this.level().getGameTime());
        this.setBondPoints(snapshot.bondPoints());
        this.setEquipment(snapshot.equipment().copy());
        this.setDisabledAbilityNames(snapshot.disabledAbilities());
        this.abilityCooldowns.clear();
        this.abilityCooldowns.putAll(snapshot.state().cooldowns());
        this.applyLevelScaling(false);
        float health = snapshot.state().health();
        this.setHealth(health < 0 || !Float.isFinite(health) ? this.getMaxHealth()
                : Math.clamp(health, 0.0f, this.getMaxHealth()));
    }

    public void applyLevelScaling(boolean healToFull) {
        int stars = this.getStars();

        CompanionLevels.applyBaseStats(this.getAttributes(), this.species, this.getLevel(), stars);
        this.applyBonuses(CompanionLevels.starBonuses(), CompanionLevels.starBonusesAt(stars), true);
        this.unlockedAbilities();
        this.applyAbilityBonuses();
        this.applyEquipmentBonuses();

        if (healToFull) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void tickProgress() {
        if (this.tickCount % PROGRESS_INTERVAL != 0 || !this.isAlive()) {
            return;
        }

        ServerPlayer owner = this.serverOwner();
        this.progress.tickSecond(this.level().getGameTime(), this.ownerChunk());

        if (!this.isBonded() || owner == null || owner.level() != this.level()
                || !this.ownerWithin(owner, CompanionBondMath.PASSIVE_RANGE * CompanionBondMath.PASSIVE_RANGE)
                || CompanionProgressEvents.isAfk(owner)) {
            return;
        }

        this.addBondPoints(CompanionBondMath.PASSIVE_POINTS);
    }

    public void onDeathProgress() {
        this.progress.startFeedCooldown(this.level().getGameTime());
        this.setBondPoints(CompanionBondMath.afterDeath(this.getBondPoints()));
    }

    private void playInteractFeedback() {
        this.playAction(CompanionAnimations.INTERACT, this.species.sounds().interact().get(), 0.8f, 1.0f);
    }

    private void consumeWithFeedback(Player player, ItemStack stack) {
        stack.consume(1, player);
        this.playInteractFeedback();
    }

    public void playAction(String animation, SoundEvent sound, float volume, float pitch) {
        this.playCompanionAnim(animation);
        this.playSound(sound, volume, pitch);
    }

    public void playSpecialAttack(float volume, float pitch) {
        this.playAction(CompanionAnimations.SPECIAL_ATTACK, this.species.sounds().specialAttack().get(), volume, pitch);
    }

    public float attackDamage() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public void magicHurt(ServerLevel level, LivingEntity target, float damage) {
        target.hurtServer(level, this.damageSources().indirectMagic(this, this), damage);
    }

    public static boolean isHostile(Entity entity) {
        return entity instanceof Enemy && !(entity instanceof CompanionEntity);
    }

    private List<LivingEntity> hostilesWithin(ServerLevel level, AABB box, @Nullable LivingEntity alwaysInclude) {
        return level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity != this && (entity == alwaysInclude || isHostile(entity)));
    }

    private void tryTame(Player player, ItemStack stack) {
        this.consumeWithFeedback(player, stack);

        if (this.random.nextDouble() < KindredConfig.COMMON.tamingChance.get()) {
            this.adopt(player);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    public void adopt(Player player) {
        this.tame(player);
        this.setCommand(CompanionCommand.FOLLOW);
        this.burst(KindredParticles.BOND_HEART.get(), FEED_HEARTS);
        this.level().broadcastEntityEvent(this, (byte) 7);
    }

    private boolean feedForBond(Player player, ItemStack stack) {
        long gameTime = this.level().getGameTime();
        this.playInteractFeedback();

        if (!this.progress.canFeed(gameTime)) {
            player.sendSystemMessage(Component.translatable("message.kindredspirits.bond_feed_wait",
                    this.getDisplayName(), this.progress.feedSecondsLeft(gameTime)));
            return false;
        }

        stack.consume(1, player);
        this.progress.startFeedCooldown(gameTime);
        this.addBondPoints(CompanionBondMath.feedPoints(this.getBondPoints()));
        this.burst(KindredParticles.BOND_HEART.get(), FEED_HEARTS);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickHitboxOffset();

        if (this.level().isClientSide()) {
            return;
        }

        if (!this.isAlive()) return;
        if (this.isQuokkaDecoy() && this.level().getGameTime() >= this.quokkaDecoyUntil) {
            this.discard();
            return;
        }
        this.showEquipment(this.equipment);
        this.entityData.set(DATA_QUOKKA_FLEEING, this.isQuokkaFleeing());
        QuokkaSupport.tick(this);

        boolean enabled = CompanionAbilities.enabled();
        if (enabled != this.abilitiesWereEnabled) {
            this.abilitiesWereEnabled = enabled;
            this.applyAbilityBonuses();
            if (!enabled) {
                this.setTarget(null);
                this.lights.clear((ServerLevel) this.level());
                this.pendingImpact = null;
                this.digPos = null;
                this.meteors.forEach(meteor -> meteor.fireball.discard());
                this.meteors.clear();
                this.endTabletWander();
            }
        }

        if (!this.playedSpawnAnimation && this.tickCount >= SPAWN_ANIMATION_TICK) {
            this.playedSpawnAnimation = true;
            this.playCompanionAnim(CompanionAnimations.SPAWN);
        }

        if (this.tickCount % PROGRESS_INTERVAL == 0) {
            this.entityData.set(DATA_LEVEL_LIMIT, CompanionLevels.maxLevel());
            this.entityData.set(DATA_STAR_LIMIT, CompanionLevels.maxStars());
        }
        this.updateRunning();
        if (this.species == CompanionSpecies.NIGHTFOX && this.isBonded() && this.getTarget() == null
                && enabled && this.tickCount % 30 == 0) this.burst(KindredParticles.SPIRIT_WISP.get(), 1);
        if (enabled && this.isBonded() && this.isSprinting() && this.onGround()
                && this.hasEquipment(KindredItems.RUNNING_SHOES.get()) && this.tickCount % 4 == 0
                && this.level() instanceof ServerLevel level) {
            BlockState ground = level.getBlockState(this.blockPosition().below());
            this.burst(KindredParticles.SWIFT_MOTE.get(), 1);
            if (!ground.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    this.getX(), this.getY() + 0.05, this.getZ(), 2, 0.15, 0.02, 0.15, 0.02);
        }

        if (this.isAlive() && this.tickCount % LOCATION_UPDATE_INTERVAL == 0) {
            this.refreshBondState();
        }

        this.updateCombatMode();
        this.faceTarget();

        if (this.tickCount % EQUIPMENT_INTERVAL == 0) {
            this.refreshEquipment();
        }

        this.tickAbilityCooldowns();
        this.tickLeapImpact();
        this.tickShockwave();
        this.tickMeteors();
        this.tickDig();
        this.tickTabletWander();
        this.tickBattery();
        this.tickProgress();
        this.tickLevelChime();

        if (!this.isBonded() || !CompanionAbilities.enabled()) {
            return;
        }

        ServerPlayer owner = this.serverOwner();
        for (CompanionAbility ability : this.activeAbilities()) {
            if (Math.floorMod(this.tickCount + this.getId(), this.scaledInterval(ability)) == 0) {
                ability.serverTick(this, owner);
            }
        }
    }

    public void cycleCommandBy(Player player) {
        CompanionCommand command = this.getCommand().next();
        this.setCommand(command);
        player.sendSystemMessage(Component.translatable("message.kindredspirits.command_set",
                this.getDisplayName(), command.displayName()));
    }

    public static Optional<CompanionEntity> bondedNear(Player player, double range) {
        return player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(range),
                        companion -> companion.isAlive() && companion.isBonded() && companion.isOwnedBy(player)
                                && KindredAttachments.bond(player).isBoundTo(companion.getUUID())
                                && companion.distanceToSqr(player) <= range * range).stream()
                .min(Comparator.comparingDouble(companion -> companion.distanceToSqr(player)));
    }

    public static Optional<CompanionEntity> bondedWithAbility(Player player, double range, CompanionAbility ability) {
        if (!CompanionAbilities.enabled()) {
            return Optional.empty();
        }
        return bondedNear(player, range).filter(companion -> companion.isAlive() && companion.hasAbility(ability));
    }

    private InteractionResult serverOnly(Runnable action) {
        if (!this.level().isClientSide()) {
            action.run();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isQuokkaDecoy()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof KindredCharmItem) {
            return InteractionResult.PASS;
        }

        if (this.isTame() && this.isOwnedBy(player)) {
            if (player.isSecondaryUseActive()) {
                return this.serverOnly(() -> this.cycleCommandBy(player));
            }

            if (stack.is(KindredTags.COMPANION_HEALING) && this.getHealth() < this.getMaxHealth()) {
                return this.serverOnly(() -> {
                    this.consumeWithFeedback(player, stack);
                    this.heal((float) (double) KindredConfig.COMMON.healingPerItem.get());

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(KindredParticles.BOND_HEART.get(),
                                this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
                    }
                });
            }

            if (stack.is(this.species.equipmentTag())) {
                return this.serverOnly(() -> this.tryEquip(player, stack));
            }

            if (this.isBonded() && stack.is(this.species.tamingTag())) {
                return this.serverOnly(() -> this.feedForBond(player, stack));
            }

            if (this.isBonded() && stack.isEmpty()) {
                if (this.level().isClientSide()) {
                    return InteractionResult.SUCCESS;
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    this.openStorage(serverPlayer);
                }
                return InteractionResult.SUCCESS_SERVER;
            }

            if (this.isFood(stack)) {
                return this.serverOnly(() -> {
                    this.consumeWithFeedback(player, stack);
                    this.gainExperience(KindredConfig.COMMON.experiencePerFeed.get());
                    this.heal(2.0f);
                    if (this.isBonded()) {
                        this.addBondPoints(FOOD_BOND_POINTS);
                    }
                });
            }
        } else if (!this.isTame() && stack.is(this.species.tamingTag())) {
            return this.serverOnly(() -> this.tryTame(player, stack));
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(KindredTags.COMPANION_FOOD) || stack.is(this.species.tamingTag());
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

    public static @Nullable EquipmentSlot wornSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return null;
        }
        return switch (equippable.slot()) {
            case CHEST, BODY -> equippable.slot();
            default -> null;
        };
    }

    private void refreshEquipment() {
        EquipmentSlot worn = wornSlot(this.equipment);
        this.equip(EquipmentSlot.CHEST, worn == EquipmentSlot.CHEST ? this.equipment.copy() : ItemStack.EMPTY);
        this.equip(EquipmentSlot.BODY, worn == EquipmentSlot.BODY ? this.equipment.copy() : ItemStack.EMPTY);

        if (!this.species.carriesWeapons()) {
            return;
        }

        this.equip(EquipmentSlot.MAINHAND, !this.isBonded() ? ItemStack.EMPTY
                : this.rangedMode ? new ItemStack(Items.BOW) : this.meleeWeapon());
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
        this.turnBodyTo(yawTowards(this, target));
        this.setYHeadRot(this.getYRot());
    }

    private void faceTarget() {
        LivingEntity target = this.getTarget();

        if (target == null || !target.isAlive() || this.isInSittingPose()) {
            return;
        }

        if (this.distanceToSqr(target) > FACE_TARGET_RANGE_SQR && !this.getNavigation().isDone()) {
            return;
        }

        this.turnBodyTo(Mth.approachDegrees(this.getYRot(), yawTowards(this, target), FACE_TARGET_STEP));
    }

    private void turnBodyTo(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
    }

    private static float yawTowards(Entity from, Entity to) {
        return (float) (Mth.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public void tryRangedAttack(LivingEntity target) {
        if (!this.isAbilityReady(BOW_COOLDOWN) || !this.species.carriesWeapons() || !this.isBonded()
                || !this.isAlive() || !CompanionAbilities.enabled()) {
            return;
        }

        if (!(this.getMainHandItem().getItem() instanceof BowItem)) {
            this.refreshEquipment();
        }

        this.setAbilityCooldown(BOW_COOLDOWN, BOW_INTERVAL);
        this.playCompanionAnim(CompanionAnimations.SHOOT);
        this.performRangedAttack(target,
                (float) (CompanionLevels.attackDamageAt(this.species, this.getLevel(), this.getStars()) / BOW_POWER_DIVISOR));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!this.isAlive() || !this.isBonded() || !CompanionAbilities.enabled() || !(this.level() instanceof ServerLevel level)) {
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
        this.playSound(KindredSounds.MINI_PLAYER_BOW.get(), 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    public boolean isAbilityReady(Identifier ability) {
        return this.abilityCooldownTicks(ability) <= 0;
    }

    public void setAbilityCooldown(Identifier ability, int ticks) {
        this.abilityCooldowns.put(ability, this.level().getGameTime() + Math.max(0, ticks));
    }

    private void tickLevelChime() {
        if (this.levelChimeTicks > 0 && --this.levelChimeTicks == 0) {
            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        }
    }

    private void tickAbilityCooldowns() {
        this.abilityCooldowns.values().removeIf(deadline -> deadline <= this.level().getGameTime());
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

        for (LivingEntity victim : this.hostilesWithin(level, this.getBoundingBox().inflate(impact.radius), target)) {
            victim.hurtServer(level, source, victim == target ? impact.damage : impact.damage * 0.5f);
            victim.knockback(impact.knockback, this.getX() - victim.getX(), this.getZ() - victim.getZ());
        }

        this.startShockwave();
        this.burst(KindredParticles.EARTH_IMPACT.get(), 12);

        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground),
                    this.getX(), this.getY(), this.getZ(), 24, 0.5, 0.1, 0.5, 0.2);
        }

        this.playSound(KindredSounds.TREX_LEAP_IMPACT.get(), 0.5f, 1.0f);
    }

    public void callMeteor(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        LargeFireball fireball = new LargeFireball(level, this, new Vec3(0.0, -1.0, 0.0), 0);
        fireball.setPos(target.getX(), target.getY() + METEOR_HEIGHT, target.getZ());
        fireball.setDeltaMovement(0.0, -0.6, 0.0);
        level.addFreshEntity(fireball);
        this.meteors.add(new Meteor(fireball, METEOR_TIMEOUT));
    }

    private void tickMeteors() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        this.lights.sweep(level, METEOR_FIRE, this.position(), METEOR_FIRE_TICKS, Double.MAX_VALUE);

        for (Iterator<Meteor> iterator = this.meteors.iterator(); iterator.hasNext(); ) {
            Meteor meteor = iterator.next();
            Vec3 at = meteor.fireball.position();

            if (meteor.fireball.isRemoved()) {
                iterator.remove();
                this.meteorImpact(level, at);
            } else if (--meteor.remaining <= 0) {
                iterator.remove();
                meteor.fireball.discard();
            }
        }
    }

    private void meteorImpact(ServerLevel level, Vec3 at) {
        for (LivingEntity victim : this.hostilesWithin(level,
                AABB.ofSize(at, METEOR_RADIUS * 2, METEOR_RADIUS * 2, METEOR_RADIUS * 2), null)) {
            this.magicHurt(level, victim, METEOR_DAMAGE);
        }

        for (BlockPos pos : BlockPosUtil.cube(BlockPos.containing(at), 1)) {
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                this.lights.place(level, METEOR_FIRE, pos.immutable(), Blocks.FIRE.defaultBlockState(), 9);
            }
        }

        level.sendParticles(KindredParticles.METEOR_BURST.get(), at.x, at.y, at.z, 16, 0.6, 0.3, 0.6, 0.05);
        level.sendParticles(KindredParticles.FORGE_EMBER.get(), at.x, at.y, at.z, 24, 0.8, 0.4, 0.8, 0.05);
        level.playSound(null, at.x, at.y, at.z, KindredSounds.METEOR_IMPACT.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.7f, 1.0f);
    }

    private void tickTabletWander() {
        if (this.tabletWanderRestore != null) {
            if (this.level().getGameTime() >= this.tabletWanderUntil) {
                this.endTabletWander();
            }
            return;
        }

        if (!CompanionAbilities.enabled() || this.tickCount % TABLET_WANDER_INTERVAL != 0 || !this.isBonded()
                || !this.hasEquipment(KindredItems.DRAGON_TABLET.get())
                || this.getCommand() == CompanionCommand.WANDER
                || this.random.nextFloat() >= TABLET_WANDER_CHANCE) {
            return;
        }

        this.tabletWanderRestore = this.getCommand();
        this.tabletWanderUntil = this.level().getGameTime() + TABLET_WANDER_TICKS;
        this.setCommand(CompanionCommand.WANDER);
    }

    private void tickBattery() {
        if (!CompanionAbilities.enabled() || !this.isAlive() || this.tickCount % BATTERY_INTERVAL != 0 || !this.isBonded()
                || !this.hasEquipment(KindredItems.BATTERY.get())
                || !(this.level() instanceof ServerLevel level)
                || this.random.nextFloat() >= BATTERY_CHANCE) {
            return;
        }

        BlockPos accelerated = CompanionTinkering.accelerateNearby(level, this.blockPosition(),
                BATTERY_RADIUS, BATTERY_EXTRA_TICKS, this.random);
        if (accelerated == null) {
            return;
        }

        CompanionTinkering.sparks(level, accelerated);
        DamageSource shock = this.damageSources().source(DamageTypes.LIGHTNING_BOLT, this);

        for (LivingEntity hostile : this.hostilesWithin(level, this.getBoundingBox().inflate(BATTERY_SHOCK_RADIUS), null)) {
            hostile.hurtServer(level, shock, BATTERY_SHOCK_DAMAGE);
            CompanionAbilities.hitParticles(level, hostile, KindredParticles.GREMLIN_SPARK.get(), 8, 0.5, 0.3, 0.02);
        }
    }

    public boolean isIdle() {
        return this.digPos == null && this.getTarget() == null && this.onGround() && !this.isInWater()
                && !this.isInSittingPose() && !this.isSprinting() && this.getNavigation().isDone();
    }

    public boolean isDigging() {
        return this.digPos != null;
    }

    public void startDig(BlockPos ground) {
        this.digPos = ground.immutable();
        this.digTicks = DIG_TICKS;
        this.getNavigation().stop();
        this.playCompanionAnim(CompanionAnimations.DIG);
    }

    private void tickDig() {
        BlockPos ground = this.digPos;
        if (ground == null || !(this.level() instanceof ServerLevel level)) {
            return;
        }

        BlockState state = level.getBlockState(ground);
        if (!this.isBonded() || !CompanionAbilities.enabled() || !this.hasAbility(CompanionAbilities.NOT_ANOTHER_HOLE)
                || this.getTarget() != null || !this.onGround() || this.isInSittingPose()
                || !this.getNavigation().isDone() || !state.is(KindredTags.DIREWOLF_DIGGABLE)
                || this.distanceToSqr(Vec3.atBottomCenterOf(ground.above())) > DIG_DRIFT_SQR) {
            this.digPos = null;
            this.abilityCooldowns.remove(CompanionAbilities.NOT_ANOTHER_HOLE.id());
            return;
        }

        if (this.digTicks % DIG_EFFECT_INTERVAL == 0) {
            Vec3 paws = this.position().add(Vec3.directionFromRotation(0.0f, this.yBodyRot).scale(0.6 * this.species.renderScale()));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    paws.x, paws.y + 0.1, paws.z, 6, 0.15, 0.05, 0.15, 0.08);
            level.playSound(null, ground, state.getSoundType(level, ground, this).getHitSound(),
                    SoundSource.NEUTRAL, 0.6f, 0.7f);
        }

        if (--this.digTicks <= 0) {
            this.digPos = null;
            this.finishDig(level, ground);
        }
    }

    private void finishDig(ServerLevel level, BlockPos ground) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(DIG_LOOT);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(ground))
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT);

        for (ItemStack stack : table.getRandomItems(params)) {
            this.spawnAtLocation(level, stack);
        }

        this.burst(KindredParticles.EARTH_IMPACT.get(), 8);
        this.playSound(this.species.sounds().interact().get(), 0.8f, 1.1f);
    }

    private void endTabletWander() {
        if (this.tabletWanderRestore == null) {
            return;
        }

        CompanionCommand restore = this.tabletWanderRestore;
        this.tabletWanderRestore = null;
        if (this.getCommand() == CompanionCommand.WANDER) {
            this.setCommand(restore);
        }
    }

    public void startShockwave() {
        this.shockwaveOrigin = this.position();
        this.shockwaveTicks = SHOCKWAVE_TICKS;
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
            if (i % 3 == 0) level.sendParticles(KindredParticles.EARTH_IMPACT.get(),
                    x, this.shockwaveOrigin.y + 0.15, z, 1, 0.02, 0.02, 0.02, lift * 0.2);
        }
    }

    private static final class Meteor {
        private final LargeFireball fireball;
        private int remaining;

        private Meteor(LargeFireball fireball, int remaining) {
            this.fireball = fireball;
            this.remaining = remaining;
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
        if (!this.isAlive() || !this.isBonded() || !CompanionAbilities.enabled()
                || this.species.combatStyle() == CompanionSpecies.CombatStyle.RANGED) {
            return false;
        }

        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            this.playAction(CompanionAnimations.ATTACK, this.species.sounds().attack().get(), 0.9f, 1.0f);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.playCompanionAnim(CompanionAnimations.HURT);
            if (this.species == CompanionSpecies.QUOKKA && !this.isTame() && !this.isBaby()
                    && !this.isQuokkaDecoy() && !this.isQuokkaFleeing()
                    && source.getEntity() instanceof LivingEntity attacker) {
                this.quokkaFleeUntil = level.getGameTime() + 200;
                QuokkaSupport.throwBaby(this, attacker);
            }
        }
        return hurt;
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        if (this.level() instanceof ServerLevel level && transition.newLevel() != level) {
            this.lights.clear(level);
        }
        return super.teleport(transition);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        int duration = this.species == CompanionSpecies.TREX ? 58 : this.species == CompanionSpecies.GREMLIN ? 30 : 20;
        if (this.deathTime >= duration && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        if ((reason.shouldDestroy() || reason == RemovalReason.UNLOADED_TO_CHUNK) && this.level() instanceof ServerLevel serverLevel) {
            this.lights.clear(serverLevel);
        }
        super.onRemoval(reason);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            this.setTarget(null);
            this.pendingImpact = null;
            this.digPos = null;
            this.lights.clear((ServerLevel) this.level());
            this.meteors.forEach(meteor -> meteor.fireball.discard());
            this.meteors.clear();
            this.onDeathProgress();
            KindredCharmItem.onCompanionDied(this);
            if (!this.isBonded()) {
                this.dropEquipment();
            }
        }
        super.die(source);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.species.sounds().ambient().get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return this.species.sounds().hurt().get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return this.species.sounds().death().get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CompanionEntity> action =
                new AnimationController<CompanionEntity>(ACTION_CONTROLLER, 3, test -> PlayState.STOP);

        addTriggerable(action, CompanionAnimations.ATTACK, ATTACK);
        addTriggerable(action, CompanionAnimations.SPECIAL_ATTACK, SPECIAL_ATTACK);
        addTriggerable(action, CompanionAnimations.JUMP_ATTACK, JUMP_ATTACK);
        addTriggerable(action, CompanionAnimations.DIG, DIG);
        addTriggerable(action, CompanionAnimations.SHOOT, SHOOT);
        addTriggerable(action, CompanionAnimations.HURT, HURT);
        addTriggerable(action, CompanionAnimations.SPAWN, SPAWN);
        addTriggerable(action, CompanionAnimations.INTERACT, INTERACT);

        controllers.add(new AnimationController<CompanionEntity>(MAIN_CONTROLLER, 5, this::animateMain));
        controllers.add(action);
    }

    private void addTriggerable(AnimationController<CompanionEntity> controller, String name, RawAnimation animation) {
        if (this.species.hasAnimation(name)) {
            controller.triggerableAnim(name, animation);
        }
    }

    private PlayState animateMain(AnimationTest<CompanionEntity> test) {
        CompanionEntity companion = test.animatable();
        CompanionSpecies species = companion.species();

        if (companion.isDeadOrDying() && species.hasAnimation(CompanionAnimations.DEATH)) {
            return test.setAndContinue(DEATH);
        }

        AnimationController<CompanionEntity> action =
                test.manager().getAnimationControllers().get(ACTION_CONTROLLER);
        if (action != null && action.isPlayingTriggeredAnimation() && !action.hasAnimationFinished()) {
            return PlayState.STOP;
        }

        if (companion.isInSittingPose() && species.hasAnimation(CompanionAnimations.SIT)) {
            return test.setAndContinue(companion.sitAnimation());
        }

        companion.sitVariantUntil = 0L;

        if (!test.isMoving()) {
            return test.setAndContinue(IDLE);
        }
        if ((companion.isSprinting() || companion.isQuokkaFleeing()) && species.hasAnimation(CompanionAnimations.RUN)) {
            return test.setAndContinue(RUN);
        }
        if (species.hasAnimation(CompanionAnimations.WALK)) {
            return test.setAndContinue(WALK);
        }
        if (species.hasAnimation(CompanionAnimations.FLY)) {
            return test.setAndContinue(FLY);
        }
        return test.setAndContinue(IDLE);
    }

    private RawAnimation sitAnimation() {
        if (!this.species.hasAnimation(CompanionAnimations.SIT_STILL)) {
            return SIT;
        }

        long time = this.level().getGameTime();
        if (time < this.sitVariantUntil) {
            return this.sitVariant;
        }

        float roll = this.random.nextFloat();

        if (roll < SIT_RARE_CHANCE && this.species.hasAnimation(CompanionAnimations.SIT_RARE)) {
            this.sitVariant = SIT_RARE;
            this.sitVariantUntil = time + SIT_RARE_HOLD;
        } else if (roll < SIT_RESTLESS_CHANCE) {
            this.sitVariant = SIT;
            this.sitVariantUntil = time + SIT_VARIANT_HOLD;
        } else {
            this.sitVariant = SIT_STILL;
            this.sitVariantUntil = time + SIT_VARIANT_HOLD;
        }

        return this.sitVariant;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
