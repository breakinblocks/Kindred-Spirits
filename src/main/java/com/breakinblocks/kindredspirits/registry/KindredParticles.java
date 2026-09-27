package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Shared registration and motion profiles; sprites and providers are loaded only on the client. */
public final class KindredParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KindredSpirits.MOD_ID);
    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPIRIT_WISP =
            register("spirit_wisp", 0.12f, 32, -0.015f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHADOW_FLAME =
            register("shadow_flame", 0.14f, 16, -0.02f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHADOW_BURST =
            register("shadow_burst", 0.22f, 15, 0, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LEVEL_STAR =
            register("level_star", 0.16f, 25, -0.025f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BOND_HEART =
            register("bond_heart", 0.15f, 24, -0.02f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SUMMON_RUNE =
            register("summon_rune", 0.23f, 24, -0.01f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISMISS_RUNE =
            register("dismiss_rune", 0.23f, 24, 0.005f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REVIVE_BLOOM =
            register("revive_bloom", 0.23f, 25, -0.025f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EARTH_IMPACT =
            register("earth_impact", 0.24f, 15, 0.06f, false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CRUSHING_MIGHT =
            register("crushing_might", 0.17f, 20, -0.01f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MIRROR_SLASH =
            register("mirror_slash", 0.25f, 12, 0, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CRAFT_SPARK =
            register("craft_spark", 0.15f, 20, -0.02f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON_FLAME =
            register("dragon_flame", 0.18f, 20, -0.025f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON_SMOKE =
            register("dragon_smoke", 0.24f, 30, -0.008f, false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FORGE_EMBER =
            register("forge_ember", 0.12f, 20, -0.035f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> METEOR_BURST =
            register("meteor_burst", 0.3f, 20, 0.01f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GREMLIN_SPARK =
            register("gremlin_spark", 0.14f, 12, 0, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SNACK_CRUMB =
            register("snack_crumb", 0.12f, 16, 0.08f, false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAPPY_BLOOM =
            register("happy_bloom", 0.17f, 25, -0.02f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EGG_WARMTH =
            register("egg_warmth", 0.15f, 24, -0.025f, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SWIFT_MOTE =
            register("swift_mote", 0.12f, 12, 0, true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PACK_CALL =
            register("pack_call", 0.2f, 25, -0.01f, true);

    public record Effect(
            DeferredHolder<ParticleType<?>, SimpleParticleType> type,
            float size,
            int lifetime,
            float gravity,
            boolean emissive) {}

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(
            String name, float size, int lifetime, float gravity, boolean emissive) {
        var type = TYPES.register(name, () -> new SimpleParticleType(false));
        EFFECTS.add(new Effect(type, size, lifetime, gravity, emissive));
        return type;
    }

    public static List<Effect> effects() {
        return List.copyOf(EFFECTS);
    }

    private KindredParticles() {}
}
