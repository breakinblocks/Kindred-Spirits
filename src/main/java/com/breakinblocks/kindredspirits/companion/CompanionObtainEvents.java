package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.registry.KindredParticles;
import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredAttachments;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionObtainEvents {
    private static final int TRADER_PRICE = 32;
    private static final int TRADER_MAX_USES = 1;
    private static final int TRADER_XP = 1;
    private static final float TRADER_PRICE_MULTIPLIER = 0.05f;
    private static final int HATCH_FIRE_IMMUNITY_TICKS = 200;
    private static final int HATCH_PARTICLES = 12;
    private static final int TICKS_PER_DAY = 24000;
    private static final int GREMLIN_HOUR_START = 18000;
    private static final int GREMLIN_HOUR_END = 23000;

    private CompanionObtainEvents() {
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(Blocks.LAVA)) {
            return;
        }

        BlockPos lava = event.getPos();
        for (Direction direction : Direction.values()) {
            BlockPos eggPos = lava.relative(direction);
            if (level.getBlockState(eggPos).is(Blocks.DRAGON_EGG)) {
                hatchDragonEgg(level, eggPos);
                return;
            }
        }
    }

    private static void hatchDragonEgg(ServerLevel level, BlockPos eggPos) {
        CompanionEntity dragon = CompanionSpawns.spawnWild(level, CompanionSpecies.BABY_DRAGON,
                eggPos.getCenter(), level.getRandom().nextFloat() * 360.0f);
        if (dragon != null) {
            level.destroyBlock(eggPos, false);
            dragon.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, HATCH_FIRE_IMMUNITY_TICKS, 0, false, false, true));
            dragon.burst(KindredParticles.METEOR_BURST.get(), HATCH_PARTICLES);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof WanderingTrader trader) || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }

        ItemStack egg = new ItemStack(KindredItems.spawnEggs().get(CompanionSpecies.MINI_PLAYER).get());
        MerchantOffers offers = trader.getOffers();
        if (offers.stream().anyMatch(offer -> ItemStack.isSameItem(offer.getResult(), egg))) {
            return;
        }

        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, TRADER_PRICE), egg,
                TRADER_MAX_USES, TRADER_XP, TRADER_PRICE_MULTIPLIER));
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();

        if (event.getTarget() instanceof Fox fox && stack.is(Items.SCULK)) {
            transform(event, fox, CompanionSpecies.NIGHTFOX, ParticleTypes.SCULK_SOUL);
            return;
        }

        if (event.getTarget() instanceof Rabbit rabbit) {
            interactRabbit(event, rabbit, stack);
            return;
        }

        if (event.getTarget() instanceof Wolf wolf && stack.is(KindredItems.GOLDEN_BONE.get())) {
            interactWolf(event, wolf);
        }
    }

    private static void interactWolf(PlayerInteractEvent.EntityInteract event, Wolf wolf) {
        if (wolf.isTame() && !wolf.isOwnedBy(event.getEntity())) {
            return;
        }

        boolean owned = wolf.isTame();
        ItemStack armour = wolf.getBodyArmorItem().copy();
        CompanionEntity direwolf = transform(event, wolf, CompanionSpecies.DIREWOLF, KindredParticles.PACK_CALL.get());
        if (direwolf == null) {
            return;
        }

        if (wolf.hasCustomName()) {
            direwolf.setCustomName(wolf.getCustomName());
        }
        if (owned) {
            direwolf.adopt(event.getEntity());
            if (!armour.isEmpty()) {
                direwolf.setEquipment(armour);
            }
        } else if (!armour.isEmpty()) {
            direwolf.spawnAtLocation((ServerLevel) direwolf.level(), armour);
        }
    }

    private static void interactRabbit(PlayerInteractEvent.EntityInteract event, Rabbit rabbit, ItemStack stack) {
        if (stack.is(Items.CARROT)) {
            if (!event.getLevel().isClientSide()) {
                rabbit.setData(KindredAttachments.RABBIT_FED_BY, Optional.of(event.getEntity().getUUID()));
            }
            return;
        }

        UUID fedBy = rabbit.getData(KindredAttachments.RABBIT_FED_BY).orElse(null);

        if (!stack.is(Items.COOKED_CHICKEN) || !event.getEntity().getUUID().equals(fedBy)
                || !isWitchingHour(event.getLevel())) {
            return;
        }

        transform(event, rabbit, CompanionSpecies.GREMLIN, KindredParticles.GREMLIN_SPARK.get());
    }

    private static boolean isWitchingHour(Level level) {
        long timeOfDay = level.getDefaultClockTime() % TICKS_PER_DAY;
        return timeOfDay >= GREMLIN_HOUR_START && timeOfDay < GREMLIN_HOUR_END;
    }

    private static @Nullable CompanionEntity transform(PlayerInteractEvent.EntityInteract event, Mob source,
                                                       CompanionSpecies species, ParticleOptions particle) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getLevel().isClientSide()) {
            return null;
        }

        CompanionEntity companion = CompanionSpawns.transform(source, species, particle);
        if (companion != null) {
            event.getItemStack().consume(1, event.getEntity());
        }
        return companion;
    }
}
