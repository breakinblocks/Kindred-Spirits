package com.breakinblocks.kindredspirits.companion;

import java.util.Optional;
import net.minecraft.util.StringRepresentable;

public final class EnumLookup {
    public static <E extends Enum<E>> E byOrdinal(E[] values, int ordinal) {
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static <E extends Enum<E>> E next(E[] values, E current) {
        return values[(current.ordinal() + 1) % values.length];
    }

    public static <E extends Enum<E> & StringRepresentable> Optional<E> byName(E[] values, String name) {
        for (E value : values) {
            if (value.getSerializedName().equals(name)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private EnumLookup() {}
}
