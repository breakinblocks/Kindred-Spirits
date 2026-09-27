package com.breakinblocks.kindredspirits.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CompanionLights {
    public static final int NO_EXPIRY = -1;

    private record Entry(BlockPos pos, Block block, long placedAt) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(Entry::block),
                        Codec.LONG.fieldOf("placed_at").forGetter(Entry::placedAt))
                .apply(instance, Entry::new));
    }

    public static final Codec<CompanionLights> CODEC = Codec.unboundedMap(Identifier.CODEC, Entry.CODEC.listOf())
            .xmap(CompanionLights::new, lights -> lights.entries);

    private final Map<Identifier, List<Entry>> entries;

    public CompanionLights() {
        this(Map.of());
    }

    private CompanionLights(Map<Identifier, List<Entry>> entries) {
        this.entries = new HashMap<>();
        entries.forEach((key, list) -> this.entries.put(key, new ArrayList<>(list)));
    }

    public int count(Identifier key) {
        return this.entries.getOrDefault(key, List.of()).size();
    }

    public boolean holds(Identifier key, BlockPos pos) {
        return this.entries.getOrDefault(key, List.of()).stream()
                .anyMatch(entry -> entry.pos().equals(pos));
    }

    public boolean place(ServerLevel level, Identifier key, BlockPos pos, BlockState state, int max) {
        if (max <= 0 || !level.isLoaded(pos) || !level.getBlockState(pos).isAir() || this.holds(key, pos)) {
            return false;
        }

        List<Entry> list = this.entries.computeIfAbsent(key, ignored -> new ArrayList<>());
        while (list.size() >= max && !list.isEmpty()) {
            this.remove(level, list.removeFirst());
        }

        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }

        list.add(new Entry(pos.immutable(), state.getBlock(), level.getGameTime()));
        return true;
    }

    public void sweep(ServerLevel level, Identifier key, Vec3 origin, int expiryTicks, double maxDistance) {
        List<Entry> list = this.entries.get(key);
        if (list == null) {
            return;
        }

        long now = level.getGameTime();
        double maxDistanceSqr = maxDistance * maxDistance;

        for (Iterator<Entry> iterator = list.iterator(); iterator.hasNext(); ) {
            Entry entry = iterator.next();
            boolean expired = expiryTicks != NO_EXPIRY && now - entry.placedAt() >= expiryTicks;
            boolean far = entry.pos().getCenter().distanceToSqr(origin) > maxDistanceSqr;
            boolean gone = level.isLoaded(entry.pos())
                    && !level.getBlockState(entry.pos()).is(entry.block());

            if (expired || far || gone) {
                iterator.remove();
                this.remove(level, entry);
            }
        }
    }

    public void clear(ServerLevel level, Identifier key) {
        List<Entry> list = this.entries.remove(key);
        if (list != null) {
            list.forEach(entry -> this.remove(level, entry));
        }
    }

    public void clear(ServerLevel level) {
        this.entries.values().forEach(list -> list.forEach(entry -> this.remove(level, entry)));
        this.entries.clear();
    }

    public boolean isEmpty() {
        return this.entries.values().stream().allMatch(List::isEmpty);
    }

    private void remove(ServerLevel level, Entry entry) {
        CompanionWorldData.removeLight(level, entry.pos(), entry.block());
    }
}
