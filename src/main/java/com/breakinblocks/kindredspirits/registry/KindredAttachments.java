package com.breakinblocks.kindredspirits.registry;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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

    public static BondRecord get(Player player) {
        return player.getData(BOND_RECORD);
    }

    public static void modify(Player player, UnaryOperator<BondRecord> modifier) {
        player.setData(BOND_RECORD, modifier.apply(player.getData(BOND_RECORD)));
    }

    private KindredAttachments() {
    }
}
