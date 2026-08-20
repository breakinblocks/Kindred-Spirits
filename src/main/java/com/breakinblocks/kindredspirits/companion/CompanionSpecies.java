package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public enum CompanionSpecies implements StringRepresentable {
    EMBERFOX("emberfox",
            new Size(0.7f, 0.8f, 1.0f),
            new Stats(14.0, 0.32, 3.0, 2.0, 0.0),
            CombatStyle.RANGED, true,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.WALK,
                    CompanionAnimations.SPECIAL_ATTACK, CompanionAnimations.INTERACT),
            new SoundSet(SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH,
                    SoundEvents.FOX_BITE, SoundEvents.EVOKER_CAST_SPELL, SoundEvents.FOX_SNIFF),
            List.of(
                    new Unlock(1, CompanionAbilities.SWIFT_STEP),
                    new Unlock(1, CompanionAbilities.SHADOW_BALL),
                    new Unlock(8, CompanionAbilities.EMBER_WARD),
                    new Unlock(16, CompanionAbilities.KINDLED_VIGOUR))),

    TREX("trex",
            new Size(1.2f, 1.7f, 0.6f),
            new Stats(30.0, 0.25, 4.0, 2.0, 0.6),
            CombatStyle.MELEE, false,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.WALK, CompanionAnimations.RUN,
                    CompanionAnimations.ATTACK, CompanionAnimations.SPECIAL_ATTACK,
                    CompanionAnimations.JUMP_ATTACK, CompanionAnimations.SPAWN, CompanionAnimations.DEATH),
            new SoundSet(SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH,
                    SoundEvents.RAVAGER_ATTACK, SoundEvents.RAVAGER_ROAR, SoundEvents.RAVAGER_STEP),
            List.of(
                    new Unlock(1, CompanionAbilities.SAVAGE_LEAP),
                    new Unlock(1, CompanionAbilities.CRUSHING_MIGHT),
                    new Unlock(10, CompanionAbilities.KINDLED_VIGOUR)));

    private final String name;
    private final Size size;
    private final Stats stats;
    private final CombatStyle combatStyle;
    private final boolean huntsDangerousPrey;
    private final Set<String> animations;
    private final SoundSet sounds;
    private final List<Unlock> unlocks;

    CompanionSpecies(String name, Size size, Stats stats,
                     CombatStyle combatStyle, boolean huntsDangerousPrey, Set<String> animations,
                     SoundSet sounds, List<Unlock> unlocks) {
        this.name = name;
        this.size = size;
        this.stats = stats;
        this.combatStyle = combatStyle;
        this.huntsDangerousPrey = huntsDangerousPrey;
        this.animations = animations;
        this.sounds = sounds;
        this.unlocks = unlocks;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public float width() {
        return this.size.width();
    }

    public float height() {
        return this.size.height();
    }

    public float renderScale() {
        return this.size.renderScale();
    }

    public double baseHealth() {
        return this.stats.health();
    }

    public double moveSpeed() {
        return this.stats.moveSpeed();
    }

    public double attackDamage() {
        return this.stats.attackDamage();
    }

    public double armour() {
        return this.stats.armour();
    }

    public double knockbackResistance() {
        return this.stats.knockbackResistance();
    }

    public CombatStyle combatStyle() {
        return this.combatStyle;
    }

    public boolean huntsDangerousPrey() {
        return this.huntsDangerousPrey;
    }

    public boolean hasAnimation(String name) {
        return this.animations.contains(name);
    }

    public SoundSet sounds() {
        return this.sounds;
    }

    public List<Unlock> unlocks() {
        return this.unlocks;
    }

    public List<CompanionAbility> abilitiesAt(int level) {
        return this.unlocks.stream()
                .filter(unlock -> level >= unlock.level())
                .map(Unlock::ability)
                .toList();
    }

    public String translationKey() {
        return "entity.kindredspirits." + this.name;
    }

    public static Optional<CompanionSpecies> byName(String name) {
        for (CompanionSpecies species : values()) {
            if (species.name.equals(name)) {
                return Optional.of(species);
            }
        }
        return Optional.empty();
    }

    public record Size(float width, float height, float renderScale) {
    }

    public record Stats(double health, double moveSpeed, double attackDamage,
                        double armour, double knockbackResistance) {
    }

    public record Unlock(int level, CompanionAbility ability) {
    }

    public record SoundSet(SoundEvent ambient, SoundEvent hurt, SoundEvent death,
                           SoundEvent attack, SoundEvent specialAttack, SoundEvent interact) {
    }

    public enum CombatStyle {
        MELEE,
        RANGED
    }
}
