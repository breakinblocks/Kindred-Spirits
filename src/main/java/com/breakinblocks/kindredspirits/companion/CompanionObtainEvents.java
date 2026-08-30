package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.registry.KindredItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class CompanionObtainEvents {
    private static final int TRADER_PRICE = 32;
    private static final int TRADER_MAX_USES = 1;
    private static final int TRADER_XP = 1;
    private static final float TRADER_PRICE_MULTIPLIER = 0.05f;
    private static final int HATCH_FIRE_IMMUNITY_TICKS = 200;
    private static final int HATCH_PARTICLES = 12;

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
        level.destroyBlock(eggPos, false);
        CompanionEntity dragon = CompanionSpawns.spawnWild(level, CompanionSpecies.BABY_DRAGON,
                eggPos.getCenter(), level.getRandom().nextFloat() * 360.0f);
        if (dragon != null) {
            dragon.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, HATCH_FIRE_IMMUNITY_TICKS, 0, false, false, true));
            dragon.burst(ParticleTypes.LAVA, HATCH_PARTICLES);
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
        if (!(event.getTarget() instanceof Fox fox) || !event.getItemStack().is(Items.SCULK)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getLevel().isClientSide()) {
            return;
        }

        event.getItemStack().consume(1, event.getEntity());
        CompanionSpawns.transform(fox, CompanionSpecies.NIGHTFOX, ParticleTypes.SCULK_SOUL);
    }
}
