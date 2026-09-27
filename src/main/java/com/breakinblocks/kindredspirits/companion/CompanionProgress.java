package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class CompanionProgress {
    public static final int REST_DELAY_TICKS = 5 * 60 * 20;
    private static final long NO_ANCHOR = Long.MIN_VALUE;

    public static final Codec<CompanionProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("saturation", 0).forGetter(p -> p.saturation),
                    Codec.LONG.optionalFieldOf("anchor", NO_ANCHOR).forGetter(p -> p.anchor),
                    Codec.INT.optionalFieldOf("rested", 0).forGetter(p -> p.rested),
                    Codec.LONG.optionalFieldOf("last_gain", 0L).forGetter(p -> p.lastGain),
                    Codec.LONG.optionalFieldOf("feed_ready_at", 0L).forGetter(p -> p.feedReadyAt),
                    Codec.DOUBLE.optionalFieldOf("fractional_share", 0.0).forGetter(p -> p.fractionalShare),
                    Codec.LONG.optionalFieldOf("last_tick", 0L).forGetter(p -> p.lastTick))
            .apply(instance, CompanionProgress::new));

    public static final StreamCodec<ByteBuf, CompanionProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            p -> p.saturation,
            ByteBufCodecs.VAR_LONG,
            p -> p.anchor,
            ByteBufCodecs.VAR_INT,
            p -> p.rested,
            ByteBufCodecs.VAR_LONG,
            p -> p.lastGain,
            ByteBufCodecs.VAR_LONG,
            p -> p.feedReadyAt,
            ByteBufCodecs.DOUBLE,
            p -> p.fractionalShare,
            ByteBufCodecs.VAR_LONG,
            p -> p.lastTick,
            CompanionProgress::new);

    private int saturation;
    private long anchor;
    private int rested;
    private long lastGain;
    private long feedReadyAt;
    private double fractionalShare;
    private long lastTick;

    public CompanionProgress() {
        this(0, NO_ANCHOR, 0, 0L, 0L, 0.0, 0L);
    }

    private CompanionProgress(
            int saturation,
            long anchor,
            int rested,
            long lastGain,
            long feedReadyAt,
            double fractionalShare,
            long lastTick) {
        this.saturation = saturation;
        this.anchor = anchor;
        this.rested = rested;
        this.lastGain = lastGain;
        this.feedReadyAt = feedReadyAt;
        this.fractionalShare =
                Double.isFinite(fractionalShare) ? Math.clamp(fractionalShare, 0.0, Math.nextDown(1.0)) : 0.0;
        this.lastTick = lastTick;
    }

    public CompanionProgress copy() {
        return new CompanionProgress(
                this.saturation,
                this.anchor,
                this.rested,
                this.lastGain,
                this.feedReadyAt,
                this.fractionalShare,
                this.lastTick);
    }

    public int shareExperience(int amount, double fraction) {
        if (amount <= 0 || fraction <= 0 || !Double.isFinite(fraction)) return 0;
        double shared = Math.min(Integer.MAX_VALUE, amount * fraction + this.fractionalShare);
        int whole = (int) Math.floor(shared + 1.0e-9);
        this.fractionalShare = Math.max(0, shared - whole);
        return whole;
    }

    public int saturation() {
        return this.saturation;
    }

    public int rested() {
        return this.rested;
    }

    public long feedReadyAt() {
        return this.feedReadyAt;
    }

    public boolean canFeed(long gameTime) {
        return gameTime >= this.feedReadyAt;
    }

    public int feedSecondsLeft(long gameTime) {
        return (int) Math.max(0L, (this.feedReadyAt - gameTime + 19) / 20);
    }

    public void startFeedCooldown(long gameTime) {
        this.feedReadyAt = gameTime + CompanionBondMath.feedCooldownTicks();
    }

    public double saturationMultiplier() {
        if (this.saturation >= KindredConfig.COMMON.saturationHardCap.get()) {
            return 0.1;
        }
        return this.saturation >= KindredConfig.COMMON.saturationSoftCap.get() ? 0.5 : 1.0;
    }

    public int applyGain(int amount, long gameTime, @Nullable ChunkPos ownerChunk) {
        if (amount <= 0) {
            return 0;
        }

        int gained = Math.max(1, (int) Math.floor(amount * this.saturationMultiplier()));
        int bonus = Math.min(this.rested, gained);
        this.rested -= bonus;
        this.saturation = (int) Math.min(Integer.MAX_VALUE, (long) this.saturation + gained);
        this.lastGain = gameTime;

        if (this.anchor == NO_ANCHOR && ownerChunk != null) {
            this.anchor = ownerChunk.pack();
        }

        return (int) Math.min(Integer.MAX_VALUE, (long) gained + bonus);
    }

    public void creditStoredRest(long gameTime) {
        if (this.lastTick > 0L && gameTime > this.lastTick) {
            long seconds = (gameTime - Math.max(this.lastTick, this.lastGain + REST_DELAY_TICKS)) / 20L;
            if (seconds > 0L) {
                this.rested = (int) Math.min(KindredConfig.COMMON.restedCap.get(), this.rested + seconds);
            }
        }
        this.lastTick = gameTime;
    }

    public void tickSecond(long gameTime, @Nullable ChunkPos ownerChunk) {
        this.lastTick = gameTime;
        if (this.saturation > 0) {
            this.saturation--;
        }

        if (this.anchor != NO_ANCHOR
                && ownerChunk != null
                && ChunkPos.unpack(this.anchor).getChessboardDistance(ownerChunk)
                        >= KindredConfig.COMMON.saturationResetChunks.get()) {
            this.saturation = 0;
            this.anchor = NO_ANCHOR;
        }

        if (this.saturation == 0) {
            this.anchor = NO_ANCHOR;
        }

        if (gameTime - this.lastGain >= REST_DELAY_TICKS) {
            this.rested = Math.min(KindredConfig.COMMON.restedCap.get(), this.rested + 1);
        }
    }
}
