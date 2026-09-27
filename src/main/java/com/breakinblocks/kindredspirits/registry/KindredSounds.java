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
    public static final DeferredHolder<SoundEvent, SoundEvent> QUOKKA_INTERACT =
            register("entity.quokka.interact");

    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTFOX_AMBIENT =
            register("entity.nightfox.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTFOX_HURT =
            register("entity.nightfox.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTFOX_DEATH =
            register("entity.nightfox.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTFOX_SPECIAL_ATTACK =
            register("entity.nightfox.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTFOX_INTERACT =
            register("entity.nightfox.interact");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_AMBIENT =
            register("entity.trex.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_HURT =
            register("entity.trex.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_DEATH =
            register("entity.trex.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_ATTACK =
            register("entity.trex.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_SPECIAL_ATTACK =
            register("entity.trex.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_INTERACT =
            register("entity.trex.interact");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_AMBIENT =
            register("entity.mini_player.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_HURT =
            register("entity.mini_player.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_DEATH =
            register("entity.mini_player.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_ATTACK =
            register("entity.mini_player.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_SPECIAL_ATTACK =
            register("entity.mini_player.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_INTERACT =
            register("entity.mini_player.interact");
    public static final DeferredHolder<SoundEvent, SoundEvent> BABY_DRAGON_AMBIENT =
            register("entity.baby_dragon.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BABY_DRAGON_HURT =
            register("entity.baby_dragon.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BABY_DRAGON_SPECIAL_ATTACK =
            register("entity.baby_dragon.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BABY_DRAGON_INTERACT =
            register("entity.baby_dragon.interact");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_AMBIENT =
            register("entity.gremlin.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_HURT =
            register("entity.gremlin.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_DEATH =
            register("entity.gremlin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_ATTACK =
            register("entity.gremlin.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_SPECIAL_ATTACK =
            register("entity.gremlin.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_INTERACT =
            register("entity.gremlin.interact");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUOKKA_AMBIENT =
            register("entity.quokka.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUOKKA_HURT =
            register("entity.quokka.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUOKKA_DEATH =
            register("entity.quokka.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUOKKA_ATTACK =
            register("entity.quokka.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUOKKA_SPECIAL_ATTACK =
            register("entity.quokka.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIREWOLF_AMBIENT =
            register("entity.direwolf.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIREWOLF_HURT =
            register("entity.direwolf.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIREWOLF_DEATH =
            register("entity.direwolf.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIREWOLF_ATTACK =
            register("entity.direwolf.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIREWOLF_SPECIAL_ATTACK =
            register("entity.direwolf.special_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIREWOLF_INTERACT =
            register("entity.direwolf.interact");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARM_SUMMON =
            register("charm.summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARM_DISMISS =
            register("charm.dismiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARM_REVIVE =
            register("charm.revive");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP =
            register("level.up");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESTIGE =
            register("prestige");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_EGG_CRACK =
            register("trex.egg_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_EGG_HATCH =
            register("trex.egg_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_LEAP_IMPACT =
            register("trex.leap_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREX_CRUSH =
            register("trex.crush");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINI_PLAYER_BOW =
            register("mini.player_bow");
    public static final DeferredHolder<SoundEvent, SoundEvent> GREMLIN_SNACK =
            register("gremlin.snack");
    public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_IMPACT =
            register("meteor.impact");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, identifier -> SoundEvent.createVariableRangeEvent(identifier));
    }

    private KindredSounds() {
    }
}
