package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.advancement.CompanionTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KindredTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, KindredSpirits.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, CompanionTrigger> COMPANION =
            TRIGGERS.register("companion", CompanionTrigger::new);

    private KindredTriggers() {}
}
