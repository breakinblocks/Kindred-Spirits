package com.breakinblocks.kindredspirits.companion;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum CompanionAggression implements StringRepresentable {
    PASSIVE("passive"),
    NEUTRAL("neutral"),
    AGGRESSIVE("aggressive");

    private final String name;

    CompanionAggression(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component displayName() {
        return Component.translatable("aggression.kindredspirits." + this.name);
    }

    public boolean defendsSelf() {
        return this != PASSIVE;
    }

    public boolean defendsOwner() {
        return this != PASSIVE;
    }

    public boolean joinsOwnerAttacks() {
        return this == AGGRESSIVE;
    }

    public boolean huntsMonsters() {
        return this == AGGRESSIVE;
    }

    public CompanionAggression next() {
        return EnumLookup.next(values(), this);
    }

    public static CompanionAggression byOrdinal(int ordinal) {
        return EnumLookup.byOrdinal(values(), ordinal);
    }

    public static CompanionAggression byName(String name) {
        return EnumLookup.byName(values(), name).orElse(NEUTRAL);
    }
}
