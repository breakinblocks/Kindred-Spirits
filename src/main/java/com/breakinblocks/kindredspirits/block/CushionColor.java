package com.breakinblocks.kindredspirits.block;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public enum CushionColor implements StringRepresentable {
    NONE(null),
    WHITE(DyeColor.WHITE),
    ORANGE(DyeColor.ORANGE),
    MAGENTA(DyeColor.MAGENTA),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    PINK(DyeColor.PINK),
    GRAY(DyeColor.GRAY),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN),
    PURPLE(DyeColor.PURPLE),
    BLUE(DyeColor.BLUE),
    BROWN(DyeColor.BROWN),
    GREEN(DyeColor.GREEN),
    RED(DyeColor.RED),
    BLACK(DyeColor.BLACK);

    public static final int LINEN = 0xE3D1AE;

    private final @Nullable DyeColor dye;
    private final String name;

    CushionColor(@Nullable DyeColor dye) {
        this.dye = dye;
        this.name = this.name().toLowerCase(Locale.ROOT);
    }

    public @Nullable DyeColor dye() {
        return this.dye;
    }

    public int rgb() {
        return this.dye == null ? LINEN : this.dye.getTextureDiffuseColor() & 0xFFFFFF;
    }

    public static CushionColor of(DyeColor dye) {
        for (CushionColor colour : values()) {
            if (colour.dye == dye) {
                return colour;
            }
        }
        return NONE;
    }

    public static CushionColor byName(@Nullable String name) {
        for (CushionColor colour : values()) {
            if (colour.name.equals(name)) {
                return colour;
            }
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
