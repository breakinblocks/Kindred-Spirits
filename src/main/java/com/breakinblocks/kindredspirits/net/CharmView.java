package com.breakinblocks.kindredspirits.net;

import com.breakinblocks.kindredspirits.companion.CompanionAggression;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;

public record CharmView(boolean bound, String species, Optional<String> name,
                        int level, int experience, int experienceToNext, int bond, int maxBond,
                        float health, float maxHealth, float armour, float attackDamage,
                        int command, int aggression, boolean stored, int reviveSeconds, boolean present) {

    public static final CharmView EMPTY = new CharmView(false, "", Optional.empty(),
            0, 0, 1, 0, 1, 0.0f, 0.0f, 0.0f, 0.0f, 0, 1, true, 0, false);

    public static final StreamCodec<ByteBuf, CharmView> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharmView decode(ByteBuf buffer) {
            return new CharmView(
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer));
        }

        @Override
        public void encode(ByteBuf buffer, CharmView view) {
            ByteBufCodecs.BOOL.encode(buffer, view.bound());
            ByteBufCodecs.STRING_UTF8.encode(buffer, view.species());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, view.name());
            ByteBufCodecs.VAR_INT.encode(buffer, view.level());
            ByteBufCodecs.VAR_INT.encode(buffer, view.experience());
            ByteBufCodecs.VAR_INT.encode(buffer, view.experienceToNext());
            ByteBufCodecs.VAR_INT.encode(buffer, view.bond());
            ByteBufCodecs.VAR_INT.encode(buffer, view.maxBond());
            ByteBufCodecs.FLOAT.encode(buffer, view.health());
            ByteBufCodecs.FLOAT.encode(buffer, view.maxHealth());
            ByteBufCodecs.FLOAT.encode(buffer, view.armour());
            ByteBufCodecs.FLOAT.encode(buffer, view.attackDamage());
            ByteBufCodecs.VAR_INT.encode(buffer, view.command());
            ByteBufCodecs.VAR_INT.encode(buffer, view.aggression());
            ByteBufCodecs.BOOL.encode(buffer, view.stored());
            ByteBufCodecs.VAR_INT.encode(buffer, view.reviveSeconds());
            ByteBufCodecs.BOOL.encode(buffer, view.present());
        }
    };

    public Optional<CompanionSpecies> resolveSpecies() {
        return CompanionSpecies.byName(this.species);
    }

    public CompanionCommand commandValue() {
        return CompanionCommand.byOrdinal(this.command);
    }

    public CompanionAggression aggressionValue() {
        return CompanionAggression.byOrdinal(this.aggression);
    }

    public static CharmView of(CompanionBond bond, CompanionEntity live, long gameTime) {
        CompanionSnapshot snapshot = bond.snapshot().orElseThrow();
        int level = live != null ? live.getLevel() : snapshot.level();

        return new CharmView(true,
                live != null ? live.species().getSerializedName() : snapshot.species(),
                live != null
                        ? Optional.ofNullable(live.getCustomName()).map(net.minecraft.network.chat.Component::getString)
                        : snapshot.name(),
                level,
                live != null ? live.getExperience() : snapshot.experience(),
                CompanionLevels.experienceToNext(level),
                live != null ? live.getBond() : snapshot.bond(),
                CompanionLevels.bondCap(),
                live != null ? live.getHealth() : 0.0f,
                live != null ? live.getMaxHealth() : 0.0f,
                live != null ? (float) live.getAttributeValue(Attributes.ARMOR) : 0.0f,
                live != null ? (float) live.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0f,
                live != null ? live.getCommand().ordinal() : snapshot.command(),
                live != null ? live.getAggression().ordinal() : snapshot.aggression(),
                bond.stored(),
                (int) Math.max(0, (bond.reviveReadyAt() - gameTime) / 20),
                live != null);
    }
}
