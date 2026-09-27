package com.breakinblocks.kindredspirits.companion;

import java.util.regex.Pattern;

public final class CompanionSkin {
    public static final String MARKER = "_skin_";
    public static final int MAX_LENGTH = 32;
    private static final Pattern VALID = Pattern.compile("[a-z0-9_]{1," + MAX_LENGTH + "}");

    public static boolean isValid(String skin) {
        return VALID.matcher(skin).matches();
    }

    private CompanionSkin() {}
}
