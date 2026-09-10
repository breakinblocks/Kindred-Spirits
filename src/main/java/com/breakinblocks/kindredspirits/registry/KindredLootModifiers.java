package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class KindredLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KindredSpirits.MOD_ID);
    public static final MapCodec<TrexEgg> TREX_EGG = TrexEgg.CODEC;
    static { TYPES.register("trex_egg", () -> TREX_EGG); }

    public static final class TrexEgg extends LootModifier {
        private static final MapCodec<TrexEgg> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, TrexEgg::new));
        public TrexEgg(LootItemCondition[] conditions, int priority) { super(conditions, priority); }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            // Brushable blocks retain only the first result; replace rather than append.
            if (context.getRandom().nextInt(8) == 0) {
                loot.clear();
                loot.add(new ItemStack(KindredItems.TREX_EGG.get()));
            }
            return loot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() { return TREX_EGG; }
    }

    private KindredLootModifiers() { }
}
