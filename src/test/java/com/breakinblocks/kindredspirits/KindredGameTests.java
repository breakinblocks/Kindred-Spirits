package com.breakinblocks.kindredspirits;

import com.breakinblocks.kindredspirits.block.TrexEggBlock;
import com.breakinblocks.kindredspirits.companion.*;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.net.KindredNetworking.CharmActionPayload.Action;
import com.breakinblocks.kindredspirits.registry.*;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Server integration regressions. These classes and fixtures are excluded from the release jar. */
@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class KindredGameTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("archaeology_egg_is_brushable", KindredGameTests::archaeology);
        TESTS.put("stored_health_and_cooldowns", KindredGameTests::storage);
        TESTS.put("unreachable_equipment_is_not_duplicated", KindredGameTests::unreachable);
        TESTS.put("revival_heals_and_retains_equipment", KindredGameTests::revival);
        TESTS.put("offline_death_reconciles", KindredGameTests::offlineDeath);
        TESTS.put("bond_lookup_skips_unbonded_pets", KindredGameTests::bondLookup);
        TESTS.put("owner_range_rejects_other_dimensions", KindredGameTests::ownerDimension);
        TESTS.put("recall_across_dimensions", KindredGameTests::recall);
        TESTS.put("mirror_strike_deals_echo_damage", KindredGameTests::mirrorStrike);
        TESTS.put("disabled_abilities_stop_combat", KindredGameTests::disabledAbilities);
        TESTS.put("bond_curve_extreme_config", KindredGameTests::bondCurve);
        TESTS.put("fractional_xp_survives_save", KindredGameTests::fractionalXp);
        TESTS.put("snapshot_network_and_save_round_trip", KindredGameTests::snapshotRoundTrip);
        TESTS.put("entity_save_retains_bond_and_cooldown", KindredGameTests::entitySave);
        TESTS.put("stored_chestplate_stats_match_live", KindredGameTests::chestplateStats);
        TESTS.put("tooltip_uses_server_progression", KindredGameTests::tooltip);
        TESTS.put("light_cleanup_preserves_replacement_blocks", KindredGameTests::lights);
        TESTS.put("unloaded_light_removals_are_persistent", KindredGameTests::deferredLights);
        TESTS.put("death_animation_lifetime", KindredGameTests::deathAnimation);
        TESTS.put("egg_hatches_through_three_stages", KindredGameTests::eggHatching);
        TESTS.put("xp_events_accumulate_small_gains", KindredGameTests::xpEvents);
        TESTS.put("feeding_cooldown_survives_storage", KindredGameTests::feeding);
        TESTS.put("transformation_preserves_source_if_spawn_canceled", KindredGameTests::canceledTransform);
        TESTS.put("invalid_ability_name_is_ignored", KindredGameTests::invalidAbility);
        TESTS.put("light_cleanup_on_dimension_transfer", KindredGameTests::lightTransfer);
        TESTS.put("light_cleanup_on_chunk_unload", KindredGameTests::lightUnload);
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) ->
                registry.register(KindredSpirits.id(name), test)));
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(KindredSpirits.id("regressions"));
        TESTS.forEach((name, test) -> event.registerTest(KindredSpirits.id(name),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, KindredSpirits.id(name)),
                        new TestData<>(environment, KindredSpirits.id("empty"), 120, 2, true))));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
        Vec3 pos = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static CompanionEntity pet(GameTestHelper h, CompanionSpecies species, ServerPlayer player, boolean bonded) {
        CompanionEntity pet = h.spawnWithNoFreeWill(KindredEntities.type(species), new BlockPos(4, 1, 4));
        pet.tame(player);
        pet.setCommand(CompanionCommand.STAY);
        if (bonded) {
            pet.setBonded(true);
            KindredAttachments.modifyBond(player, b -> KindredCharmItem.snapshot(b, pet).withStored(false));
        }
        return pet;
    }

    private static void finish(GameTestHelper h, ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
        h.succeed();
    }

    private static void action(ServerPlayer player, Action action) {
        KindredCharmItem.handleAction(player, action, 0, "");
    }

    private static CompanionEntity deployed(ServerPlayer player) {
        UUID uuid = KindredAttachments.bond(player).companion().orElseThrow();
        return (CompanionEntity) player.level().getEntity(uuid);
    }

    private static void archaeology(GameTestHelper h) {
        ServerPlayer player = player(h);
        for (String path : List.of("archaeology/desert_well", "archaeology/desert_pyramid")) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
            LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
            LootParams params = new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.BRUSH))
                    .create(LootContextParamSets.ARCHAEOLOGY);
            long eggSeed = -1;
            int eggs = 0;
            for (long seed = 1; seed <= 256; seed++) {
                var loot = table.getRandomItems(params, seed);
                h.assertTrue(loot.size() == 1, "Archaeology must yield exactly one brushable result");
                if (loot.getFirst().is(KindredItems.TREX_EGG.get())) { eggs++; eggSeed = seed; }
            }
            h.assertTrue(eggs > 0 && eggs < 100, "Egg replacement must occur alongside ordinary archaeology loot");
            BlockPos pos = h.absolutePos(new BlockPos(7, 1, 7));
            h.getLevel().setBlockAndUpdate(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState());
            BrushableBlockEntity block = (BrushableBlockEntity) h.getLevel().getBlockEntity(pos);
            block.setLootTable(key, eggSeed);
            for (int i = 0; i < 10; i++) block.brush(h.getLevel().getGameTime() + i * 10L,
                    h.getLevel(), player, Direction.UP, new ItemStack(Items.BRUSH));
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2), item -> item.getItem().is(KindredItems.TREX_EGG.get())).size() > 0,
                    "Brushing the selected loot seed must drop a T-Rex egg");
            h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).forEach(Entity::discard);
        }
        finish(h, player);
    }

    private static void storage(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.BABY_DRAGON, player, true);
        pet.setHealth(3);
        pet.setAbilityCooldown(CompanionAbilities.KILN_BREATH.id(), 6000);
        action(player, Action.DISMISS);
        h.assertTrue(pet.isRemoved() && KindredAttachments.bond(player).stored(), "Dismiss must store and remove the live entity");
        action(player, Action.SUMMON);
        CompanionEntity summoned = deployed(player);
        h.assertTrue(summoned != null && summoned.getHealth() == 3, "Summoning a resting companion must not heal it");
        h.assertTrue(summoned.abilityCooldownTicks(CompanionAbilities.KILN_BREATH.id()) == 6000, "Summoning must not reset Kiln Breath");
        finish(h, player);
    }

    private static void unreachable(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pet.setEquipment(new ItemStack(Items.DIAMOND_CHESTPLATE));
        KindredAttachments.modifyBond(player, b -> KindredCharmItem.snapshot(b, pet));
        CompanionBond before = KindredAttachments.bond(player);
        pet.discard(); // Models a deployed entity unavailable to the server lookup.
        for (Action action : List.of(Action.UNEQUIP, Action.RELEASE, Action.PRESTIGE, Action.SET_NAME, Action.SUMMON)) action(player, action);
        h.assertTrue(before.equals(KindredAttachments.bond(player)), "Unavailable deployed snapshots must remain read-only");
        h.assertTrue(player.getInventory().countItem(Items.DIAMOND_CHESTPLATE) == 0, "Unreachable equipment must not be returned");
        h.assertTrue(!KindredCharmItem.releaseFully(player), "Command release must use the same unreachable guard");
        finish(h, player);
    }

    private static void revival(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pet.setEquipment(new ItemStack(Items.IRON_CHESTPLATE));
        pet.hurtServer(h.getLevel(), pet.damageSources().genericKill(), 10000);
        CompanionBond recovering = KindredAttachments.bond(player);
        h.assertTrue(recovering.stored() && recovering.reviveReadyAt() > 0, "Death must enter recovery");
        action(player, Action.SUMMON);
        h.assertTrue(KindredAttachments.bond(player).stored(), "Recovery cannot be bypassed by summoning early");
        KindredAttachments.modifyBond(player, b -> b.withReviveReadyAt(Math.max(1, h.getLevel().getGameTime())));
        action(player, Action.SUMMON);
        CompanionEntity revived = deployed(player);
        h.assertTrue(revived != null && revived.isAlive() && revived.getHealth() == revived.getMaxHealth(), "Revival must restore full health");
        h.assertTrue(revived.equipment().is(Items.IRON_CHESTPLATE), "Revival must keep companion equipment");
        finish(h, player);
    }

    private static void offlineDeath(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        UUID offline = UUID.randomUUID();
        pet.setOwnerReference(EntityReference.of(offline));
        pet.refreshBondState();
        h.assertTrue(pet.isBonded(), "An unresolved owner must not erase the bond");
        pet.hurtServer(h.getLevel(), pet.damageSources().genericKill(), 10000);
        var storage = h.getLevel().getServer().overworld();
        var death = storage.getData(CompanionWorldData.DEATHS).stream().filter(d -> d.companion().equals(pet.getUUID())).findFirst().orElseThrow();
        h.assertTrue(death.owner().equals(offline), "Offline death must be persisted with owner UUID");
        // Reassociate this fixture's queued owner with its connected test player.
        storage.setData(CompanionWorldData.DEATHS, storage.getData(CompanionWorldData.DEATHS).stream()
                .map(d -> d == death ? new CompanionWorldData.Death(player.getUUID(), d.companion(), d.snapshot(), d.readyAt()) : d).toList());
        CompanionWorldData.reconcile(player);
        h.assertTrue(KindredAttachments.bond(player).stored() && KindredAttachments.bond(player).reviveReadyAt() == death.readyAt(), "Owner login must recover the death record");
        h.assertTrue(storage.getData(CompanionWorldData.DEATHS).stream().noneMatch(d -> d.companion().equals(pet.getUUID())), "Consumed death records must be removed");
        finish(h, player);
    }

    private static void bondLookup(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity bonded = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        CompanionEntity unbonded = pet(h, CompanionSpecies.NIGHTFOX, player, false);
        unbonded.setPos(player.position());
        h.assertTrue(CompanionEntity.bondedNear(player, 16).orElse(null) == bonded, "A closer unbonded pet must not mask the active bond");
        finish(h, player);
    }

    private static void ownerDimension(GameTestHelper h) {
        ServerPlayer player = player(h);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        CompanionEntity pet = KindredEntities.type(CompanionSpecies.NIGHTFOX).create(other, EntitySpawnReason.COMMAND);
        pet.tame(player);
        pet.setPos(player.position());
        h.assertTrue(!pet.ownerWithin(player, 100), "Matching coordinates in different dimensions are not nearby");
        finish(h, player);
    }

    private static void recall(GameTestHelper h) {
        ServerPlayer player = player(h);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        CompanionEntity pet = KindredEntities.type(CompanionSpecies.MINI_PLAYER).create(other, EntitySpawnReason.COMMAND);
        pet.tame(player);
        pet.setBonded(true);
        pet.setPos(0, 100, 0);
        // Generate the destination fixture before the tick-budgeted asynchronous lookup.
        other.getChunk(0, 0);
        other.addFreshEntity(pet);
        KindredAttachments.modifyBond(player, b -> KindredCharmItem.snapshot(b, pet).withStored(false));
        // The first request schedules the source chunk; entity loading completes on later ticks.
        h.succeedWhen(() -> {
            action(player, Action.RECALL);
            CompanionEntity arrived = deployed(player);
            h.assertTrue(arrived != null && arrived != pet && pet.isRemoved(),
                    "Cross-dimension recall must load and replace the source entity (destination=" + arrived + ", source removed=" + pet.isRemoved() + ")");
            h.assertTrue(KindredAttachments.bond(player).lastDimension().orElseThrow().equals(player.level().dimension().identifier()), "Recall must save the destination dimension");
            player.level().getServer().getPlayerList().remove(player);
        });
    }

    private static void mirrorStrike(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        LivingEntity target = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 4));
        float before = target.getHealth();
        target.hurtServer(h.getLevel(), player.damageSources().playerAttack(player), 4);
        h.assertTrue(Math.abs(target.getHealth() - (before - 5)) < 0.001, "Mirror Strike must add 25% through the triggering hit's invulnerability frames");
        h.assertTrue(pet.abilityCooldownTicks(CompanionAbilities.MIRROR_STRIKE.id()) == 20, "Echo must start its cooldown without recursion");
        finish(h, player);
    }

    private static void disabledAbilities(GameTestHelper h) {
        boolean previous = KindredConfig.COMMON.abilitiesEnabled.get();
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.TREX, player, true);
        LivingEntity target = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 4));
        try {
            KindredConfig.COMMON.abilitiesEnabled.set(false);
            float health = target.getHealth();
            h.assertTrue(!pet.doHurtTarget(h.getLevel(), target) && target.getHealth() == health, "Cosmetic mode must prevent companion melee damage");
            h.assertTrue(!pet.useActiveAbility(player), "Cosmetic mode must prevent active abilities");
        } finally { KindredConfig.COMMON.abilitiesEnabled.set(previous); }
        finish(h, player);
    }

    private static void bondCurve(GameTestHelper h) {
        double growth = KindredConfig.COMMON.bondGrowth.get();
        int base = KindredConfig.COMMON.bondBaseCost.get();
        int max = KindredConfig.COMMON.bondMaxLevel.get();
        try {
            KindredConfig.COMMON.bondGrowth.set(2.0);
            KindredConfig.COMMON.bondBaseCost.set(760);
            KindredConfig.COMMON.bondMaxLevel.set(30);
            int previous = 0;
            for (int level = 1; level <= 30; level++) {
                int points = CompanionBondMath.pointsAtLevelStart(level);
                h.assertTrue(points > previous && CompanionBondMath.levelFromPoints(points) == level, "Extreme configured costs must stay positive and monotonic");
                previous = points;
            }
            h.assertTrue(CompanionBondMath.clampPoints(Integer.MAX_VALUE) == previous, "Clamping must remain valid after curve saturation");
            h.assertTrue(CompanionBondMath.migrateLegacy(100) == previous, "Legacy migration must not overflow at maximum bond");
        } finally {
            KindredConfig.COMMON.bondGrowth.set(growth);
            KindredConfig.COMMON.bondBaseCost.set(base);
            KindredConfig.COMMON.bondMaxLevel.set(max);
        }
        h.succeed();
    }

    private static void fractionalXp(GameTestHelper h) {
        CompanionProgress progress = new CompanionProgress();
        h.assertTrue(progress.shareExperience(1, 0.5) == 0, "Half an XP should be retained as a fraction");
        var json = CompanionProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow();
        CompanionProgress loaded = CompanionProgress.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        h.assertTrue(loaded.shareExperience(1, 0.5) == 1, "Two 1-XP gains must yield one companion XP even across saving");
        h.succeed();
    }

    private static void snapshotRoundTrip(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pet.setEquipment(new ItemStack(Items.DIAMOND_CHESTPLATE));
        pet.setHealth(5);
        pet.setAbilityCooldown(CompanionAbilities.MIRROR_STRIKE.id(), 20);
        CompanionSnapshot snapshot = CompanionSnapshot.of(pet);
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json = CompanionSnapshot.CODEC.encodeStart(ops, snapshot).getOrThrow();
        CompanionSnapshot saved = CompanionSnapshot.CODEC.parse(ops, json).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            CompanionSnapshot.STREAM_CODEC.encode(buffer, saved);
            CompanionSnapshot decoded = CompanionSnapshot.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.state().equals(snapshot.state()) && ItemStack.matches(decoded.equipment(), snapshot.equipment()), "Save and network codecs must preserve health, cooldowns, and equipment");
            h.assertTrue(buffer.readableBytes() == 0, "Snapshot codec must consume the whole payload");
        } finally { buffer.release(); }
        finish(h, player);
    }

    private static void entitySave(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        pet.setAbilityCooldown(CompanionAbilities.SHADOW_BALL.id(), 200);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        pet.save(output);
        CompanionEntity loaded = KindredEntities.type(pet.species()).create(h.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(loaded.isBonded() && loaded.abilityCooldownTicks(CompanionAbilities.SHADOW_BALL.id()) == 200, "Entity NBT must retain deployed bond status and cooldowns");
        finish(h, player);
    }

    private static void chestplateStats(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pet.setEquipment(new ItemStack(Items.DIAMOND_CHESTPLATE));
        // Vanilla equipment modifiers are applied on the next living-entity tick.
        h.runAfterDelay(2, () -> {
            CompanionStats stored = CompanionStats.of(pet.species(), CompanionSnapshot.of(pet));
            h.assertTrue(Math.abs(stored.modified().get(2) - pet.getAttributeValue(Attributes.ARMOR)) < 0.001,
                    "Resting stats must include vanilla chestplate armor");
            finish(h, player);
        });
    }

    private static void tooltip(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        pet.setBondPoints(CompanionBondMath.pointsAtLevelStart(5));
        KindredAttachments.modifyBond(player, b -> KindredCharmItem.snapshot(b, pet));
        var progress = player.getData(KindredAttachments.TOOLTIP_PROGRESS);
        h.assertTrue(progress.bondLevel() == 5 && progress.bondMaxLevel() == CompanionBondMath.maxLevel()
                && progress.experienceToNext() == pet.experienceToNextLevel(), "Tooltip attachment must carry authoritative progression values");
        finish(h, player);
    }

    private static void lights(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        BlockPos first = h.absolutePos(new BlockPos(6, 1, 6));
        BlockPos second = first.east();
        var key = CompanionAbilities.WISPLIGHT.id();
        var state = KindredBlocks.WISP_LIGHT.get().defaultBlockState();
        pet.lights().place(h.getLevel(), key, first, state, 2);
        pet.lights().place(h.getLevel(), key, second, state, 2);
        h.getLevel().setBlockAndUpdate(first, Blocks.STONE.defaultBlockState());
        pet.setBonded(false);
        h.assertTrue(h.getLevel().getBlockState(first).is(Blocks.STONE) && h.getLevel().getBlockState(second).isAir(), "Release must clean owned lights and preserve replacement blocks");
        h.assertTrue(pet.lights().isEmpty(), "Release must clear tracked lights");
        finish(h, player);
    }

    private static void deferredLights(GameTestHelper h) {
        BlockPos far = new BlockPos(20_000_000, 80, 20_000_000);
        var level = h.getLevel();
        h.assertTrue(!level.isLoaded(far), "Fixture must start outside loaded chunks");
        CompanionWorldData.removeLight(level, far, KindredBlocks.WISP_LIGHT.get());
        CompanionWorldData.cleanLoadedLights(level);
        var pending = level.getData(CompanionWorldData.LIGHT_REMOVALS);
        var entry = pending.stream().filter(e -> e.pos().equals(far)).findFirst().orElseThrow();
        var json = CompanionWorldData.LightRemoval.CODEC.encodeStart(JsonOps.INSTANCE, entry).getOrThrow();
        h.assertTrue(CompanionWorldData.LightRemoval.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(entry), "Unloaded removals must survive serialization");
        h.assertTrue(!level.isLoaded(far), "Cleanup must not load distant chunks");
        BlockPos local = h.absolutePos(new BlockPos(7, 1, 7));
        level.setBlockAndUpdate(local, KindredBlocks.WISP_LIGHT.get().defaultBlockState());
        level.setData(CompanionWorldData.LIGHT_REMOVALS, List.of(new CompanionWorldData.LightRemoval(local, entry.block())));
        CompanionWorldData.cleanLoadedLights(level);
        h.assertTrue(level.getBlockState(local).isAir() && level.getData(CompanionWorldData.LIGHT_REMOVALS).isEmpty(), "Queued cleanup must finish once its chunk is available");
        h.succeed();
    }

    private static void deathAnimation(GameTestHelper h) {
        CompanionEntity trex = h.spawnWithNoFreeWill(KindredEntities.type(CompanionSpecies.TREX), new BlockPos(4, 1, 4));
        CompanionEntity gremlin = h.spawnWithNoFreeWill(KindredEntities.type(CompanionSpecies.GREMLIN), new BlockPos(8, 1, 8));
        trex.hurtServer(h.getLevel(), trex.damageSources().genericKill(), 10000);
        gremlin.hurtServer(h.getLevel(), gremlin.damageSources().genericKill(), 10000);
        h.runAfterDelay(21, () -> h.assertTrue(!trex.isRemoved() && !gremlin.isRemoved(), "Long death animations must outlive vanilla's 20 ticks"));
        h.runAfterDelay(35, () -> h.assertTrue(!trex.isRemoved() && gremlin.isRemoved(), "Gremlin should finish at 30 ticks"));
        h.runAfterDelay(65, () -> {
            h.assertTrue(trex.isRemoved(), "T-Rex should finish at 58 ticks");
            h.succeed();
        });
    }

    private static void eggHatching(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(7, 1, 7));
        h.getLevel().setBlockAndUpdate(pos, KindredBlocks.TREX_EGG.get().defaultBlockState());
        for (int stage = 0; stage < 3; stage++) {
            var state = h.getLevel().getBlockState(pos);
            h.assertTrue(state.getValue(TrexEggBlock.HATCH) == stage, "Egg stages must advance in order");
            state.tick(h.getLevel(), pos, h.getLevel().getRandom());
        }
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Hatched egg must be removed");
        h.assertTrue(h.getLevel().getEntitiesOfClass(CompanionEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2),
                pet -> pet.species() == CompanionSpecies.TREX).size() == 1, "Final stage must spawn exactly one T-Rex");
        h.succeed();
    }

    private static void xpEvents(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        double previous = KindredConfig.COMMON.xpShare.get();
        try {
            KindredConfig.COMMON.xpShare.set(0.5);
            player.giveExperiencePoints(1);
            h.assertTrue(pet.getExperience() == 0, "The first half-point should remain fractional");
            player.giveExperiencePoints(1);
            h.assertTrue(pet.getExperience() == 1, "Actual player XP events must accumulate small shares");
        } finally { KindredConfig.COMMON.xpShare.set(previous); }
        finish(h, player);
    }

    private static void feeding(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        var ingredient = h.getLevel().registryAccess().lookupOrThrow(Registries.ITEM)
                .getOrThrow(pet.species().tamingTag()).iterator().next().value();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ingredient, 3));
        pet.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        int points = pet.getBondPoints();
        h.assertTrue(points > 0 && !pet.progress().canFeed(h.getLevel().getGameTime()), "Feeding must grant bond and start its cooldown");
        action(player, Action.DISMISS);
        action(player, Action.SUMMON);
        CompanionEntity summoned = deployed(player);
        summoned.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(summoned.getBondPoints() == points, "Storing and summoning must not bypass feeding cooldown");
        finish(h, player);
    }

    private static void canceledTransform(GameTestHelper h) {
        var fox = h.spawnWithNoFreeWill(EntityType.FOX, new BlockPos(4, 1, 4));
        Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel = event -> {
            if (event.getEntity() instanceof CompanionEntity pet && pet.species() == CompanionSpecies.NIGHTFOX
                    && pet.position().distanceToSqr(fox.position()) < 0.01) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var result = CompanionSpawns.transform(fox, CompanionSpecies.NIGHTFOX, net.minecraft.core.particles.ParticleTypes.SCULK_SOUL);
            h.assertTrue(result == null && !fox.isRemoved(), "A canceled companion spawn must retain the original mob");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
        h.succeed();
    }

    private static void invalidAbility(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        KindredCharmItem.handleAction(player, Action.TOGGLE_ABILITY, 0, "INVALID / identifier");
        h.assertTrue(pet.disabledAbilityNames().isEmpty(), "Malformed client ability identifiers must be ignored");
        finish(h, player);
    }

    private static void lightTransfer(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        BlockPos pos = h.absolutePos(new BlockPos(7, 1, 7));
        pet.lights().place(h.getLevel(), CompanionAbilities.WISPLIGHT.id(), pos,
                KindredBlocks.WISP_LIGHT.get().defaultBlockState(), 1);
        var arrived = pet.teleport(new net.minecraft.world.level.portal.TeleportTransition(
                h.getLevel().getServer().getLevel(Level.NETHER), new Vec3(0, 100, 0), Vec3.ZERO,
                0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        h.assertTrue(arrived != null && h.getLevel().getBlockState(pos).isAir(), "Dimension travel must clear lights in the source world");
        h.assertTrue(((CompanionEntity) arrived).lights().isEmpty(), "Destination must not inherit source-world light coordinates");
        arrived.discard();
        finish(h, player);
    }

    private static void lightUnload(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        BlockPos pos = h.absolutePos(new BlockPos(7, 1, 7));
        pet.lights().place(h.getLevel(), CompanionAbilities.WISPLIGHT.id(), pos,
                KindredBlocks.WISP_LIGHT.get().defaultBlockState(), 1);
        // The entity-section manager calls setRemoved directly, bypassing Entity.remove.
        pet.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir() && pet.lights().isEmpty(),
                "Chunk unloading must invoke light cleanup through the removal callback");
        finish(h, player);
    }

    private KindredGameTests() { }
}

