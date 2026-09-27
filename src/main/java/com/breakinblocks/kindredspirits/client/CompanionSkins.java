package com.breakinblocks.kindredspirits.client;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.companion.CompanionSkin;
import com.breakinblocks.kindredspirits.companion.CompanionSpecies;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CompanionSkins {
    private static final String FOLDER = "textures/entity";
    private static final String PNG = ".png";
    private static final Map<Identifier, Boolean> PRESENT = new ConcurrentHashMap<>();
    private static final Map<CompanionSpecies, List<String>> AVAILABLE = new ConcurrentHashMap<>();

    public static Identifier skinned(Identifier texture, String skin) {
        if (skin.isEmpty() || !texture.getPath().endsWith(PNG)) {
            return texture;
        }
        Identifier candidate = texture.withPath(
                path -> path.substring(0, path.length() - PNG.length()) + CompanionSkin.MARKER + skin + PNG);
        return exists(candidate) ? candidate : texture;
    }

    public static List<String> available(CompanionSpecies species) {
        if (species.usesPlayerSkin()) {
            return List.of();
        }
        return AVAILABLE.computeIfAbsent(species, CompanionSkins::scan);
    }

    public static Component displayName(CompanionSpecies species, String skin) {
        if (skin.isEmpty()) {
            return Component.translatable("screen.kindredspirits.skin_default");
        }
        String key = "skin.kindredspirits." + species.getSerializedName() + "." + skin;
        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        String words = skin.replace('_', ' ');
        return Component.literal(words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1));
    }

    public static boolean exists(Identifier texture) {
        return PRESENT.computeIfAbsent(
                texture,
                id -> Minecraft.getInstance()
                        .getResourceManager()
                        .getResource(id)
                        .isPresent());
    }

    public static void clear() {
        PRESENT.clear();
        AVAILABLE.clear();
    }

    private static List<String> scan(CompanionSpecies species) {
        String prefix = FOLDER + "/" + species.getSerializedName() + CompanionSkin.MARKER;
        return Minecraft.getInstance()
                .getResourceManager()
                .listResources(
                        FOLDER,
                        id -> id.getNamespace().equals(KindredSpirits.MOD_ID)
                                && id.getPath().startsWith(prefix)
                                && id.getPath().endsWith(PNG))
                .keySet()
                .stream()
                .map(id -> id.getPath().substring(prefix.length(), id.getPath().length() - PNG.length()))
                .filter(CompanionSkin::isValid)
                .filter(skin -> !skin.endsWith("_glowmask") && !skin.endsWith("_dyemask"))
                .sorted()
                .toList();
    }

    private CompanionSkins() {}
}
