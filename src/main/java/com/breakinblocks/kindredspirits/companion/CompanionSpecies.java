package com.breakinblocks.kindredspirits.companion;

import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbility;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.registry.KindredSounds;
import com.breakinblocks.kindredspirits.config.KindredConfig.SpeciesStat;
import com.breakinblocks.kindredspirits.KindredSpirits;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.Set;

public enum CompanionSpecies implements StringRepresentable {
    NIGHTFOX("nightfox",
            new Size(0.7f, 0.8f, 1.0f, 0.0f),
            new Stats(14.0, 0.32, 3.0, 2.0, 0.0, 1.0),
            CombatStyle.RANGED, true,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.WALK,
                    CompanionAnimations.SPECIAL_ATTACK, CompanionAnimations.INTERACT),
            new SoundSet(SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH,
                    SoundEvents.FOX_BITE, SoundEvents.EVOKER_CAST_SPELL, SoundEvents.FOX_SNIFF),
            List.of(
                    Unlock.level(1, CompanionAbilities.SWIFT_STEP),
                    Unlock.level(1, CompanionAbilities.SHADOW_BALL),
                    Unlock.level(1, CompanionAbilities.WISPLIGHT),
                    Unlock.bond(5, CompanionAbilities.NIGHT_LIGHT),
                    Unlock.level(8, CompanionAbilities.NIGHT_WARD),
                    Unlock.level(16, CompanionAbilities.KINDLED_VIGOUR),
                    Unlock.level(30, CompanionAbilities.ONE_WITH_THE_NIGHT))),

    TREX("trex",
            new Size(1.2f, 1.7f, 0.6f, 0.5f),
            new Stats(30.0, 0.25, 4.0, 2.0, 0.6, 1.0),
            CombatStyle.MELEE, false,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.WALK, CompanionAnimations.RUN,
                    CompanionAnimations.ATTACK, CompanionAnimations.SPECIAL_ATTACK,
                    CompanionAnimations.JUMP_ATTACK, CompanionAnimations.SPAWN, CompanionAnimations.DEATH),
            new SoundSet(SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH,
                    SoundEvents.RAVAGER_ATTACK, SoundEvents.RAVAGER_ROAR, SoundEvents.RAVAGER_STEP),
            List.of(
                    Unlock.level(1, CompanionAbilities.SAVAGE_LEAP),
                    Unlock.level(1, CompanionAbilities.CRUSHING_MIGHT),
                    Unlock.level(1, CompanionAbilities.ALPHA),
                    Unlock.bond(5, CompanionAbilities.ALPHA_BOOST),
                    Unlock.level(10, CompanionAbilities.KINDLED_VIGOUR),
                    Unlock.level(30, CompanionAbilities.XRAY_STOMP))),

    MINI_PLAYER("mini_player",
            new Size(0.4f, 1.2f, 0.6f, 0.0f),
            new Stats(18.0, 0.30, 4.0, 2.0, 0.0, 0.5),
            CombatStyle.HYBRID, true,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.WALK, CompanionAnimations.RUN,
                    CompanionAnimations.SIT, CompanionAnimations.ATTACK, CompanionAnimations.SHOOT,
                    CompanionAnimations.SPECIAL_ATTACK, CompanionAnimations.INTERACT,
                    CompanionAnimations.HURT, CompanionAnimations.SPAWN, CompanionAnimations.DEATH),
            new SoundSet(SoundEvents.PLAYER_BREATH, SoundEvents.PLAYER_HURT, SoundEvents.PLAYER_DEATH,
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_BURP),
            List.of(
                    Unlock.level(1, CompanionAbilities.MIRROR_STRIKE),
                    Unlock.level(1, CompanionAbilities.EAT_THAT),
                    Unlock.bond(5, CompanionAbilities.HELPING_HAND),
                    Unlock.level(12, CompanionAbilities.MENDING_PRESENCE),
                    Unlock.level(30, CompanionAbilities.FRIENDLY_FACE))),

    BABY_DRAGON("baby_dragon",
            new Size(0.7f, 1.2f, 0.6f, 0.0f),
            new Stats(10.0, 0.36, 7.0, 2.0, 0.0, 1.5),
            CombatStyle.RANGED, true,
            Set.of(CompanionAnimations.IDLE, CompanionAnimations.FLY, CompanionAnimations.SIT,
                    CompanionAnimations.SIT_STILL, CompanionAnimations.SIT_RARE,
                    CompanionAnimations.ATTACK, CompanionAnimations.SPECIAL_ATTACK,
                    CompanionAnimations.SPAWN, CompanionAnimations.DEATH),
            new SoundSet(SoundEvents.PARROT_IMITATE_ENDER_DRAGON, SoundEvents.ENDER_DRAGON_HURT,
                    SoundEvents.ENDER_DRAGON_FLAP, SoundEvents.ENDER_DRAGON_GROWL,
                    SoundEvents.ENDER_DRAGON_SHOOT, SoundEvents.GENERIC_EAT.value())
                    .withDeath(KindredSounds.BABY_DRAGON_DEATH),
            List.of(
                    Unlock.level(1, CompanionAbilities.DRAGON_BREATH),
                    Unlock.level(1, CompanionAbilities.FORGE_DRAFT),
                    Unlock.bond(5, CompanionAbilities.DRAGONFIRE),
                    Unlock.level(30, CompanionAbilities.KILN_BREATH)));

    public static final double DEFAULT_FOLLOW_RANGE = 24.0;

    private final String name;
    private final Size size;
    private final Stats stats;
    private final CombatStyle combatStyle;
    private final boolean huntsDangerousPrey;
    private final Set<String> animations;
    private final SoundSet sounds;
    private final List<Unlock> unlocks;
    private List<String> headBones;
    private TagKey<Item> tamingTag;
    private TagKey<Item> equipmentTag;

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

    public double hitboxOffset() {
        return this.size.hitboxOffset();
    }

    public Stats defaults() {
        return this.stats;
    }

    public double baseHealth() {
        return KindredConfig.speciesStat(this, SpeciesStat.HEALTH, this.stats.health());
    }

    public double moveSpeed() {
        return KindredConfig.speciesStat(this, SpeciesStat.MOVE_SPEED, this.stats.moveSpeed());
    }

    public double attackDamage() {
        return KindredConfig.speciesStat(this, SpeciesStat.ATTACK_DAMAGE, this.stats.attackDamage());
    }

    public double armour() {
        return KindredConfig.speciesStat(this, SpeciesStat.ARMOUR, this.stats.armour());
    }

    public double knockbackResistance() {
        return KindredConfig.speciesStat(this, SpeciesStat.KNOCKBACK_RESISTANCE, this.stats.knockbackResistance());
    }

    public double followRange() {
        return KindredConfig.speciesStat(this, SpeciesStat.FOLLOW_RANGE, DEFAULT_FOLLOW_RANGE);
    }

    public double growthMultiplier() {
        return KindredConfig.speciesStat(this, SpeciesStat.GROWTH, this.stats.growth());
    }

    public TagKey<Item> tamingTag() {
        if (this.tamingTag == null) {
            this.tamingTag = this.itemTag("taming");
        }

        return this.tamingTag;
    }

    public TagKey<Item> equipmentTag() {
        if (this.equipmentTag == null) {
            this.equipmentTag = this.itemTag("equipment");
        }

        return this.equipmentTag;
    }

    private TagKey<Item> itemTag(String folder) {
        return TagKey.create(Registries.ITEM, KindredSpirits.id(folder + "/" + this.name));
    }

    public boolean usesPlayerSkin() {
        return this == MINI_PLAYER;
    }

    public boolean carriesWeapons() {
        return this == MINI_PLAYER;
    }

    public int storageRows() {
        return switch (this) {
            case NIGHTFOX, BABY_DRAGON -> 1;
            case TREX, MINI_PLAYER -> 2;
        };
    }

    public int storageSlots() {
        return this.storageRows() * 9;
    }

    public List<String> headBones() {
        if (this.headBones == null) {
            this.headBones = switch (this) {
                case TREX -> List.of("neck_upper", "head_main");
                case MINI_PLAYER, BABY_DRAGON -> List.of("head");
                case NIGHTFOX -> List.of();
            };
        }

        return this.headBones;
    }

    public float maxHeadYaw() {
        return 30.0f;
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

    public List<CompanionAbility> abilitiesAt(int level, int bondLevel) {
        return this.unlocks.stream()
                .filter(unlock -> unlock.isMet(level, bondLevel))
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

    public record Size(float width, float height, float renderScale, double hitboxOffset) {
    }

    public record Stats(double health, double moveSpeed, double attackDamage,
                        double armour, double knockbackResistance, double growth) {
    }

    public record Unlock(Gate gate, int required, CompanionAbility ability) {
        public static Unlock level(int level, CompanionAbility ability) {
            return new Unlock(Gate.LEVEL, level, ability);
        }

        public static Unlock bond(int bondLevel, CompanionAbility ability) {
            return new Unlock(Gate.BOND, bondLevel, ability);
        }

        public boolean isMet(int level, int bondLevel) {
            return (this.gate == Gate.LEVEL ? level : bondLevel) >= this.required;
        }

        public Component requirement() {
            return Component.translatable(this.gate.key, this.required);
        }

        public enum Gate {
            LEVEL("unlock.kindredspirits.level"),
            BOND("unlock.kindredspirits.bond");

            private final String key;

            Gate(String key) {
                this.key = key;
            }
        }
    }

    public record SoundSet(Supplier<SoundEvent> ambient, Supplier<SoundEvent> hurt,
                           Supplier<SoundEvent> death, Supplier<SoundEvent> attack,
                           Supplier<SoundEvent> specialAttack, Supplier<SoundEvent> interact) {
        public SoundSet(SoundEvent ambient, SoundEvent hurt, SoundEvent death,
                        SoundEvent attack, SoundEvent specialAttack, SoundEvent interact) {
            this(() -> ambient, () -> hurt, () -> death, () -> attack, () -> specialAttack, () -> interact);
        }

        public SoundSet withDeath(Supplier<SoundEvent> death) {
            return new SoundSet(this.ambient, this.hurt, death, this.attack, this.specialAttack, this.interact);
        }
    }

    public enum CombatStyle {
        MELEE,
        RANGED,
        HYBRID
    }
}
