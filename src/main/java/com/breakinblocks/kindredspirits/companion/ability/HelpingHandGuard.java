package com.breakinblocks.kindredspirits.companion.ability;

import com.breakinblocks.kindredspirits.KindredSpirits;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jspecify.annotations.Nullable;

public final class HelpingHandGuard {
    private static WeakReference<RecipeManager> graphSource = new WeakReference<>(null);
    private static Map<Item, Set<Item>> graph = Map.of();
    private static final Map<Item, Set<Item>> reachableCache = new HashMap<>();
    private static int cachedDepth = -1;

    public static boolean mayDuplicate(ServerLevel level, ItemStack crafted, Container grid) {
        Identifier recipeId = grid instanceof CraftingContainer crafting
                ? level.recipeAccess()
                        .getRecipeFor(RecipeType.CRAFTING, crafting.asCraftInput(), level)
                        .map(holder -> holder.id().identifier())
                        .orElse(null)
                : null;

        List<Item> inputs = new ArrayList<>();
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack stack = grid.getItem(slot);
            if (!stack.isEmpty()) {
                inputs.add(stack.getItem());
            }
        }

        return allows(
                level,
                recipeId,
                crafted.getItem(),
                inputs,
                KindredConfig.COMMON.helpingHandRecipeBlacklist.get(),
                KindredConfig.COMMON.helpingHandReverseDepth.get());
    }

    public static boolean allows(
            ServerLevel level,
            @Nullable Identifier recipeId,
            Item crafted,
            Collection<Item> inputs,
            List<? extends String> blacklist,
            int depth) {
        if (inputs.isEmpty() || inputs.contains(crafted)) {
            return false;
        }

        if (recipeId != null && isBlacklisted(recipeId, blacklist)) {
            return false;
        }

        return Collections.disjoint(reachableFrom(level, crafted, depth), Set.copyOf(inputs));
    }

    public static boolean isBlacklisted(Identifier recipeId, List<? extends String> blacklist) {
        String id = recipeId.toString();
        for (String entry : blacklist) {
            String pattern = entry.trim();
            if (pattern.endsWith("*")
                    ? id.startsWith(pattern.substring(0, pattern.length() - 1))
                    : id.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Item> reachableFrom(ServerLevel level, Item start, int depth) {
        RecipeManager manager = level.getServer().getRecipeManager();
        if (graphSource.get() != manager) {
            graph = buildGraph(level, manager);
            graphSource = new WeakReference<>(manager);
            reachableCache.clear();
        }
        if (cachedDepth != depth) {
            reachableCache.clear();
            cachedDepth = depth;
        }

        return reachableCache.computeIfAbsent(start, item -> walk(item, depth));
    }

    private static Set<Item> walk(Item start, int depth) {
        Set<Item> seen = new HashSet<>();
        List<Item> frontier = List.of(start);
        for (int step = 0; step < depth && !frontier.isEmpty(); step++) {
            List<Item> next = new ArrayList<>();
            for (Item item : frontier) {
                for (Item output : graph.getOrDefault(item, Set.of())) {
                    if (seen.add(output)) {
                        next.add(output);
                    }
                }
            }
            frontier = next;
        }
        return seen;
    }

    private static Map<Item, Set<Item>> buildGraph(ServerLevel level, RecipeManager manager) {
        ContextMap context = SlotDisplayContext.fromLevel(level);
        Map<Item, Set<Item>> edges = new HashMap<>();
        int skipped = 0;

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            try {
                Recipe<?> recipe = holder.value();
                Set<Item> outputs = new HashSet<>();
                for (RecipeDisplay display : recipe.display()) {
                    for (ItemStack stack : display.result().resolveForStacks(context)) {
                        if (!stack.isEmpty()) {
                            outputs.add(stack.getItem());
                        }
                    }
                }
                if (outputs.isEmpty()) {
                    continue;
                }

                for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
                    ingredient
                            .items()
                            .forEach(input -> edges.computeIfAbsent(input.value(), item -> new HashSet<>())
                                    .addAll(outputs));
                }
            } catch (RuntimeException e) {
                skipped++;
            }
        }

        KindredSpirits.LOGGER.debug(
                "Helping Hand recipe graph: {} input items from {} recipes ({} skipped)",
                edges.size(),
                manager.getRecipes().size(),
                skipped);
        return edges;
    }

    private HelpingHandGuard() {}
}
