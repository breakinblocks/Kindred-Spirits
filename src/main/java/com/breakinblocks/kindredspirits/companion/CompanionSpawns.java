package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.registry.KindredEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CompanionSpawns {
    private static final int TRANSFORM_PARTICLES = 20;

    private CompanionSpawns() {
    }

    public static @Nullable CompanionEntity spawnWild(ServerLevel level, CompanionSpecies species, Vec3 pos, float yaw) {
        CompanionEntity companion = KindredEntities.type(species).create(level, EntitySpawnReason.EVENT);
        if (companion == null) {
            return null;
        }

        companion.snapTo(pos.x(), pos.y(), pos.z(), Mth.wrapDegrees(yaw), 0.0f);
        level.addFreshEntity(companion);
        level.playSound(null, pos.x(), pos.y(), pos.z(), species.sounds().interact().get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        return companion;
    }

    public static @Nullable CompanionEntity transform(Entity source, CompanionSpecies species, ParticleOptions particle) {
        if (!(source.level() instanceof ServerLevel level)) {
            return null;
        }

        Vec3 pos = source.position();
        float yaw = source.getYRot();
        source.discard();

        CompanionEntity companion = spawnWild(level, species, pos, yaw);
        if (companion != null) {
            companion.burst(particle, TRANSFORM_PARTICLES);
        }
        return companion;
    }
}
