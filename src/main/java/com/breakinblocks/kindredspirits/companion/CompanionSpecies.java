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
    EMBERFOX("emberfox", 0.7f, 0.8f, 1.0f, 14.0, 0.32, 3.0, CombatStyle.RANGED,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.WALK,
                    CompanionAnimations.SPECIAL_ATTACK, CompanionAnimations.INTERACT),
            new SoundSet(SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH,
                    SoundEvents.FOX_BITE, SoundEvents.EVOKER_CAST_SPELL, SoundEvents.FOX_SNIFF),
            List.of(
                    new Unlock(1, CompanionAbilities.SWIFT_STEP),
                    new Unlock(1, CompanionAbilities.SHADOW_BALL),
                    new Unlock(8, CompanionAbilities.EMBER_WARD),
                    new Unlock(16, CompanionAbilities.KINDLED_VIGOUR)));

    private final String name;
    private final float width;
    private final float height;
    private final float renderScale;
    private final double baseHealth;
    private final double moveSpeed;
    private final double attackDamage;
    private final CombatStyle combatStyle;
    private final Set<String> animations;
    private final SoundSet sounds;
    private final List<Unlock> unlocks;

    CompanionSpecies(String name, float width, float height, float renderScale,
                     double baseHealth, double moveSpeed, double attackDamage,
                     CombatStyle combatStyle, Set<String> animations,
                     SoundSet sounds, List<Unlock> unlocks) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.renderScale = renderScale;
        this.baseHealth = baseHealth;
        this.moveSpeed = moveSpeed;
        this.attackDamage = attackDamage;
        this.combatStyle = combatStyle;
        this.animations = animations;
        this.sounds = sounds;
        this.unlocks = unlocks;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }

    public float renderScale() {
        return this.renderScale;
    }

    public double baseHealth() {
        return this.baseHealth;
    }

    public double moveSpeed() {
        return this.moveSpeed;
    }

    public double attackDamage() {
        return this.attackDamage;
    }

    public CombatStyle combatStyle() {
        return this.combatStyle;
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
