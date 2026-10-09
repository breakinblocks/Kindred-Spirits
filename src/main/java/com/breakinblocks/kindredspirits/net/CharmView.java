package com.breakinblocks.kindredspirits.net;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionAggression;
import com.breakinblocks.kindredspirits.companion.CompanionBondMath;
import com.breakinblocks.kindredspirits.companion.CompanionCommand;
import com.breakinblocks.kindredspirits.companion.CompanionEntity;
import com.breakinblocks.kindredspirits.companion.CompanionLevels;
import com.breakinblocks.kindredspirits.companion.CompanionSnapshot;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import com.breakinblocks.kindredspirits.companion.CompanionStats;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.BondRecord;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record CharmView(
        boolean bound,
        String species,
        Optional<String> name,
        int level,
        int experience,
        int experienceToNext,
        int bondLevel,
        int bondMaxLevel,
        int bondProgress,
        int bondCost,
        int feedSeconds,
        int stars,
        boolean canPrestige,
        float health,
        float maxHealth,
        float armour,
        float attackDamage,
        int command,
        int aggression,
        boolean stored,
        int reviveSeconds,
        boolean present,
        String skin,
        int companionsBonded,
        int highestLevel,
        int highestStars,
        String activeAbility,
        int activeCooldownSeconds,
        ItemStack equipment,
        CompanionStats stats,
        List<String> disabledAbilities,
        int dye) {

    public static CharmView unbound(BondRecord record) {
        return new CharmView(
                false,
                "",
                Optional.empty(),
                0,
                0,
                1,
                0,
                1,
                0,
                1,
                0,
                0,
                false,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0,
                1,
                true,
                0,
                false,
                "",
                record.companionsBonded(),
                record.highestLevelReached(),
                record.highestStars(),
                "",
                0,
                ItemStack.EMPTY,
                CompanionStats.EMPTY,
                List.of(),
                CompanionEntity.NO_DYE);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CharmView> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharmView decode(RegistryFriendlyByteBuf buffer) {
            return new CharmView(
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    CompanionStats.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CharmView view) {
            ByteBufCodecs.BOOL.encode(buffer, view.bound());
            ByteBufCodecs.STRING_UTF8.encode(buffer, view.species());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, view.name());
            ByteBufCodecs.VAR_INT.encode(buffer, view.level());
            ByteBufCodecs.VAR_INT.encode(buffer, view.experience());
            ByteBufCodecs.VAR_INT.encode(buffer, view.experienceToNext());
            ByteBufCodecs.VAR_INT.encode(buffer, view.bondLevel());
            ByteBufCodecs.VAR_INT.encode(buffer, view.bondMaxLevel());
            ByteBufCodecs.VAR_INT.encode(buffer, view.bondProgress());
            ByteBufCodecs.VAR_INT.encode(buffer, view.bondCost());
            ByteBufCodecs.VAR_INT.encode(buffer, view.feedSeconds());
            ByteBufCodecs.VAR_INT.encode(buffer, view.stars());
            ByteBufCodecs.BOOL.encode(buffer, view.canPrestige());
            ByteBufCodecs.FLOAT.encode(buffer, view.health());
            ByteBufCodecs.FLOAT.encode(buffer, view.maxHealth());
            ByteBufCodecs.FLOAT.encode(buffer, view.armour());
            ByteBufCodecs.FLOAT.encode(buffer, view.attackDamage());
            ByteBufCodecs.VAR_INT.encode(buffer, view.command());
            ByteBufCodecs.VAR_INT.encode(buffer, view.aggression());
            ByteBufCodecs.BOOL.encode(buffer, view.stored());
            ByteBufCodecs.VAR_INT.encode(buffer, view.reviveSeconds());
            ByteBufCodecs.BOOL.encode(buffer, view.present());
            ByteBufCodecs.STRING_UTF8.encode(buffer, view.skin());
            ByteBufCodecs.VAR_INT.encode(buffer, view.companionsBonded());
            ByteBufCodecs.VAR_INT.encode(buffer, view.highestLevel());
            ByteBufCodecs.VAR_INT.encode(buffer, view.highestStars());
            ByteBufCodecs.STRING_UTF8.encode(buffer, view.activeAbility());
            ByteBufCodecs.VAR_INT.encode(buffer, view.activeCooldownSeconds());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, view.equipment());
            CompanionStats.STREAM_CODEC.encode(buffer, view.stats());
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buffer, view.disabledAbilities());
            ByteBufCodecs.VAR_INT.encode(buffer, view.dye());
        }
    };

    public boolean usesPlayerSkin() {
        return this.resolveSpecies().map(CompanionSpecies::usesPlayerSkin).orElse(false);
    }

    public static int cooldownSeconds(int ticks) {
        return Math.ceilDiv(ticks, 20);
    }

    public Optional<CompanionSpecies> resolveSpecies() {
        return CompanionSpecies.byName(this.species);
    }

    public CompanionCommand commandValue() {
        return CompanionCommand.byOrdinal(this.command);
    }

    public CompanionAggression aggressionValue() {
        return CompanionAggression.byOrdinal(this.aggression);
    }

    public static CharmView of(CompanionBond bond, @Nullable CompanionEntity live, long gameTime, BondRecord record) {
        CompanionSnapshot snapshot =
                live != null ? CompanionSnapshot.of(live) : bond.snapshot().orElseThrow();
        int bondPoints = snapshot.bondPoints();
        Optional<CompanionAbility> active = live != null ? live.activeAbility() : Optional.empty();

        return new CharmView(
                true,
                snapshot.species(),
                snapshot.name(),
                snapshot.level(),
                snapshot.experience(),
                CompanionLevels.experienceToNext(snapshot.level()),
                snapshot.bondLevel(),
                CompanionBondMath.maxLevel(),
                CompanionBondMath.pointsIntoLevel(bondPoints),
                CompanionBondMath.costToNext(bondPoints),
                snapshot.progress().feedSecondsLeft(gameTime),
                snapshot.stars(),
                CompanionLevels.canPrestige(snapshot.level(), snapshot.stars()),
                live != null ? live.getHealth() : 0.0f,
                live != null ? live.getMaxHealth() : 0.0f,
                live != null ? (float) live.getAttributeValue(Attributes.ARMOR) : 0.0f,
                live != null ? (float) live.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0f,
                snapshot.command(),
                snapshot.aggression(),
                bond.stored(),
                bond.reviveSecondsLeft(gameTime),
                live != null,
                snapshot.skin().orElse(""),
                record.companionsBonded(),
                record.highestLevelReached(),
                record.highestStars(),
                active.map(ability -> ability.id().getPath()).orElse(""),
                active.map(ability -> cooldownSeconds(live.abilityCooldownTicks(ability.id())))
                        .orElse(0),
                snapshot.equipment(),
                live != null
                        ? CompanionStats.of(live)
                        : snapshot.resolveSpecies()
                                .map(species -> CompanionStats.of(species, snapshot))
                                .orElse(CompanionStats.EMPTY),
                snapshot.disabledAbilities(),
                snapshot.dye());
    }

    public boolean isDisabled(CompanionAbility ability) {
        return CompanionSnapshot.isDisabled(this.disabledAbilities, ability);
    }

    public Optional<CompanionAbility> resolveActiveAbility() {
        return this.activeAbility.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(CompanionAbilities.get(KindredSpirits.id(this.activeAbility)));
    }

    public static Component starText(int stars) {
        return Component.literal("★".repeat(Math.max(0, stars)));
    }
}
