package com.breakinblocks.kindredspirits.companion;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum CompanionCommand implements StringRepresentable {
    FOLLOW("follow"),
    STAY("stay"),
    WANDER("wander");

    private final String name;

    CompanionCommand(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component displayName() {
        return Component.translatable("command.kindredspirits.mode." + this.name);
    }

    public CompanionCommand next() {
        return EnumLookup.next(values(), this);
    }

    public static CompanionCommand byOrdinal(int ordinal) {
        return EnumLookup.byOrdinal(values(), ordinal);
    }

    public static CompanionCommand byName(String name) {
        return EnumLookup.byName(values(), name).orElse(FOLLOW);
    }
}
