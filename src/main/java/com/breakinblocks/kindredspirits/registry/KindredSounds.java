package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KindredSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KindredSpirits.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BABY_DRAGON_DEATH =
            register("entity.baby_dragon.death");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, identifier -> SoundEvent.createVariableRangeEvent(identifier));
    }

    private KindredSounds() {
    }
}
