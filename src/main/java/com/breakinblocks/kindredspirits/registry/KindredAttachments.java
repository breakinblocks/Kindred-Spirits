package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class KindredAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KindredSpirits.MOD_ID);

    public record BondRecord(int companionsBonded, int highestLevelReached) {
        public static final BondRecord DEFAULT = new BondRecord(0, 0);

        public static final MapCodec<BondRecord> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("companions_bonded", 0).forGetter(BondRecord::companionsBonded),
                Codec.INT.optionalFieldOf("highest_level_reached", 0).forGetter(BondRecord::highestLevelReached)
        ).apply(instance, BondRecord::new));

        public BondRecord withBonded(int bonded) {
            return new BondRecord(bonded, this.highestLevelReached);
        }

        public BondRecord withHighestLevel(int level) {
            return new BondRecord(this.companionsBonded, Math.max(this.highestLevelReached, level));
        }
    }

    public static final Supplier<AttachmentType<BondRecord>> BOND_RECORD = ATTACHMENT_TYPES.register(
            "bond_record", () -> AttachmentType.builder(() -> BondRecord.DEFAULT)
                    .serialize(BondRecord.CODEC)
                    .copyOnDeath()
                    .build());

    public record CompanionBond(Optional<UUID> companion, Optional<CompanionSnapshot> snapshot,
                                boolean stored, long reviveReadyAt,
                                Optional<Identifier> lastDimension, Optional<BlockPos> lastPos) {
        public static final CompanionBond NONE =
                new CompanionBond(Optional.empty(), Optional.empty(), true, 0L, Optional.empty(), Optional.empty());

        public static final MapCodec<CompanionBond> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.optionalFieldOf("companion").forGetter(CompanionBond::companion),
                CompanionSnapshot.CODEC.optionalFieldOf("snapshot").forGetter(CompanionBond::snapshot),
                Codec.BOOL.optionalFieldOf("stored", true).forGetter(CompanionBond::stored),
                Codec.LONG.optionalFieldOf("revive_ready_at", 0L).forGetter(CompanionBond::reviveReadyAt),
                Identifier.CODEC.optionalFieldOf("last_dimension").forGetter(CompanionBond::lastDimension),
                BlockPos.CODEC.optionalFieldOf("last_pos").forGetter(CompanionBond::lastPos)
        ).apply(instance, CompanionBond::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CompanionBond> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), CompanionBond::companion,
                ByteBufCodecs.optional(CompanionSnapshot.STREAM_CODEC), CompanionBond::snapshot,
                ByteBufCodecs.BOOL, CompanionBond::stored,
                ByteBufCodecs.VAR_LONG, CompanionBond::reviveReadyAt,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), CompanionBond::lastDimension,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), CompanionBond::lastPos,
                CompanionBond::new);

        public boolean isBound() {
            return this.companion.isPresent() && this.snapshot.isPresent();
        }

        public CompanionBond withSnapshot(UUID companion, CompanionSnapshot snapshot) {
            return new CompanionBond(Optional.of(companion), Optional.of(snapshot),
                    this.stored, this.reviveReadyAt, this.lastDimension, this.lastPos);
        }

        public CompanionBond withStored(boolean stored) {
            return new CompanionBond(this.companion, this.snapshot, stored,
                    this.reviveReadyAt, this.lastDimension, this.lastPos);
        }

        public CompanionBond withReviveReadyAt(long readyAt) {
            return new CompanionBond(this.companion, this.snapshot, this.stored,
                    readyAt, this.lastDimension, this.lastPos);
        }

        public CompanionBond withLocation(Identifier dimension, BlockPos pos) {
            return new CompanionBond(this.companion, this.snapshot, this.stored,
                    this.reviveReadyAt, Optional.of(dimension), Optional.of(pos));
        }
    }

    public static final Supplier<AttachmentType<CompanionBond>> COMPANION_BOND = ATTACHMENT_TYPES.register(
            "companion_bond", () -> AttachmentType.builder(() -> CompanionBond.NONE)
                    .serialize(CompanionBond.CODEC)
                    .sync((holder, to) -> holder == to, CompanionBond.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    public static BondRecord get(Player player) {
        return player.getData(BOND_RECORD);
    }

    public static void modify(Player player, UnaryOperator<BondRecord> modifier) {
        player.setData(BOND_RECORD, modifier.apply(player.getData(BOND_RECORD)));
    }

    public static CompanionBond bond(Player player) {
        return player.getData(COMPANION_BOND);
    }

    public static void modifyBond(Player player, UnaryOperator<CompanionBond> modifier) {
        player.setData(COMPANION_BOND, modifier.apply(player.getData(COMPANION_BOND)));
    }

    private KindredAttachments() {
    }
}
