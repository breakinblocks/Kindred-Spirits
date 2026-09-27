package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.registry.KindredParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Native pixel sprites advance over their lifetime; no interpolation between texture frames. */
public final class KindredParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final boolean emissive;

    private KindredParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double dx,
            double dy,
            double dz,
            SpriteSet sprites,
            KindredParticles.Effect effect) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.emissive = effect.emissive();
        this.lifetime = effect.lifetime();
        this.quadSize = effect.size();
        this.gravity = effect.gravity();
        this.friction = 0.9f;
        this.hasPhysics = false;
        this.setParticleSpeed(dx, dy, dz);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) this.setSpriteFromAge(this.sprites);
        // Fade only near the end; keep the silhouette readable through the authored animation.
        this.alpha = Math.min(1.0f, Math.max(0.0f, (this.lifetime - this.age) / 4.0f));
    }

    @Override
    public int getLightCoords(float partialTick) {
        return this.emissive ? 0xF000F0 : super.getLightCoords(partialTick);
    }

    public static void register(RegisterParticleProvidersEvent event) {
        for (var effect : KindredParticles.effects()) {
            event.registerSpriteSet(
                    effect.type().get(),
                    sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                            new KindredParticle(level, x, y, z, dx, dy, dz, sprites, effect));
        }
    }
}
