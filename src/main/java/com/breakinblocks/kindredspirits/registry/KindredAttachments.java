package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class KindredAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KindredSpirits.MOD_ID);

    public static final Supplier<AttachmentType<Long>> CHARMED = ATTACHMENT_TYPES.register(
            "charmed",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("until").codec())
                    .build());

    public static final Supplier<AttachmentType<Long>> QUOKKA_AGED_AT = ATTACHMENT_TYPES.register(
            "quokka_aged_at", () -> AttachmentType.builder(() -> Long.MIN_VALUE).build());

    public record BondRecord(int companionsBonded, int highestLevelReached, int highestStars) {
        public static final BondRecord DEFAULT = new BondRecord(0, 0, 0);

        public static final MapCodec<BondRecord> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.INT.optionalFieldOf("companions_bonded", 0).forGetter(BondRecord::companionsBonded),
                        Codec.INT
                                .optionalFieldOf("highest_level_reached", 0)
                                .forGetter(BondRecord::highestLevelReached),
                        Codec.INT.optionalFieldOf("highest_stars", 0).forGetter(BondRecord::highestStars))
                .apply(instance, BondRecord::new));

        public BondRecord withBonded(int bonded) {
            return new BondRecord(bonded, this.highestLevelReached, this.highestStars);
        }

        public BondRecord withHighestLevel(int level) {
            return new BondRecord(this.companionsBonded, Math.max(this.highestLevelReached, level), this.highestStars);
        }

        public BondRecord withHighestStars(int stars) {
            return new BondRecord(this.companionsBonded, this.highestLevelReached, Math.max(this.highestStars, stars));
        }
    }

    public static final Supplier<AttachmentType<BondRecord>> BOND_RECORD = ATTACHMENT_TYPES.register(
            "bond_record",
            () -> AttachmentType.builder(() -> BondRecord.DEFAULT)
                    .serialize(BondRecord.CODEC.codec())
                    .copyOnDeath()
                    .build());

    public record CompanionBond(
            Optional<UUID> companion,
            Optional<CompanionSnapshot> snapshot,
            boolean stored,
            long reviveReadyAt,
            Optional<ResourceLocation> lastDimension,
            Optional<BlockPos> lastPos) {
        public static final CompanionBond NONE =
                new CompanionBond(Optional.empty(), Optional.empty(), true, 0L, Optional.empty(), Optional.empty());

        public static final MapCodec<CompanionBond> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        UUIDUtil.CODEC.optionalFieldOf("companion").forGetter(CompanionBond::companion),
                        CompanionSnapshot.CODEC.optionalFieldOf("snapshot").forGetter(CompanionBond::snapshot),
                        Codec.BOOL.optionalFieldOf("stored", true).forGetter(CompanionBond::stored),
                        Codec.LONG.optionalFieldOf("revive_ready_at", 0L).forGetter(CompanionBond::reviveReadyAt),
                        ResourceLocation.CODEC
                                .optionalFieldOf("last_dimension")
                                .forGetter(CompanionBond::lastDimension),
                        BlockPos.CODEC.optionalFieldOf("last_pos").forGetter(CompanionBond::lastPos))
                .apply(instance, CompanionBond::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CompanionBond> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
                CompanionBond::companion,
                ByteBufCodecs.optional(CompanionSnapshot.STREAM_CODEC),
                CompanionBond::snapshot,
                ByteBufCodecs.BOOL,
                CompanionBond::stored,
                ByteBufCodecs.VAR_LONG,
                CompanionBond::reviveReadyAt,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
                CompanionBond::lastDimension,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
                CompanionBond::lastPos,
                CompanionBond::new);

        public boolean isBound() {
            return this.companion.isPresent() && this.snapshot.isPresent();
        }

        public boolean isBoundTo(UUID id) {
            return this.isBound() && this.companion.get().equals(id);
        }

        public int reviveSecondsLeft(long gameTime) {
            return (int) Math.max(0, Math.ceilDiv(this.reviveReadyAt - gameTime, 20));
        }

        public CompanionBond withSnapshot(UUID companion, CompanionSnapshot snapshot) {
            return new CompanionBond(
                    Optional.of(companion),
                    Optional.of(snapshot),
                    this.stored,
                    this.reviveReadyAt,
                    this.lastDimension,
                    this.lastPos);
        }

        public CompanionBond withStored(boolean stored) {
            return new CompanionBond(
                    this.companion, this.snapshot, stored, this.reviveReadyAt, this.lastDimension, this.lastPos);
        }

        public CompanionBond withReviveReadyAt(long readyAt) {
            return new CompanionBond(
                    this.companion, this.snapshot, this.stored, readyAt, this.lastDimension, this.lastPos);
        }

        public CompanionBond withLocation(ResourceLocation dimension, BlockPos pos) {
            return new CompanionBond(
                    this.companion,
                    this.snapshot,
                    this.stored,
                    this.reviveReadyAt,
                    Optional.of(dimension),
                    Optional.of(pos));
        }
    }

    public static final class PlayerActivity {
        private double x = Double.NaN;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private long lastActive;

        public void update(Player player, long gameTime) {
            boolean moved = this.x != player.getX()
                    || this.y != player.getY()
                    || this.z != player.getZ()
                    || this.yaw != player.getYRot()
                    || this.pitch != player.getXRot();

            if (moved) {
                this.x = player.getX();
                this.y = player.getY();
                this.z = player.getZ();
                this.yaw = player.getYRot();
                this.pitch = player.getXRot();
                this.lastActive = gameTime;
            }
        }

        public boolean isAfk(long gameTime, int afkTicks) {
            return gameTime - this.lastActive >= afkTicks;
        }
    }

    public static final Supplier<AttachmentType<PlayerActivity>> PLAYER_ACTIVITY = ATTACHMENT_TYPES.register(
            "player_activity",
            () -> AttachmentType.builder(() -> new PlayerActivity()).build());

    public static final Supplier<AttachmentType<Long>> SNACK_SCALE = ATTACHMENT_TYPES.register(
            "snack_scale", () -> AttachmentType.builder(() -> 0L).build());

    public static final Supplier<AttachmentType<Boolean>> BOTTLE_XP = ATTACHMENT_TYPES.register(
            "bottle_xp",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("bottle").codec())
                    .build());

    public static final Supplier<AttachmentType<Long>> ORB_XP_PICKUP = ATTACHMENT_TYPES.register(
            "orb_xp_pickup", () -> AttachmentType.builder(() -> -1L).build());

    public static final Supplier<AttachmentType<Optional<UUID>>> RABBIT_FED_BY = ATTACHMENT_TYPES.register(
            "rabbit_fed_by",
            () -> AttachmentType.<Optional<UUID>>builder(() -> Optional.empty())
                    .serialize(UUIDUtil.CODEC.optionalFieldOf("player").codec())
                    .build());

    public static final Supplier<AttachmentType<ItemContainerContents>> COMPANION_STORAGE = ATTACHMENT_TYPES.register(
            "companion_storage",
            () -> AttachmentType.builder(() -> ItemContainerContents.EMPTY)
                    .serialize(ItemContainerContents.CODEC.fieldOf("items").codec())
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<CompanionBond>> COMPANION_BOND = ATTACHMENT_TYPES.register(
            "companion_bond",
            () -> AttachmentType.builder(() -> CompanionBond.NONE)
                    .serialize(CompanionBond.CODEC.codec())
                    .sync((holder, to) -> holder == to, CompanionBond.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    /** Server-derived values: COMMON progression config is not synchronized to clients. */
    public record TooltipProgress(int experienceToNext, int bondLevel, int bondMaxLevel) {
        public static final TooltipProgress EMPTY = new TooltipProgress(0, 0, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, TooltipProgress> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                TooltipProgress::experienceToNext,
                ByteBufCodecs.VAR_INT,
                TooltipProgress::bondLevel,
                ByteBufCodecs.VAR_INT,
                TooltipProgress::bondMaxLevel,
                TooltipProgress::new);
    }

    public static final Supplier<AttachmentType<TooltipProgress>> TOOLTIP_PROGRESS = ATTACHMENT_TYPES.register(
            "tooltip_progress",
            () -> AttachmentType.builder(() -> TooltipProgress.EMPTY)
                    .sync((holder, to) -> holder == to, TooltipProgress.STREAM_CODEC)
                    .build());

    public static void syncTooltip(Player player) {
        if (player.level().isClientSide) return;
        TooltipProgress progress = bond(player)
                .snapshot()
                .map(snapshot -> new TooltipProgress(
                        CompanionLevels.experienceToNext(snapshot.level()),
                        snapshot.bondLevel(),
                        CompanionBondMath.maxLevel()))
                .orElse(TooltipProgress.EMPTY);
        if (!progress.equals(player.getData(TOOLTIP_PROGRESS))) player.setData(TOOLTIP_PROGRESS, progress);
    }

    public static PlayerActivity activity(Player player) {
        return player.getData(PLAYER_ACTIVITY);
    }

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
        syncTooltip(player);
    }

    public static SimpleContainer storageContainer(Player player) {
        SimpleContainer container = new SimpleContainer(CompanionLevels.MAX_STORAGE_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                player.setData(COMPANION_STORAGE, ItemContainerContents.fromItems(this.getItems()));
            }
        };

        player.getData(COMPANION_STORAGE).copyInto(container.getItems());
        return container;
    }

    private KindredAttachments() {}
}
