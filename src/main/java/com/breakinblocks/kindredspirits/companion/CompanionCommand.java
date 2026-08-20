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
        return values()[(this.ordinal() + 1) % values().length];
    }

    public static CompanionCommand byOrdinal(int ordinal) {
        CompanionCommand[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static CompanionCommand byName(String name) {
        for (CompanionCommand value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return FOLLOW;
    }
}
