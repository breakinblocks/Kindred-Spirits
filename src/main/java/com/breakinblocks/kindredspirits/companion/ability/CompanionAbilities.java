package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.registry.KindredParticles;
import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAnimations;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionLevels.AttributeBonus;
import com.breakinblocks.kindredspirits.companion.CompanionLights;
import com.breakinblocks.kindredspirits.companion.CompanionTinkering;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.net.KindredNetworking;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredBlocks;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import com.breakinblocks.kindredspirits.util.BlockPosUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.breakinblocks.kindredspirits.registry.KindredSounds;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class CompanionAbilities {
    private static final Map<Identifier, CompanionAbility> REGISTRY = new LinkedHashMap<>();
    private static final double OWNER_RANGE_SQR = 144.0;
    private static final double OWNER_RANGE_LONG_SQR = 256.0;
    private static final double OWNER_RANGE_SHORT_SQR = 64.0;
    private static final double OWNER_RANGE_FIRE_SQR = 100.0;

    public static final CompanionAbility BRIGHTER_SIDE = register(new SimpleAbility(KindredSpirits.id("brighter_side"), 100,
            (companion, owner) -> {
                if (ownerEffect(companion, owner, OWNER_RANGE_LONG_SQR, MobEffects.LUCK, 120, 0))
                    companion.burst(KindredParticles.HAPPY_BLOOM.get(), 1);
            }));
    public static final CompanionAbility SMILE = register(new SimpleAbility(KindredSpirits.id("smile"), 20,
            com.breakinblocks.kindredspirits.companion.QuokkaSupport::smile));
    public static final CompanionAbility ALWAYS_HAPPY = register(new SimpleAbility(KindredSpirits.id("always_happy"), 20,
            com.breakinblocks.kindredspirits.companion.QuokkaSupport::breed));

    public static final CompanionAbility SWIFT_STEP = register(new SimpleAbility(KindredSpirits.id("swift_step"), 40,
            (companion, owner) -> ownerEffect(companion, owner, OWNER_RANGE_SQR, MobEffects.SPEED, 60,
                    companion.hasEquipment(KindredItems.RUNNING_SHOES.get()) ? 1 : 0)));

    public static final CompanionAbility MENDING_PRESENCE = register(new SimpleAbility(KindredSpirits.id("mending_presence"), 200,
            (companion, owner) -> {
                if (companion.ownerWithin(owner, OWNER_RANGE_SHORT_SQR) && owner.getHealth() < owner.getMaxHealth()) {
                    owner.heal(1.0f);
                }
            }));

    public static final CompanionAbility NIGHT_WARD = register(new SimpleAbility(KindredSpirits.id("night_ward"), 40,
            (companion, owner) -> {
                if (companion.ownerWithin(owner, OWNER_RANGE_FIRE_SQR) && owner.isOnFire()) {
                    owner.clearFire();
                }
            }));

    public static final CompanionAbility KINDLED_VIGOUR = register(new SimpleAbility(KindredSpirits.id("kindled_vigour"), 100,
            (companion, owner) -> {
                if (companion.getHealth() < companion.getMaxHealth()) {
                    companion.heal(1.0f);
                }
            }));

    public static final CompanionAbility SHADOW_BALL = register(new ShadowBallAbility(KindredSpirits.id("shadow_ball")));

    public static final CompanionAbility SAVAGE_LEAP = register(new SavageLeapAbility(KindredSpirits.id("savage_leap")));

    public static final CompanionAbility CRUSHING_MIGHT = register(new SimpleAbility(KindredSpirits.id("crushing_might"), 40,
            (companion, owner) -> {
                if (ownerEffect(companion, owner, OWNER_RANGE_SQR, MobEffects.HASTE, 60, 0))
                    companion.burst(KindredParticles.CRUSHING_MIGHT.get(), 1);
            }));

    public static final CompanionAbility DRAGON_BREATH = register(new DragonBreathAbility(KindredSpirits.id("dragon_breath")));

    public static final CompanionAbility FORGE_DRAFT = register(new ForgeDraftAbility(KindredSpirits.id("forge_draft")));

    public static final CompanionAbility MIRROR_STRIKE =
            register(new MirrorStrikeAbility(KindredSpirits.id("mirror_strike")));

    public static final CompanionAbility ALPHA = register(new PassiveAbility(KindredSpirits.id("alpha"), List.of()));

    public static final CompanionAbility ALPHA_BOOST = register(new PassiveAbility(KindredSpirits.id("alpha_boost"),
            List.of(new AttributeBonus(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    KindredSpirits.id("alpha_boost"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)))));

    public static final CompanionAbility DRAGONFIRE = register(new PassiveAbility(KindredSpirits.id("dragonfire"), List.of()));

    public static final CompanionAbility EAT_THAT = register(new PassiveAbility(KindredSpirits.id("eat_that"), List.of()));

    public static final CompanionAbility HELPING_HAND = register(new PassiveAbility(KindredSpirits.id("helping_hand"), List.of()));

    public static final CompanionAbility WISPLIGHT = register(new WisplightAbility(KindredSpirits.id("wisplight")));

    public static final CompanionAbility NIGHT_LIGHT = register(new NightLightAbility(KindredSpirits.id("night_light")));

    public static final CompanionAbility XRAY_STOMP = register(new XrayStompAbility(KindredSpirits.id("xray_stomp")));

    public static final CompanionAbility KILN_BREATH = register(new KilnBreathAbility(KindredSpirits.id("kiln_breath")));

    public static final CompanionAbility FRIENDLY_FACE = register(ownerBuff("friendly_face", 600,
            OWNER_RANGE_LONG_SQR, MobEffects.HERO_OF_THE_VILLAGE, 1200, 0));

    public static final CompanionAbility TINKER = register(new TinkerAbility(KindredSpirits.id("tinker")));

    public static final CompanionAbility SNACK_THIEF = register(new SnackThiefAbility(KindredSpirits.id("snack_thief")));

    public static final CompanionAbility ENERGIZED_CHAOS =
            register(new EnergizedChaosAbility(KindredSpirits.id("energized_chaos")));

    public static final CompanionAbility ONE_WITH_THE_NIGHT = register(new SimpleAbility(KindredSpirits.id("one_with_the_night"), 40,
            (companion, owner) -> {
                if (!ownerEffect(companion, owner, OWNER_RANGE_LONG_SQR, MobEffects.NIGHT_VISION, 400, 0)) {
                    return;
                }
                if (owner.level().getBrightness(LightLayer.BLOCK, owner.blockPosition()) <= 3) {
                    ownerEffect(companion, owner, OWNER_RANGE_LONG_SQR, MobEffects.INVISIBILITY, 80, 0);
                }
            }));

    public static boolean ownerEffect(CompanionEntity companion, @Nullable ServerPlayer owner, double rangeSqr,
                                      Holder<MobEffect> effect, int duration, int amplifier) {
        if (!companion.ownerWithin(owner, rangeSqr)) {
            return false;
        }

        owner.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
        return true;
    }

    private static CompanionAbility ownerBuff(String path, int interval, double rangeSqr,
                                              Holder<MobEffect> effect, int duration, int amplifier) {
        return new SimpleAbility(KindredSpirits.id(path), interval,
                (companion, owner) -> ownerEffect(companion, owner, rangeSqr, effect, duration, amplifier));
    }

    private static @Nullable LivingEntity attackableTarget(CompanionEntity companion, double maxRangeSqr) {
        LivingEntity target = companion.getTarget();

        if (target == null || !target.isAlive()
                || companion.distanceToSqr(target) > maxRangeSqr
                || !companion.hasLineOfSight(target)) {
            return null;
        }

        return target;
    }

    private static void breathTrail(ServerLevel level, CompanionEntity companion, LivingEntity target,
                                    ParticleOptions particle, int steps, int count, double spread, double speed) {
        Vec3 from = companion.getEyePosition();
        Vec3 step = target.getBoundingBox().getCenter().subtract(from).scale(1.0 / steps);

        for (int i = 1; i <= steps; i++) {
            Vec3 point = from.add(step.scale(i));
            level.sendParticles(particle, point.x, point.y, point.z, count, spread, spread, spread, speed);
        }
    }

    public static void hitParticles(ServerLevel level, LivingEntity target, ParticleOptions particle, int count,
                                    double height, double spread, double speed) {
        level.sendParticles(particle, target.getX(), target.getY(height), target.getZ(),
                count, spread, spread, spread, speed);
    }

    private static void breathStrike(ServerLevel level, CompanionEntity companion, LivingEntity target,
                                     ParticleOptions trail, int trailSteps, int trailCount, double trailSpread,
                                     double trailSpeed, ParticleOptions hit, int hitCount,
                                     float damageMultiplier, Consumer<LivingEntity> onHit) {
        breathTrail(level, companion, target, trail, trailSteps, trailCount, trailSpread, trailSpeed);
        hitParticles(level, target, hit, hitCount, 0.5, 0.25, 0.02);
        companion.magicHurt(level, target, companion.attackDamage() * damageMultiplier);
        onHit.accept(target);
    }

    public static void expireSnackScale(ServerPlayer owner) {
        SnackThiefAbility.expire(owner);
    }

    public static boolean enabled() {
        return KindredConfig.COMMON.abilitiesEnabled.get();
    }

    public static CompanionAbility register(CompanionAbility ability) {
        REGISTRY.put(ability.id(), ability);
        return ability;
    }

    public static @Nullable CompanionAbility get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Map<Identifier, CompanionAbility> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private record SavageLeapAbility(Identifier id) implements CompanionAbility {
        private static final int COOLDOWN_TICKS = 200;
        private static final double MIN_RANGE_SQR = 9.0;
        private static final double MAX_RANGE_SQR = CompanionEntity.RUN_DISTANCE * CompanionEntity.RUN_DISTANCE;
        private static final double SLAM_RADIUS = 2.5;
        private static final float DAMAGE_MULTIPLIER = 1.5f;
        private static final double KNOCKBACK = 0.5;

        @Override
        public int intervalTicks() {
            return 10;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!companion.isAbilityReady(this.id)) {
                return;
            }

            LivingEntity target = attackableTarget(companion, MAX_RANGE_SQR);

            if (target == null || !companion.onGround()
                    || companion.distanceToSqr(target) < MIN_RANGE_SQR) {
                return;
            }

            companion.setAbilityCooldown(this.id, COOLDOWN_TICKS);
            companion.faceInstantly(target);
            companion.playAction(CompanionAnimations.JUMP_ATTACK,
                    companion.species().sounds().specialAttack().get(), 1.0f, 1.0f);

            Vec3 leap = target.position().subtract(companion.position()).normalize();
            companion.setDeltaMovement(new Vec3(leap.x * 0.85, 0.45, leap.z * 0.85));
            companion.hurtMarked = true;

            companion.scheduleLeapImpact(companion.attackDamage() * DAMAGE_MULTIPLIER, SLAM_RADIUS, KNOCKBACK);
        }
    }

    private record ShadowBallAbility(Identifier id) implements CompanionAbility {
        private static final double RANGE_SQR = 256.0;
        private static final int TRAIL_STEPS = 12;
        private static final float DAMAGE_MULTIPLIER = 0.5f;

        @Override
        public int intervalTicks() {
            return 60;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)) {
                return;
            }

            LivingEntity target = attackableTarget(companion, RANGE_SQR);
            if (target == null) {
                return;
            }

            companion.playSpecialAttack(0.8f, 1.6f);
            breathStrike(level, companion, target, KindredParticles.SHADOW_FLAME.get(), TRAIL_STEPS, 2, 0.06, 0.0,
                    KindredParticles.SHADOW_BURST.get(), 10, DAMAGE_MULTIPLIER,
                    hit -> hit.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, true, true)));
        }
    }

    private record DragonBreathAbility(Identifier id) implements CompanionAbility {
        private static final double RANGE_SQR = 100.0;
        private static final int COOLDOWN_TICKS = 60;
        private static final int METEOR_COOLDOWN_TICKS = 80;
        private static final double METEOR_RANGE_SQR = 256.0;
        private static final float DAMAGE_MULTIPLIER = 0.75f;
        private static final float FIRE_SECONDS = 4.0f;
        private static final int TRAIL_STEPS = 14;
        private static final float CLOUD_RADIUS = 1.5f;
        private static final int CLOUD_TICKS = 100;
        private static final int CLOUD_WAIT = 5;
        static ParticleOptions breathParticle() { return KindredParticles.DRAGON_FLAME.get(); }

        @Override
        public int intervalTicks() {
            return 10;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level) || !companion.isAbilityReady(this.id)) {
                return;
            }

            boolean meteor = companion.hasEquipment(KindredItems.DRAGON_TABLET.get());
            LivingEntity target = attackableTarget(companion, meteor ? METEOR_RANGE_SQR : RANGE_SQR);
            if (target == null) {
                return;
            }

            companion.setAbilityCooldown(this.id, meteor ? METEOR_COOLDOWN_TICKS : COOLDOWN_TICKS);
            companion.playSpecialAttack(1.0f, meteor ? 0.7f : 1.2f);

            if (meteor) {
                companion.callMeteor(target);
                return;
            }

            breathStrike(level, companion, target, breathParticle(), TRAIL_STEPS, 3, 0.08, 0.01,
                    breathParticle(), 8, DAMAGE_MULTIPLIER, hit -> {
                        hit.igniteForSeconds(FIRE_SECONDS);
                        spawnBreathCloud(level, companion, hit);
                    });
        }

        private static void spawnBreathCloud(ServerLevel level, CompanionEntity companion, LivingEntity target) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, target.getX(), target.getY(), target.getZ());

            cloud.setOwner(companion);
            cloud.setCustomParticle(KindredParticles.DRAGON_SMOKE.get());
            cloud.setRadius(CLOUD_RADIUS);
            cloud.setDuration(CLOUD_TICKS);
            cloud.setWaitTime(CLOUD_WAIT);
            cloud.setRadiusPerTick(-CLOUD_RADIUS / CLOUD_TICKS);

            level.addFreshEntity(cloud);
        }
    }

    private record ForgeDraftAbility(Identifier id) implements CompanionAbility {
        private static final int RADIUS = 4;
        private static final int EXTRA_TICKS = 20;

        @Override
        public int intervalTicks() {
            return EXTRA_TICKS;
        }

        @Override
        public boolean scalesWithBond() {
            return false;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)) {
                return;
            }

            BlockPos center = companion.blockPosition();
            int radius = RADIUS + companion.bondTier();

            for (BlockPos pos : BlockPosUtil.cube(center, radius)) {
                if (!level.isLoaded(pos)) continue;
                BlockState state = level.getBlockState(pos);

                if (!(state.getBlock() instanceof AbstractFurnaceBlock)
                        || !state.getOptionalValue(AbstractFurnaceBlock.LIT).orElse(false)
                        || !(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) {
                    continue;
                }

                for (int i = 0; i < EXTRA_TICKS; i++) {
                    AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
                }

                level.sendParticles(KindredParticles.FORGE_EMBER.get(),
                        pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.15, 0.02, 0.15, 0.0);
            }
        }
    }

    private record WisplightAbility(Identifier id) implements CompanionAbility {
        private static final int RADIUS = 8;
        private static final int MAX_LIGHT = 3;
        private static final int MAX_WISPS = 16;
        private static final int EXPIRY_TICKS = 6000;
        private static final double MAX_DISTANCE = 64.0;
        private static final int ATTEMPTS = 24;

        @Override
        public int intervalTicks() {
            return 200;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)) {
                return;
            }

            CompanionLights lights = companion.lights();
            lights.sweep(level, this.id, companion.position(), EXPIRY_TICKS, MAX_DISTANCE);

            if (lights.count(this.id) >= MAX_WISPS) {
                return;
            }

            BlockPos center = companion.blockPosition();
            for (int i = 0; i < ATTEMPTS; i++) {
                BlockPos pos = center.offset(
                        companion.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS,
                        companion.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS,
                        companion.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS);

                if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir()
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        || level.getBrightness(LightLayer.BLOCK, pos) > MAX_LIGHT) {
                    continue;
                }

                if (lights.place(level, this.id, pos, KindredBlocks.WISP_LIGHT.get().defaultBlockState(), MAX_WISPS)) {
                    level.sendParticles(KindredParticles.SPIRIT_WISP.get(), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                            6, 0.2, 0.2, 0.2, 0.01);
                    return;
                }
            }
        }
    }

    private record NightLightAbility(Identifier id) implements CompanionAbility {
        private static final int LEVEL = 12;
        private static final double MAX_DISTANCE = 2.0;

        @Override
        public int intervalTicks() {
            return 1;
        }

        @Override
        public boolean scalesWithBond() {
            return false;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)) {
                return;
            }

            CompanionLights lights = companion.lights();
            BlockPos pos = companion.blockPosition();

            lights.sweep(level, this.id, pos.getCenter(), CompanionLights.NO_EXPIRY, MAX_DISTANCE);
            if (!lights.holds(this.id, pos)) {
                lights.place(level, this.id, pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LEVEL), 1);
            }
        }
    }

    private record XrayStompAbility(Identifier id) implements CompanionAbility {
        private static final double OWNER_RANGE_SQR = 64.0;
        private static final int RADIUS = 12;
        private static final int REVEAL_TICKS = 200;
        private static final int MAX_POSITIONS = 512;

        @Override
        public int intervalTicks() {
            return 1200;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)
                    || !companion.ownerWithin(owner, OWNER_RANGE_SQR) || !companion.onGround()) {
                return;
            }

            companion.playSpecialAttack(1.0f, 0.8f);
            companion.startShockwave();

            List<BlockPos> ores = new ArrayList<>();
            for (BlockPos pos : BlockPosUtil.cube(companion.blockPosition(), RADIUS)) {
                if (level.isLoaded(pos) && level.getBlockState(pos).is(Tags.Blocks.ORES)) {
                    ores.add(pos.immutable());
                    if (ores.size() >= MAX_POSITIONS) {
                        break;
                    }
                }
            }

            KindredNetworking.sendOreReveal(owner, ores, REVEAL_TICKS);
        }
    }

    private record KilnBreathAbility(Identifier id) implements CompanionAbility {
        private static final int COOLDOWN_TICKS = 6000;
        private static final double OWNER_RANGE_SQR = 256.0;

        @Override
        public int intervalTicks() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean activate(CompanionEntity companion, ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level) || !companion.isAbilityReady(this.id)
                    || !companion.ownerWithin(owner, OWNER_RANGE_SQR)) {
                return false;
            }

            Inventory inventory = owner.getInventory();
            int smelted = 0;

            for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                SingleRecipeInput input = new SingleRecipeInput(stack);
                Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
                if (recipe.isEmpty()) {
                    continue;
                }

                ItemStack result = recipe.get().value().assemble(input);
                if (result.isEmpty()) {
                    continue;
                }

                int total = result.getCount() * stack.getCount();
                ItemStack replacement = result.copyWithCount(Math.min(total, result.getMaxStackSize()));
                inventory.setItem(slot, replacement);

                for (int remaining = total - replacement.getCount(); remaining > 0; remaining -= result.getMaxStackSize()) {
                    inventory.placeItemBackInInventory(result.copyWithCount(Math.min(remaining, result.getMaxStackSize())));
                }
                smelted += stack.getCount();
            }

            if (smelted == 0) {
                owner.sendSystemMessage(Component.translatable("message.kindredspirits.kiln_nothing"));
                return false;
            }

            companion.setAbilityCooldown(this.id, COOLDOWN_TICKS);
            companion.faceInstantly(owner);
            companion.playSpecialAttack(1.0f, 0.9f);
            breathTrail(level, companion, owner, DragonBreathAbility.breathParticle(), 10, 3, 0.1, 0.01);
            level.sendParticles(KindredParticles.FORGE_EMBER.get(), owner.getX(), owner.getY(1.0), owner.getZ(), 20, 0.4, 0.5, 0.4, 0.02);
            owner.sendSystemMessage(Component.translatable("message.kindredspirits.kiln_smelted", smelted));
            return true;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
        }
    }

    private record TinkerAbility(Identifier id) implements CompanionAbility {
        private static final int RADIUS = 4;
        private static final int EXTRA_TICKS = 4;
        private static final double BASE_CHANCE = 0.10;
        private static final double CHANCE_PER_LEVEL = 0.01;

        @Override
        public int intervalTicks() {
            return 200;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)
                    || companion.getRandom().nextDouble() >= BASE_CHANCE + companion.getLevel() * CHANCE_PER_LEVEL) {
                return;
            }

            BlockPos accelerated = CompanionTinkering.accelerateNearby(level, companion.blockPosition(),
                    RADIUS, EXTRA_TICKS, companion.getRandom());

            if (accelerated != null) {
                CompanionTinkering.sparks(level, accelerated);
            }
        }
    }

    private record SnackThiefAbility(Identifier id) implements CompanionAbility {
        private static final int BUFF_TICKS = 1200;
        private static final double CHANCE = 0.5;
        private static final double SCALE_BONUS = 0.25;
        private static final AttributeModifier SNACK_SCALE = new AttributeModifier(
                KindredSpirits.id("snack_scale"), SCALE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        @Override
        public int intervalTicks() {
            return 3600;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!companion.ownerWithin(owner, OWNER_RANGE_SQR)
                    || companion.getRandom().nextDouble() >= CHANCE
                    || !steal(companion, owner)) {
                return;
            }

            companion.playAction(CompanionAnimations.INTERACT, KindredSounds.GREMLIN_SNACK.get(), 0.8f, 1.0f);
            companion.burst(KindredParticles.SNACK_CRUMB.get(), 6);

            switch (companion.getRandom().nextInt(3)) {
                case 0 -> ownerEffect(companion, owner, OWNER_RANGE_SQR, MobEffects.STRENGTH, BUFF_TICKS, 0);
                case 1 -> ownerEffect(companion, owner, OWNER_RANGE_SQR, MobEffects.SPEED, BUFF_TICKS, 0);
                default -> grow(owner);
            }
        }

        private static boolean steal(CompanionEntity companion, ServerPlayer owner) {
            Inventory inventory = owner.getInventory();
            List<Integer> slots = new ArrayList<>();

            for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
                if (slot != inventory.getSelectedSlot() && inventory.getItem(slot).has(DataComponents.FOOD)) {
                    slots.add(slot);
                }
            }

            if (slots.isEmpty()) {
                return false;
            }

            inventory.removeItem(slots.get(companion.getRandom().nextInt(slots.size())), 1);
            return true;
        }

        private static void grow(ServerPlayer owner) {
            AttributeInstance scale = owner.getAttribute(Attributes.SCALE);
            if (scale == null) {
                return;
            }

            if (!scale.hasModifier(SNACK_SCALE.id())) {
                scale.addTransientModifier(SNACK_SCALE);
            }
            owner.setData(KindredAttachments.SNACK_SCALE, owner.level().getGameTime() + BUFF_TICKS);
        }

        static void expire(ServerPlayer owner) {
            long until = owner.getData(KindredAttachments.SNACK_SCALE);
            if (until == 0L || owner.level().getGameTime() < until) {
                return;
            }

            AttributeInstance scale = owner.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.removeModifier(SNACK_SCALE.id());
            }
            owner.setData(KindredAttachments.SNACK_SCALE, 0L);
        }
    }

    private record EnergizedChaosAbility(Identifier id) implements CompanionAbility {
        private static final int RADIUS = 4;
        private static final double CHANCE = 0.3;

        @Override
        public int intervalTicks() {
            return 600;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            if (!(companion.level() instanceof ServerLevel level)
                    || companion.getRandom().nextDouble() >= CHANCE) {
                return;
            }

            List<BlockPos> targets = new ArrayList<>();
            for (BlockPos pos : BlockPosUtil.cube(companion.blockPosition(), RADIUS)) {
                if (!level.isLoaded(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) {
                    targets.add(pos.immutable());
                }
            }

            if (targets.isEmpty()) {
                return;
            }

            BlockPos pick = targets.get(companion.getRandom().nextInt(targets.size()));
            boolean deepslate = level.getBlockState(pick).is(Blocks.DEEPSLATE);

            level.setBlockAndUpdate(pick, deepslate
                    ? Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()
                    : Blocks.REDSTONE_ORE.defaultBlockState());
            companion.playSpecialAttack(0.8f, 1.4f);
            CompanionTinkering.sparks(level, pick);
        }
    }

    private record PassiveAbility(Identifier id, List<AttributeBonus> attributeBonuses) implements CompanionAbility {
        @Override
        public int intervalTicks() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
        }
    }

    private record SimpleAbility(Identifier id, int intervalTicks, Effect effect) implements CompanionAbility {
        @Override
        public void serverTick(CompanionEntity companion, @Nullable ServerPlayer owner) {
            this.effect.apply(companion, owner);
        }
    }

    @FunctionalInterface
    private interface Effect {
        void apply(CompanionEntity companion, @Nullable ServerPlayer owner);
    }

    private CompanionAbilities() {
    }
}
