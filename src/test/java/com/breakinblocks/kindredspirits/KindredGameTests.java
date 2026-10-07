package com.breakinblocks.kindredspirits;

import com.breakinblocks.kindredspirits.block.TrexEggBlock;
import com.breakinblocks.kindredspirits.companion.*;
import com.breakinblocks.kindredspirits.companion.ability.CompanionAbilities;
import com.breakinblocks.kindredspirits.companion.ability.HelpingHandGuard;
import com.breakinblocks.kindredspirits.companion.ability.OreCrushing;
import com.breakinblocks.kindredspirits.config.KindredConfig;
import com.breakinblocks.kindredspirits.item.KindredCharmItem;
import com.breakinblocks.kindredspirits.net.KindredNetworking.CharmActionPayload.Action;
import com.breakinblocks.kindredspirits.registry.*;
import com.breakinblocks.kindredspirits.registry.KindredAttachments.CompanionBond;
import com.breakinblocks.kindredspirits.worldgen.BeachSuspiciousSandFeature;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Server integration regressions. These classes and fixtures are excluded from the release jar. */
@EventBusSubscriber(modid = KindredSpirits.MOD_ID)
public final class KindredGameTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("custom_sounds_resolve_and_serialize", KindredGameTests::soundPackets);
        TESTS.put("particle_types_round_trip", KindredGameTests::particleCodecs);
        TESTS.put("dragon_cloud_uses_custom_visual_particles", KindredGameTests::dragonParticles);
        TESTS.put("quokka_taming_and_equipping", KindredGameTests::quokkaTame);
        TESTS.put("quokka_growth_ticks_and_age_lock", KindredGameTests::quokkaAgeTicks);
        TESTS.put("quokka_buffs_and_unlocks", KindredGameTests::quokkaBuffs);
        TESTS.put("bond_speeds_up_only_the_base_buff", KindredGameTests::bondRateScope);
        TESTS.put("dye_recolours_persists_and_washes_off", KindredGameTests::dyeRecolour);
        TESTS.put("spirit_bandage_heals_less_than_golden_apple", KindredGameTests::spiritBandage);
        TESTS.put("quokka_snack_cooldown_and_targeting", KindredGameTests::quokkaSnack);
        TESTS.put("quokka_charm_expires_and_protects", KindredGameTests::quokkaCharm);
        TESTS.put("quokka_charm_survives_save", KindredGameTests::quokkaCharmSave);
        TESTS.put("quokka_breeding_limits", KindredGameTests::quokkaBreeding);
        TESTS.put("quokka_baby_growth_does_not_stack", KindredGameTests::quokkaGrowth);
        TESTS.put("quokka_decoy_cannot_tame_and_expires", KindredGameTests::quokkaDecoy);
        TESTS.put("quokka_hurt_throws_once_and_flees", KindredGameTests::quokkaFlee);
        TESTS.put("quokka_jungle_spawns_and_gear", KindredGameTests::quokkaSpawns);
        TESTS.put("direwolf_golden_bone_transforms_wolves", KindredGameTests::direwolfTransform);
        TESTS.put("direwolf_digs_up_ground_loot", KindredGameTests::direwolfDig);
        TESTS.put("direwolf_wolf_armour_and_best_friend", KindredGameTests::direwolfArmour);
        TESTS.put("trex_gloves_sync_to_the_client", KindredGameTests::trexGloves);
        TESTS.put("dragon_tablet_syncs_to_the_client", KindredGameTests::dragonTablet);
        TESTS.put("meteor_call_drops_a_meteor_that_hits", KindredGameTests::meteorCall);
        TESTS.put("crushing_might_crushes_ore_items", KindredGameTests::crushingMight);
        TESTS.put("mini_player_fires_spirit_arrows", KindredGameTests::spiritArrow);
        TESTS.put("helping_hand_skips_reversible_recipes", KindredGameTests::helpingHandGuard);
        TESTS.put("skin_choice_is_saved_and_validated", KindredGameTests::skinChoice);
        TESTS.put("advancements_follow_companion_progress", KindredGameTests::advancements);
        TESTS.put("archaeology_egg_is_brushable", KindredGameTests::archaeology);
        TESTS.put("beach_suspicious_sand_generates_and_can_be_disabled", KindredGameTests::beachSuspiciousSand);
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
        TESTS.put("only_picked_up_orbs_share_xp", KindredGameTests::returnedXp);
        TESTS.put("feeding_cooldown_survives_storage", KindredGameTests::feeding);
        TESTS.put("transformation_preserves_source_if_spawn_canceled", KindredGameTests::canceledTransform);
        TESTS.put("invalid_ability_name_is_ignored", KindredGameTests::invalidAbility);
        TESTS.put("light_cleanup_on_dimension_transfer", KindredGameTests::lightTransfer);
        TESTS.put("light_cleanup_on_chunk_unload", KindredGameTests::lightUnload);
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(
                Registries.TEST_FUNCTION,
                registry -> TESTS.forEach((name, test) -> registry.register(KindredSpirits.id(name), test)));
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(KindredSpirits.id("regressions"));
        TESTS.forEach((name, test) -> event.registerTest(
                KindredSpirits.id(name),
                new FunctionGameTestInstance(
                        ResourceKey.create(Registries.TEST_FUNCTION, KindredSpirits.id(name)),
                        new TestData<>(
                                environment,
                                KindredSpirits.id("empty"),
                                name.equals("quokka_hurt_throws_once_and_flees") ? 300 : 120,
                                2,
                                true,
                                Rotation.NONE,
                                false,
                                1,
                                1,
                                name.equals("meteor_call_drops_a_meteor_that_hits"),
                                0))));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h) {
        var level = h.getLevel();
        CommonListenerCookie cookie =
                CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player =
                new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
                    @Override
                    public GameType gameMode() {
                        return GameType.CREATIVE;
                    }
                };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        Vec3 pos = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    private static CompanionEntity pet(
            GameTestHelper h, CompanionSpecies species, ServerPlayer player, boolean bonded) {
        CompanionEntity pet = h.spawnWithNoFreeWill(KindredEntities.type(species), new BlockPos(4, 1, 4));
        pet.tame(player);
        pet.setCommand(CompanionCommand.STAY);
        if (bonded) {
            pet.setBonded(true);
            KindredAttachments.modifyBond(
                    player, b -> KindredCharmItem.snapshot(b, pet).withStored(false));
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

    private static void advancements(GameTestHelper h) {
        ServerPlayer player = player(h);
        var server = h.getLevel().getServer();
        Function<String, net.minecraft.advancements.AdvancementHolder> find =
                name -> server.getAdvancements().get(KindredSpirits.id(name));
        for (String name : List.of(
                "root",
                "tame_companion",
                "tame_all",
                "craft_charm",
                "bond_companion",
                "bond_level_5",
                "bond_level_15",
                "bond_level_max",
                "level_10",
                "level_max",
                "prestige",
                "stars_max",
                "equip",
                "dye",
                "open_storage",
                "revive")) {
            h.assertTrue(find.apply(name) != null, "Advancement " + name + " must load");
        }
        for (CompanionSpecies species : CompanionSpecies.values()) {
            String name = species.getSerializedName();
            h.assertTrue(find.apply("tame_" + name) != null, "Every species needs a tame advancement (" + name + ")");
            h.assertTrue(
                    find.apply("tame_all").value().criteria().containsKey(name), "Spirit Menagerie must list " + name);
            h.assertTrue(
                    find.apply("tame_companion").value().criteria().containsKey(name),
                    "New Friend must accept " + name);
        }
        java.util.function.Predicate<String> done = name ->
                player.getAdvancements().getOrStartProgress(find.apply(name)).isDone();

        CompanionEntity pet = pet(h, CompanionSpecies.DIREWOLF, player, true);
        h.assertTrue(done.test("tame_direwolf") && done.test("tame_companion"), "Taming a Direwolf must count");
        h.assertTrue(
                !done.test("tame_nightfox") && !done.test("tame_all"),
                "Taming one species must not count for the others");
        h.assertTrue(!done.test("bond_companion"), "Nothing is reported before the companion reports its progress");
        pet.reportProgress(player);
        h.assertTrue(done.test("bond_companion"), "A bonded companion must grant Bound Together");
        h.assertTrue(
                !done.test("level_10") && !done.test("bond_level_5"),
                "A fresh companion has no level or bond milestones");

        pet.addExperience(Integer.MAX_VALUE / 2);
        h.assertTrue(
                done.test("level_10") && done.test("level_max"),
                "Levelling to the cap must grant both level advancements");
        h.assertTrue(pet.prestige() && done.test("prestige"), "Prestiging must grant Rising Star");
        h.assertTrue(done.test("stars_max") == (CompanionLevels.maxStars() <= 1), "Superstar needs every star");
        pet.addBondPoints(Integer.MAX_VALUE / 2);
        h.assertTrue(
                done.test("bond_level_5") && done.test("bond_level_15") && done.test("bond_level_max"),
                "Bonding to the cap must grant every bond advancement");

        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, new ItemStack(Items.RED_DYE));
        pet.mobInteract(player, hand);
        h.assertTrue(done.test("dye"), "Dyeing must grant True Colours");
        player.setItemInHand(hand, new ItemStack(Items.WOLF_ARMOR));
        pet.mobInteract(player, hand);
        h.assertTrue(done.test("equip"), "Equipping must grant Dressed for Success");
        player.setItemInHand(hand, ItemStack.EMPTY);
        pet.mobInteract(player, hand);
        h.assertTrue(done.test("open_storage"), "Opening storage must grant Saddlebags");
        player.closeContainer();

        h.assertTrue(!done.test("revive"), "Nothing has been revived yet");
        pet.hurtServer(h.getLevel(), pet.damageSources().genericKill(), 10000);
        KindredAttachments.modifyBond(
                player, b -> b.withReviveReadyAt(Math.max(1, h.getLevel().getGameTime())));
        action(player, Action.SUMMON);
        h.assertTrue(done.test("revive"), "Reviving must grant Not Goodbye");
        finish(h, player);
    }

    private static void beachSuspiciousSand(GameTestHelper h) {
        ServerPlayer player = player(h);
        var level = h.getLevel();
        BlockPos top = h.absolutePos(new BlockPos(3, 3, 3));
        for (int depth = 0; depth < 3; depth++) {
            level.setBlockAndUpdate(top.below(depth), Blocks.SAND.defaultBlockState());
        }
        var generator = level.getChunkSource().getGenerator();
        var feature = KindredFeatures.BEACH_SUSPICIOUS_SAND.get();
        boolean previous = KindredConfig.COMMON.beachSuspiciousSand.get();
        try {
            KindredConfig.COMMON.beachSuspiciousSand.set(false);
            h.assertTrue(
                    !feature.place(
                            NoneFeatureConfiguration.INSTANCE,
                            level,
                            generator,
                            net.minecraft.util.RandomSource.create(1),
                            top.above()),
                    "With beach_suspicious_sand off the feature must place nothing");
            KindredConfig.COMMON.beachSuspiciousSand.set(true);
            h.assertTrue(
                    feature.place(
                            NoneFeatureConfiguration.INSTANCE,
                            level,
                            generator,
                            net.minecraft.util.RandomSource.create(1),
                            top.above()),
                    "The feature must place into beach sand");
        } finally {
            KindredConfig.COMMON.beachSuspiciousSand.set(previous);
        }

        BlockPos found = null;
        int count = 0;
        for (int depth = 0; depth < 3; depth++) {
            if (level.getBlockState(top.below(depth)).is(Blocks.SUSPICIOUS_SAND)) {
                found = top.below(depth);
                count++;
            }
        }
        h.assertTrue(count == 1, "Exactly one sand block must turn suspicious (found " + count + ")");
        String table = level.getBlockEntity(found)
                .saveWithoutMetadata(level.registryAccess())
                .getStringOr("LootTable", "");
        h.assertTrue(
                table.equals(BeachSuspiciousSandFeature.LOOT_TABLE.identifier().toString()),
                "Beach suspicious sand must roll the beach table (found " + table + ")");

        LootTable loot = level.getServer().reloadableRegistries().getLootTable(BeachSuspiciousSandFeature.LOOT_TABLE);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.BRUSH))
                .create(LootContextParamSets.ARCHAEOLOGY);
        int eggs = 0;
        for (long seed = 1; seed <= 256; seed++) {
            var items = loot.getRandomItems(params, seed);
            h.assertTrue(items.size() == 1, "The beach table must yield exactly one find");
            if (items.getFirst().is(KindredItems.TREX_EGG.get())) {
                eggs++;
            }
        }
        h.assertTrue(eggs > 0 && eggs < 100, "Beach sand must sometimes hold a T-Rex Egg (found " + eggs + " in 256)");
        h.assertTrue(
                level.registryAccess()
                        .lookupOrThrow(Registries.PLACED_FEATURE)
                        .get(KindredSpirits.id("beach_suspicious_sand"))
                        .isPresent(),
                "The beach placed feature must load");
        h.assertTrue(
                level.registryAccess()
                        .lookupOrThrow(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                        .get(KindredSpirits.id("beach_suspicious_sand"))
                        .isPresent(),
                "The beach biome modifier must load");
        finish(h, player);
    }

    private static void archaeology(GameTestHelper h) {
        ServerPlayer player = player(h);
        for (String path : List.of("archaeology/desert_well", "archaeology/desert_pyramid")) {
            ResourceKey<LootTable> key =
                    ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
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
                if (loot.getFirst().is(KindredItems.TREX_EGG.get())) {
                    eggs++;
                    eggSeed = seed;
                }
            }
            h.assertTrue(eggs > 0 && eggs < 100, "Egg replacement must occur alongside ordinary archaeology loot");
            BlockPos pos = h.absolutePos(new BlockPos(7, 1, 7));
            h.getLevel().setBlockAndUpdate(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState());
            BrushableBlockEntity block = (BrushableBlockEntity) h.getLevel().getBlockEntity(pos);
            block.setLootTable(key, eggSeed);
            for (int i = 0; i < 10; i++)
                block.brush(
                        h.getLevel().getGameTime() + i * 10L,
                        h.getLevel(),
                        player,
                        Direction.UP,
                        new ItemStack(Items.BRUSH));
            h.assertTrue(
                    h.getLevel()
                                    .getEntitiesOfClass(
                                            ItemEntity.class,
                                            new net.minecraft.world.phys.AABB(pos).inflate(2),
                                            item -> item.getItem().is(KindredItems.TREX_EGG.get()))
                                    .size()
                            > 0,
                    "Brushing the selected loot seed must drop a T-Rex egg");
            h.getLevel()
                    .getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2))
                    .forEach(Entity::discard);
        }
        finish(h, player);
    }

    private static void storage(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.BABY_DRAGON, player, true);
        pet.setHealth(3);
        pet.setAbilityCooldown(CompanionAbilities.KILN_BREATH.id(), 6000);
        action(player, Action.DISMISS);
        h.assertTrue(
                pet.isRemoved() && KindredAttachments.bond(player).stored(),
                "Dismiss must store and remove the live entity");
        action(player, Action.SUMMON);
        CompanionEntity summoned = deployed(player);
        h.assertTrue(summoned != null && summoned.getHealth() == 3, "Summoning a resting companion must not heal it");
        h.assertTrue(
                summoned.abilityCooldownTicks(CompanionAbilities.KILN_BREATH.id()) == 6000,
                "Summoning must not reset Kiln Breath");
        finish(h, player);
    }

    private static void unreachable(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pet.setEquipment(new ItemStack(Items.DIAMOND_CHESTPLATE));
        KindredAttachments.modifyBond(player, b -> KindredCharmItem.snapshot(b, pet));
        CompanionBond before = KindredAttachments.bond(player);
        pet.discard(); // Models a deployed entity unavailable to the server lookup.
        for (Action action : List.of(Action.UNEQUIP, Action.RELEASE, Action.PRESTIGE, Action.SET_NAME, Action.SUMMON))
            action(player, action);
        h.assertTrue(
                before.equals(KindredAttachments.bond(player)), "Unavailable deployed snapshots must remain read-only");
        h.assertTrue(
                player.getInventory().countItem(Items.DIAMOND_CHESTPLATE) == 0,
                "Unreachable equipment must not be returned");
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
        KindredAttachments.modifyBond(
                player, b -> b.withReviveReadyAt(Math.max(1, h.getLevel().getGameTime())));
        action(player, Action.SUMMON);
        CompanionEntity revived = deployed(player);
        h.assertTrue(
                revived != null && revived.isAlive() && revived.getHealth() == revived.getMaxHealth(),
                "Revival must restore full health");
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
        var death = storage.getData(CompanionWorldData.DEATHS).stream()
                .filter(d -> d.companion().equals(pet.getUUID()))
                .findFirst()
                .orElseThrow();
        h.assertTrue(death.owner().equals(offline), "Offline death must be persisted with owner UUID");
        // Reassociate this fixture's queued owner with its connected test player.
        storage.setData(
                CompanionWorldData.DEATHS,
                storage.getData(CompanionWorldData.DEATHS).stream()
                        .map(d -> d == death
                                ? new CompanionWorldData.Death(
                                        player.getUUID(), d.companion(), d.snapshot(), d.readyAt())
                                : d)
                        .toList());
        CompanionWorldData.reconcile(player);
        h.assertTrue(
                KindredAttachments.bond(player).stored()
                        && KindredAttachments.bond(player).reviveReadyAt() == death.readyAt(),
                "Owner login must recover the death record");
        h.assertTrue(
                storage.getData(CompanionWorldData.DEATHS).stream()
                        .noneMatch(d -> d.companion().equals(pet.getUUID())),
                "Consumed death records must be removed");
        finish(h, player);
    }

    private static void bondLookup(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity bonded = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        CompanionEntity unbonded = pet(h, CompanionSpecies.NIGHTFOX, player, false);
        unbonded.setPos(player.position());
        h.assertTrue(
                CompanionEntity.bondedNear(player, 16).orElse(null) == bonded,
                "A closer unbonded pet must not mask the active bond");
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
        CompanionEntity pet =
                KindredEntities.type(CompanionSpecies.MINI_PLAYER).create(other, EntitySpawnReason.COMMAND);
        pet.tame(player);
        pet.setBonded(true);
        pet.setPos(0, 100, 0);
        // Generate the destination fixture before the tick-budgeted asynchronous lookup.
        other.getChunk(0, 0);
        other.addFreshEntity(pet);
        KindredAttachments.modifyBond(
                player, b -> KindredCharmItem.snapshot(b, pet).withStored(false));
        // The first request schedules the source chunk; entity loading completes on later ticks.
        h.succeedWhen(() -> {
            action(player, Action.RECALL);
            CompanionEntity arrived = deployed(player);
            h.assertTrue(
                    arrived != null && arrived != pet && pet.isRemoved(),
                    "Cross-dimension recall must load and replace the source entity (destination=" + arrived
                            + ", source removed=" + pet.isRemoved() + ")");
            h.assertTrue(
                    KindredAttachments.bond(player)
                            .lastDimension()
                            .orElseThrow()
                            .equals(player.level().dimension().identifier()),
                    "Recall must save the destination dimension");
            player.level().getServer().getPlayerList().remove(player);
        });
    }

    private static void mirrorStrike(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        LivingEntity target = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 4));
        float before = target.getHealth();
        target.hurtServer(h.getLevel(), player.damageSources().playerAttack(player), 4);
        h.assertTrue(
                Math.abs(target.getHealth() - (before - 5)) < 0.001,
                "Mirror Strike must add 25% through the triggering hit's invulnerability frames");
        h.assertTrue(
                pet.abilityCooldownTicks(CompanionAbilities.MIRROR_STRIKE.id()) == 20,
                "Echo must start its cooldown without recursion");
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
            h.assertTrue(
                    !pet.doHurtTarget(h.getLevel(), target) && target.getHealth() == health,
                    "Cosmetic mode must prevent companion melee damage");
            h.assertTrue(!pet.useActiveAbility(player), "Cosmetic mode must prevent active abilities");
        } finally {
            KindredConfig.COMMON.abilitiesEnabled.set(previous);
        }
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
                h.assertTrue(
                        points > previous && CompanionBondMath.levelFromPoints(points) == level,
                        "Extreme configured costs must stay positive and monotonic");
                previous = points;
            }
            h.assertTrue(
                    CompanionBondMath.clampPoints(Integer.MAX_VALUE) == previous,
                    "Clamping must remain valid after curve saturation");
            h.assertTrue(
                    CompanionBondMath.migrateLegacy(100) == previous,
                    "Legacy migration must not overflow at maximum bond");
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
        var json =
                CompanionProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow();
        CompanionProgress loaded =
                CompanionProgress.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        h.assertTrue(
                loaded.shareExperience(1, 0.5) == 1, "Two 1-XP gains must yield one companion XP even across saving");
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
            h.assertTrue(
                    decoded.state().equals(snapshot.state())
                            && ItemStack.matches(decoded.equipment(), snapshot.equipment()),
                    "Save and network codecs must preserve health, cooldowns, and equipment");
            h.assertTrue(buffer.readableBytes() == 0, "Snapshot codec must consume the whole payload");
        } finally {
            buffer.release();
        }
        finish(h, player);
    }

    private static void entitySave(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        pet.setAbilityCooldown(CompanionAbilities.SHADOW_BALL.id(), 200);
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        pet.save(output);
        CompanionEntity loaded = KindredEntities.type(pet.species()).create(h.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(
                TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(
                loaded.isBonded() && loaded.abilityCooldownTicks(CompanionAbilities.SHADOW_BALL.id()) == 200,
                "Entity NBT must retain deployed bond status and cooldowns");
        finish(h, player);
    }

    private static void chestplateStats(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pet.setEquipment(new ItemStack(Items.DIAMOND_CHESTPLATE));
        // Vanilla equipment modifiers are applied on the next living-entity tick.
        h.runAfterDelay(2, () -> {
            CompanionStats stored = CompanionStats.of(pet.species(), CompanionSnapshot.of(pet));
            h.assertTrue(
                    Math.abs(stored.modified().get(2) - pet.getAttributeValue(Attributes.ARMOR)) < 0.001,
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
        h.assertTrue(
                progress.bondLevel() == 5
                        && progress.bondMaxLevel() == CompanionBondMath.maxLevel()
                        && progress.experienceToNext() == pet.experienceToNextLevel(),
                "Tooltip attachment must carry authoritative progression values");
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
        h.assertTrue(
                h.getLevel().getBlockState(first).is(Blocks.STONE)
                        && h.getLevel().getBlockState(second).isAir(),
                "Release must clean owned lights and preserve replacement blocks");
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
        var entry =
                pending.stream().filter(e -> e.pos().equals(far)).findFirst().orElseThrow();
        var json = CompanionWorldData.LightRemoval.CODEC
                .encodeStart(JsonOps.INSTANCE, entry)
                .getOrThrow();
        h.assertTrue(
                CompanionWorldData.LightRemoval.CODEC
                        .parse(JsonOps.INSTANCE, json)
                        .getOrThrow()
                        .equals(entry),
                "Unloaded removals must survive serialization");
        h.assertTrue(!level.isLoaded(far), "Cleanup must not load distant chunks");
        BlockPos local = h.absolutePos(new BlockPos(7, 1, 7));
        level.setBlockAndUpdate(local, KindredBlocks.WISP_LIGHT.get().defaultBlockState());
        level.setData(
                CompanionWorldData.LIGHT_REMOVALS, List.of(new CompanionWorldData.LightRemoval(local, entry.block())));
        CompanionWorldData.cleanLoadedLights(level);
        h.assertTrue(
                level.getBlockState(local).isAir()
                        && level.getData(CompanionWorldData.LIGHT_REMOVALS).isEmpty(),
                "Queued cleanup must finish once its chunk is available");
        h.succeed();
    }

    private static void deathAnimation(GameTestHelper h) {
        CompanionEntity trex =
                h.spawnWithNoFreeWill(KindredEntities.type(CompanionSpecies.TREX), new BlockPos(4, 1, 4));
        CompanionEntity gremlin =
                h.spawnWithNoFreeWill(KindredEntities.type(CompanionSpecies.GREMLIN), new BlockPos(8, 1, 8));
        trex.hurtServer(h.getLevel(), trex.damageSources().genericKill(), 10000);
        gremlin.hurtServer(h.getLevel(), gremlin.damageSources().genericKill(), 10000);
        h.runAfterDelay(
                21,
                () -> h.assertTrue(
                        !trex.isRemoved() && !gremlin.isRemoved(),
                        "Long death animations must outlive vanilla's 20 ticks"));
        h.runAfterDelay(
                35, () -> h.assertTrue(!trex.isRemoved() && gremlin.isRemoved(), "Gremlin should finish at 30 ticks"));
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
        h.assertTrue(
                h.getLevel()
                                .getEntitiesOfClass(
                                        CompanionEntity.class,
                                        new net.minecraft.world.phys.AABB(pos).inflate(2),
                                        pet -> pet.species() == CompanionSpecies.TREX)
                                .size()
                        == 1,
                "Final stage must spawn exactly one T-Rex");
        h.succeed();
    }

    private static void xpEvents(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        double previous = KindredConfig.COMMON.xpShare.get();
        try {
            KindredConfig.COMMON.xpShare.set(0.5);
            pickUpOrb(h, player, 1, false);
            h.assertTrue(pet.getExperience() == 0, "The first half-point should remain fractional");
            pickUpOrb(h, player, 1, false);
            h.assertTrue(pet.getExperience() == 1, "Actual player XP events must accumulate small shares");
        } finally {
            KindredConfig.COMMON.xpShare.set(previous);
        }
        finish(h, player);
    }

    private static void returnedXp(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        pickUpOrb(h, player, 20, false);
        int earned = pet.getExperience();
        h.assertTrue(earned > 0, "A picked up orb must share xp");
        player.giveExperiencePoints(500);
        h.assertTrue(pet.getExperience() == earned, "Xp handed back without an orb, as by a grave, must not share");
        pickUpOrb(h, player, 20, true);
        h.assertTrue(pet.getExperience() == earned, "Bottle o' Enchanting orbs must not share");
        player.giveExperiencePoints(500);
        h.assertTrue(pet.getExperience() == earned, "A bottle pickup must not let the next direct gain share");
        finish(h, player);
    }

    private static void pickUpOrb(GameTestHelper h, ServerPlayer player, int value, boolean bottle) {
        ExperienceOrb orb = new ExperienceOrb(h.getLevel(), player.getX(), player.getY(), player.getZ(), value);
        orb.setData(KindredAttachments.BOTTLE_XP, bottle);
        h.getLevel().addFreshEntity(orb);
        player.takeXpDelay = 0;
        orb.playerTouch(player);
    }

    private static void feeding(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, player, true);
        var ingredient = h.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ITEM)
                .getOrThrow(pet.species().tamingTag())
                .iterator()
                .next()
                .value();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ingredient, 3));
        pet.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        int points = pet.getBondPoints();
        h.assertTrue(
                points > 0 && !pet.progress().canFeed(h.getLevel().getGameTime()),
                "Feeding must grant bond and start its cooldown");
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
            if (event.getEntity() instanceof CompanionEntity pet
                    && pet.species() == CompanionSpecies.NIGHTFOX
                    && pet.position().distanceToSqr(fox.position()) < 0.01) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var result = CompanionSpawns.transform(
                    fox, CompanionSpecies.NIGHTFOX, net.minecraft.core.particles.ParticleTypes.SCULK_SOUL);
            h.assertTrue(result == null && !fox.isRemoved(), "A canceled companion spawn must retain the original mob");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);
        }
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
        pet.lights()
                .place(
                        h.getLevel(),
                        CompanionAbilities.WISPLIGHT.id(),
                        pos,
                        KindredBlocks.WISP_LIGHT.get().defaultBlockState(),
                        1);
        var arrived = pet.teleport(new net.minecraft.world.level.portal.TeleportTransition(
                h.getLevel().getServer().getLevel(Level.NETHER),
                new Vec3(0, 100, 0),
                Vec3.ZERO,
                0,
                0,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        h.assertTrue(
                arrived != null && h.getLevel().getBlockState(pos).isAir(),
                "Dimension travel must clear lights in the source world");
        h.assertTrue(
                ((CompanionEntity) arrived).lights().isEmpty(),
                "Destination must not inherit source-world light coordinates");
        arrived.discard();
        finish(h, player);
    }

    private static void lightUnload(GameTestHelper h) {
        ServerPlayer player = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.NIGHTFOX, player, true);
        BlockPos pos = h.absolutePos(new BlockPos(7, 1, 7));
        pet.lights()
                .place(
                        h.getLevel(),
                        CompanionAbilities.WISPLIGHT.id(),
                        pos,
                        KindredBlocks.WISP_LIGHT.get().defaultBlockState(),
                        1);
        // The entity-section manager calls setRemoved directly, bypassing Entity.remove.
        pet.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        h.assertTrue(
                h.getLevel().getBlockState(pos).isAir() && pet.lights().isEmpty(),
                "Chunk unloading must invoke light cleanup through the removal callback");
        finish(h, player);
    }

    private KindredGameTests() {}

    private static void quokkaBuffs(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity quokka = pet(h, CompanionSpecies.QUOKKA, owner, true);
        h.assertTrue(
                quokka.hasAbility(CompanionAbilities.SMILE) && !quokka.hasAbility(CompanionAbilities.BRIGHTER_SIDE),
                "Only Smile unlocks at level one, bond zero");
        CompanionAbilities.SMILE.serverTick(quokka, owner);
        h.assertTrue(
                quokka.abilityCooldownTicks(CompanionAbilities.SMILE.id()) == 2340,
                "Level one Smile cooldown is 117 seconds");
        h.assertTrue(owner.getActiveEffects().size() == 1, "Smile gives the owner one positive effect");
        quokka.setBondPoints(CompanionBondMath.pointsAtLevelStart(5));
        h.assertTrue(quokka.hasAbility(CompanionAbilities.BRIGHTER_SIDE), "Bond five unlocks Brighter Side");
        CompanionAbilities.BRIGHTER_SIDE.serverTick(quokka, owner);
        h.assertTrue(owner.hasEffect(net.minecraft.world.effect.MobEffects.LUCK), "Nearby owner must receive Luck I");
        quokka.setBondPoints(CompanionBondMath.pointsAtLevelStart(15));
        quokka.setAbilityCooldown(CompanionAbilities.SMILE.id(), 0);
        CompanionAbilities.SMILE.serverTick(quokka, owner);
        h.assertTrue(
                quokka.abilityCooldownTicks(CompanionAbilities.SMILE.id()) == 1170,
                "Bond fifteen halves the Smile cooldown");
        quokka.setBondPoints(CompanionBondMath.pointsAtLevelStart(5));
        quokka.addExperience(Integer.MAX_VALUE);
        h.assertTrue(quokka.hasAbility(CompanionAbilities.ALWAYS_HAPPY), "Level thirty unlocks Always Happy");
        quokka.setAbilityCooldown(CompanionAbilities.SMILE.id(), 0);
        CompanionAbilities.SMILE.serverTick(quokka, owner);
        h.assertTrue(
                quokka.abilityCooldownTicks(CompanionAbilities.SMILE.id()) == 600,
                "Level thirty Smile cooldown is thirty seconds");
        quokka.setBondPoints(CompanionBondMath.pointsAtLevelStart(30));
        quokka.setAbilityCooldown(CompanionAbilities.SMILE.id(), 0);
        CompanionAbilities.SMILE.serverTick(quokka, owner);
        h.assertTrue(
                quokka.abilityCooldownTicks(CompanionAbilities.SMILE.id()) == 150,
                "Bond thirty quarters the level thirty Smile cooldown");
        owner.removeAllEffects();
        quokka.teleportTo(owner.getX() + 20, owner.getY(), owner.getZ());
        CompanionAbilities.BRIGHTER_SIDE.serverTick(quokka, owner);
        h.assertTrue(
                !owner.hasEffect(net.minecraft.world.effect.MobEffects.LUCK),
                "Luck cannot reach beyond sixteen blocks");
        finish(h, owner);
    }

    private static void bondRateScope(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity nightfox = pet(h, CompanionSpecies.NIGHTFOX, owner, true);
        nightfox.setBondPoints(CompanionBondMath.pointsAtLevelStart(30));
        h.assertTrue(
                nightfox.scaledInterval(CompanionAbilities.WISPLIGHT)
                        == CompanionAbilities.WISPLIGHT.intervalTicks() / 4,
                "Bond thirty runs the base buff four times as often");
        h.assertTrue(
                nightfox.scaledInterval(CompanionAbilities.SWIFT_STEP) == CompanionAbilities.SWIFT_STEP.intervalTicks(),
                "Bond does not speed up abilities other than the base buff");
        h.assertTrue(
                nightfox.scaledInterval(CompanionAbilities.LEADER_OF_THE_PACK)
                        == CompanionAbilities.LEADER_OF_THE_PACK.intervalTicks(),
                "Bond does not speed up ultimates");
        finish(h, owner);
    }

    private static void dyeRecolour(GameTestHelper h) {
        ServerPlayer owner = player(h);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        CompanionEntity trex = pet(h, CompanionSpecies.TREX, owner, true);
        owner.setItemInHand(hand, new ItemStack(Items.RED_DYE));
        trex.mobInteract(owner, hand);
        int red = net.minecraft.world.item.DyeColor.RED.getId();
        h.assertTrue(trex.getDyeId() == red, "Dye recolours an owned companion");
        CompanionSnapshot snapshot = CompanionSnapshot.of(trex);
        h.assertTrue(snapshot.dye() == red, "The resting snapshot keeps the dye");
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var saved = CompanionSnapshot.CODEC.encodeStart(ops, snapshot).getOrThrow();
        h.assertTrue(CompanionSnapshot.CODEC.parse(ops, saved).getOrThrow().dye() == red, "The dye survives a save");
        owner.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
        trex.mobInteract(owner, hand);
        h.assertTrue(trex.getDyeId() == CompanionEntity.NO_DYE, "A water bucket washes the dye off");
        h.assertTrue(owner.getItemInHand(hand).is(Items.WATER_BUCKET), "Washing keeps the water bucket");
        CompanionEntity mini = pet(h, CompanionSpecies.MINI_PLAYER, owner, false);
        owner.setItemInHand(hand, new ItemStack(Items.BLUE_DYE));
        mini.mobInteract(owner, hand);
        h.assertTrue(mini.getDyeId() == CompanionEntity.NO_DYE, "The Mini Player keeps its player skin instead of dye");
        finish(h, owner);
    }

    private static void spiritBandage(GameTestHelper h) {
        ServerPlayer owner = player(h);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        CompanionEntity trex = pet(h, CompanionSpecies.TREX, owner, true);
        trex.setHealth(trex.getMaxHealth() - 20.0f);
        float before = trex.getHealth();
        owner.setItemInHand(hand, new ItemStack(KindredItems.SPIRIT_BANDAGE.get()));
        trex.mobInteract(owner, hand);
        float bandaged = trex.getHealth() - before;
        h.assertTrue(Math.abs(bandaged - 6.0f) < 0.01f, "A Spirit Bandage restores 3 hearts");
        before = trex.getHealth();
        owner.setItemInHand(hand, new ItemStack(Items.GOLDEN_APPLE));
        trex.mobInteract(owner, hand);
        h.assertTrue(trex.getHealth() - before > bandaged, "A golden apple heals more than a bandage");
        finish(h, owner);
    }

    private static void quokkaSnack(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity quokka = pet(h, CompanionSpecies.QUOKKA, owner, true);
        quokka.setEquipment(new ItemStack(KindredItems.QUOKKA_SNACK.get()));
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 4));
        var skeleton = h.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(7, 1, 4));
        // Find a deterministic successful roll without relying on a particular random implementation.
        long seed = 0;
        while (true) {
            quokka.getRandom().setSeed(seed);
            if (quokka.getRandom().nextFloat() < 0.2f) break;
            seed++;
        }
        quokka.getRandom().setSeed(seed);
        QuokkaSupport.tick(quokka);
        h.assertTrue(
                QuokkaSupport.isCharmed(zombie) && zombie.getTarget() == skeleton,
                "Snack charms the nearest hostile into attacking another monster");
        h.assertTrue(
                quokka.abilityCooldownTicks(KindredSpirits.id("quokka_snack")) == 600,
                "Each snack attempt has a thirty-second cooldown");
        QuokkaSupport.tick(quokka);
        h.assertTrue(!QuokkaSupport.isCharmed(skeleton), "Repeated ticks cannot bypass the snack cooldown");
        action(owner, Action.DISMISS);
        action(owner, Action.SUMMON);
        CompanionEntity summoned = deployed(owner);
        h.assertTrue(
                summoned.hasEquipment(KindredItems.QUOKKA_SNACK.get())
                        && summoned.abilityCooldownTicks(KindredSpirits.id("quokka_snack")) == 600,
                "Dismiss and summon retain the snack and cooldown");
        h.runAfterDelay(2, () -> {
            h.assertTrue(
                    summoned.equipmentId()
                            .equals(KindredItems.QUOKKA_SNACK.getId().toString()),
                    "Equipped snack must synchronize the smiling appearance");
            summoned.setEquipment(ItemStack.EMPTY);
            h.runAfterDelay(2, () -> {
                h.assertTrue(summoned.equipmentId().isEmpty(), "Unequipping restores the normal appearance");
                finish(h, owner);
            });
        });
    }

    private static void quokkaCharm(GameTestHelper h) {
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 1, 4));
        var skeleton = h.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(7, 1, 4));
        var cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(4, 1, 4));
        QuokkaSupport.charm(zombie, 5);
        zombie.setTarget(cow);
        h.assertTrue(
                zombie.getTarget() == skeleton,
                "Vanilla target changes cannot redirect a charmed mob onto a non-hostile");
        float health = cow.getHealth();
        cow.hurtServer(h.getLevel(), zombie.damageSources().mobAttack(zombie), 4);
        h.assertTrue(cow.getHealth() == health, "Charmed attacks cannot hurt non-hostiles");
        h.runAfterDelay(8, () -> {
            h.assertTrue(
                    !zombie.hasData(KindredAttachments.CHARMED) && zombie.getTarget() == null,
                    "Charm must expire and clear its forced target");
            zombie.setTarget(cow);
            h.assertTrue(zombie.getTarget() == cow, "Normal targeting resumes after expiry");
            h.succeed();
        });
    }

    private static void quokkaCharmSave(GameTestHelper h) {
        var mob = h.spawnWithNoFreeWill(EntityType.PIGLIN, new BlockPos(5, 1, 4));
        var other = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(7, 1, 4));
        QuokkaSupport.charm(mob, 400);
        h.assertTrue(
                mob.getBrain()
                                .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
                                .orElse(null)
                        == other,
                "Brain-based hostiles receive the charmed target too");
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        mob.save(output);
        var loaded = EntityType.PIGLIN.create(h.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(
                TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(
                QuokkaSupport.isCharmed(loaded)
                        && loaded.getData(KindredAttachments.CHARMED).equals(mob.getData(KindredAttachments.CHARMED)),
                "Charm's absolute expiry survives save/load without extending it");
        h.succeed();
    }

    private static void quokkaBreeding(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity quokka = pet(h, CompanionSpecies.QUOKKA, owner, true);
        var first = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 4));
        var second = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(6, 1, 4));
        var lonePig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(5, 1, 6));
        for (int i = 0; i < 16; i++) h.spawnWithNoFreeWill(EntityType.SHEEP, new BlockPos(5 + i % 4, 1, 5 + i / 4));
        QuokkaSupport.breed(quokka, owner);
        h.assertTrue(
                first.isInLove() && second.isInLove() && !lonePig.isInLove(), "Only compatible pairs enter love mode");
        h.assertTrue(
                h
                        .getLevel()
                        .getEntitiesOfClass(
                                net.minecraft.world.entity.animal.sheep.Sheep.class,
                                quokka.getBoundingBox().inflate(8))
                        .stream()
                        .noneMatch(animal -> animal.isInLove()),
                "A herd of sixteen must not be bred");
        first.resetLove();
        second.resetLove();
        QuokkaSupport.breed(quokka, owner);
        h.assertTrue(
                !first.isInLove() && quokka.abilityCooldownTicks(CompanionAbilities.ALWAYS_HAPPY.id()) == 2400,
                "Breeding obeys its two-minute cooldown");
        finish(h, owner);
    }

    private static void quokkaGrowth(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity first = pet(h, CompanionSpecies.QUOKKA, owner, true);
        CompanionEntity second = pet(h, CompanionSpecies.QUOKKA, owner, false);
        second.setBonded(true);
        first.addExperience(Integer.MAX_VALUE);
        second.addExperience(Integer.MAX_VALUE);
        var calf = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(5, 1, 4));
        calf.setAge(-100);
        QuokkaSupport.tick(first);
        QuokkaSupport.tick(second);
        h.assertTrue(calf.getAge() == -98, "Two Quokkas provide only two extra age ticks, not four");
        first.setDisabledAbilityNames(List.of("always_happy"));
        second.setDisabledAbilityNames(List.of("always_happy"));
        h.runAfterDelay(2, () -> {
            int age = calf.getAge();
            QuokkaSupport.tick(first);
            h.assertTrue(calf.getAge() == age, "Disabled Always Happy cannot accelerate babies");
            finish(h, owner);
        });
    }

    private static void quokkaDecoy(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity baby =
                h.spawnWithNoFreeWill(KindredEntities.type(CompanionSpecies.QUOKKA), new BlockPos(5, 1, 4));
        baby.makeQuokkaDecoy(5);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.FERN, 64));
        for (int i = 0; i < 64; i++) baby.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(baby.getBbWidth() < CompanionSpecies.QUOKKA.width(), "Baby decoys have a smaller hitbox");
        h.assertTrue(!baby.isTame() && baby.isBaby(), "Thrown babies remain babies and cannot be tamed");
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        baby.save(output);
        CompanionEntity loaded =
                KindredEntities.type(CompanionSpecies.QUOKKA).create(h.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(
                TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        baby.discard();
        h.getLevel().addFreshEntity(loaded);
        h.assertTrue(loaded.isQuokkaDecoy(), "Temporary status survives a chunk/save reload");
        h.runAfterDelay(8, () -> {
            h.assertTrue(loaded.isRemoved(), "Reloading cannot extend the decoy lifetime");
            finish(h, owner);
        });
    }

    private static void quokkaFlee(GameTestHelper h) {
        CompanionEntity wild = h.spawn(KindredEntities.type(CompanionSpecies.QUOKKA), new BlockPos(5, 1, 4));
        var attacker = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(7, 1, 4));
        wild.hurtServer(h.getLevel(), attacker.damageSources().mobAttack(attacker), 1);
        h.assertTrue(wild.isQuokkaFleeing(), "Hurt wild adults enter the escape window");
        var babies = h.getLevel()
                .getEntitiesOfClass(
                        CompanionEntity.class, wild.getBoundingBox().inflate(4), CompanionEntity::isQuokkaDecoy);
        h.assertTrue(
                babies.size() == 1 && babies.getFirst().getDeltaMovement().x > 0,
                "One baby is thrown toward the attacker");
        wild.invulnerableTime = 0;
        wild.hurtServer(h.getLevel(), attacker.damageSources().mobAttack(attacker), 1);
        h.assertTrue(
                h.getLevel()
                                .getEntitiesOfClass(
                                        CompanionEntity.class,
                                        wild.getBoundingBox().inflate(4),
                                        CompanionEntity::isQuokkaDecoy)
                                .size()
                        == 1,
                "Hits during the same escape cannot flood the world with babies");
        h.runAfterDelay(5, () -> {
            h.assertTrue(
                    wild.getMoveControl().getSpeedModifier() == 1.5 && !wild.isSprinting(),
                    "Escape uses 1.5x movement without adding Minecraft's separate sprint bonus");
        });
        h.runAfterDelay(202, () -> {
            h.assertTrue(!wild.isQuokkaFleeing(), "Escape ends after ten seconds");
            h.succeed();
        });
    }

    private static void trexGloves(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity trex = pet(h, CompanionSpecies.TREX, owner, true);
        h.assertTrue(trex.equipmentId().isEmpty(), "A bare companion syncs no equipment");
        trex.setEquipment(new ItemStack(KindredItems.BOXING_GLOVES.get()));
        h.assertTrue(
                trex.equipmentId().equals(KindredItems.BOXING_GLOVES.getId().toString()),
                "Equipping must sync the item id the renderer swaps the gloved model on");
        h.assertTrue(
                CompanionEntity.equipmentId(new ItemStack(Items.WOLF_ARMOR)).equals("minecraft:wolf_armor"),
                "Synced ids are registry keys");
        action(owner, Action.UNEQUIP);
        h.assertTrue(
                trex.equipmentId().isEmpty()
                        && owner.getInventory().contains(new ItemStack(KindredItems.BOXING_GLOVES.get())),
                "Taking the gloves off clears the synced id and returns them");
        finish(h, owner);
    }

    private static void dragonTablet(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity dragon = pet(h, CompanionSpecies.BABY_DRAGON, owner, true);
        dragon.setEquipment(new ItemStack(KindredItems.DRAGON_TABLET.get()));
        h.assertTrue(
                dragon.equipmentId().equals(KindredItems.DRAGON_TABLET.getId().toString()),
                "Equipping must sync the item id the renderer swaps the tablet model on");
        action(owner, Action.DISMISS);
        action(owner, Action.SUMMON);
        CompanionEntity summoned = deployed(owner);
        h.assertTrue(
                summoned.equipmentId().equals(KindredItems.DRAGON_TABLET.getId().toString()),
                "A summoned companion syncs its tablet before the first render");
        action(owner, Action.UNEQUIP);
        h.assertTrue(
                summoned.equipmentId().isEmpty()
                        && owner.getInventory().contains(new ItemStack(KindredItems.DRAGON_TABLET.get())),
                "Taking the tablet off clears the synced id and returns it");
        finish(h, owner);
    }

    private static void meteorCall(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity dragon = pet(h, CompanionSpecies.BABY_DRAGON, owner, true);
        dragon.setEquipment(new ItemStack(KindredItems.DRAGON_TABLET.get()));
        var husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 8));
        float before = husk.getHealth();
        dragon.callMeteor(husk);
        AABB column = husk.getBoundingBox().inflate(1, 16, 1);
        var meteors = h.getLevel().getEntitiesOfClass(MeteorEntity.class, column);
        h.assertTrue(
                meteors.size() == 1
                        && meteors.getFirst().getOwner() == dragon
                        && meteors.getFirst().getY() > husk.getY() + 10,
                "Meteor Call must drop one meteor, owned by the dragon, from above the target");
        h.succeedWhen(() -> {
            h.assertTrue(
                    h.getLevel().getEntitiesOfClass(MeteorEntity.class, column).isEmpty(),
                    "The meteor must reach the ground");
            h.assertTrue(
                    husk.getHealth() <= before - 8.0f,
                    "The impact must deal Meteor Call's 8 damage (health " + husk.getHealth() + ")");
            h.getLevel().getServer().getPlayerList().remove(owner);
        });
    }

    private static void helpingHandGuard(GameTestHelper h) {
        var level = h.getLevel();
        List<String> none = List.of();
        h.assertFalse(
                HelpingHandGuard.allows(level, null, Items.IRON_INGOT, List.of(Items.IRON_BLOCK), none, 4),
                "Unpacking an iron block must not be copied, the ingots craft straight back");
        h.assertFalse(
                HelpingHandGuard.allows(level, null, Items.IRON_BLOCK, List.of(Items.IRON_INGOT), none, 4),
                "Packing ingots into a block must not be copied either");
        h.assertFalse(
                HelpingHandGuard.allows(level, null, Items.IRON_NUGGET, List.of(Items.IRON_INGOT), none, 4),
                "Nuggets craft back into the ingot");
        h.assertFalse(
                HelpingHandGuard.allows(level, null, Items.IRON_NUGGET, List.of(Items.IRON_BLOCK), none, 4),
                "A two step chain back to the input must be caught");
        h.assertFalse(
                HelpingHandGuard.allows(level, null, Items.IRON_NUGGET, List.of(), none, 4),
                "A craft with no visible inputs must not be copied");
        h.assertTrue(
                HelpingHandGuard.allows(level, null, Items.OAK_PLANKS, List.of(Items.OAK_LOG), none, 4),
                "Planks cannot be turned back into logs, so they may be copied");
        Identifier planks = Identifier.withDefaultNamespace("oak_planks");
        h.assertFalse(
                HelpingHandGuard.allows(
                        level, planks, Items.OAK_PLANKS, List.of(Items.OAK_LOG), List.of("minecraft:oak_planks"), 4),
                "An exact blacklist entry must block the recipe");
        h.assertFalse(
                HelpingHandGuard.allows(
                        level, planks, Items.OAK_PLANKS, List.of(Items.OAK_LOG), List.of("minecraft:*"), 4),
                "A wildcard blacklist entry must block every recipe it prefixes");
        h.assertTrue(
                HelpingHandGuard.allows(
                        level, planks, Items.OAK_PLANKS, List.of(Items.OAK_LOG), List.of("othermod:*"), 4),
                "A wildcard for another namespace must not block the recipe");
        h.succeed();
    }

    private static void crushingMight(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity trex = pet(h, CompanionSpecies.TREX, owner, true);
        Function<TagKey<Item>, List<Item>> dusts =
                tag -> tag.location().toString().equals("c:dusts/iron")
                        ? List.of(Items.SUGAR)
                        : OreCrushing.registryItems(tag);
        var level = h.getLevel();
        Vec3 at = trex.position();
        Consumer<ItemStack> drop =
                stack -> level.addFreshEntity(new ItemEntity(level, at.x + 2, at.y, at.z, stack, 0, 0, 0));
        drop.accept(new ItemStack(Items.RAW_IRON, 30));
        drop.accept(new ItemStack(Items.IRON_ORE, 1));
        drop.accept(new ItemStack(Items.RAW_GOLD, 4));
        ItemEntity far = new ItemEntity(level, at.x + 7, at.y, at.z, new ItemStack(Items.RAW_IRON, 5), 0, 0, 0);
        level.addFreshEntity(far);

        int crushed = OreCrushing.crushAround(level, trex, 5.0, 3, dusts);
        h.assertTrue(
                crushed == 31,
                "Thirty raw iron and one iron ore within 5 blocks must be crushed (crushed " + crushed + ")");
        List<ItemEntity> near =
                level.getEntitiesOfClass(ItemEntity.class, trex.getBoundingBox().inflate(5.0));
        int sugar = near.stream()
                .filter(item -> item.getItem().is(Items.SUGAR))
                .mapToInt(item -> item.getItem().getCount())
                .sum();
        h.assertTrue(sugar == 93, "Each ore must become three dust (found " + sugar + ")");
        h.assertTrue(
                near.stream()
                        .allMatch(item ->
                                item.getItem().getCount() <= item.getItem().getMaxStackSize()),
                "Dust must be split into full stacks");
        h.assertTrue(
                near.stream()
                        .anyMatch(item -> item.getItem().is(Items.RAW_GOLD)
                                && item.getItem().getCount() == 4),
                "An ore with no matching dust must be left alone");
        h.assertTrue(
                far.getItem().is(Items.RAW_IRON) && far.getItem().getCount() == 5,
                "Ore beyond 5 blocks must be left alone");
        finish(h, owner);
    }

    private static void spiritArrow(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.MINI_PLAYER, owner, true);
        var husk = h.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 1, 9));
        pet.performRangedAttack(husk, 1.0f);
        var arrows =
                h.getLevel().getEntitiesOfClass(SpiritArrow.class, h.getBounds().inflate(4));
        h.assertTrue(
                arrows.size() == 1 && arrows.getFirst().getOwner() == pet,
                "The Mini Player must fire one spirit arrow that it owns");
        h.assertTrue(
                arrows.getFirst().pickup == net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED,
                "Spirit arrows must not be pickupable");
        finish(h, owner);
    }

    private static void skinChoice(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity fox = pet(h, CompanionSpecies.NIGHTFOX, owner, true);
        KindredCharmItem.handleAction(owner, Action.SET_SKIN, 0, "frost");
        h.assertTrue(fox.getSkinName().equals("frost"), "A valid skin id must be stored on the companion");
        KindredCharmItem.handleAction(owner, Action.SET_SKIN, 0, "Frost Fox!");
        h.assertTrue(fox.getSkinName().equals("frost"), "An invalid skin id must be refused");
        action(owner, Action.DISMISS);
        action(owner, Action.SUMMON);
        CompanionEntity summoned = deployed(owner);
        h.assertTrue(summoned.getSkinName().equals("frost"), "The skin must survive a rest in the charm");
        KindredCharmItem.handleAction(owner, Action.SET_SKIN, 0, "");
        h.assertTrue(summoned.getSkinName().isEmpty(), "An empty skin id goes back to the default look");
        finish(h, owner);
    }

    private static void direwolfTransform(GameTestHelper h) {
        ServerPlayer owner = player(h);
        ServerPlayer stranger = player(h);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var wild = h.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(4, 1, 4));
        var mine = h.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(6, 1, 4));
        var theirs = h.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(8, 1, 4));
        mine.tame(owner);
        mine.setCustomName(net.minecraft.network.chat.Component.literal("Fenrir"));
        mine.setBodyArmorItem(new ItemStack(Items.WOLF_ARMOR));
        theirs.tame(stranger);
        owner.setItemInHand(hand, new ItemStack(KindredItems.GOLDEN_BONE.get()));
        for (var wolf : List.of(wild, mine, theirs)) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(
                            owner, hand, wolf));
        }
        h.assertTrue(
                wild.isRemoved() && mine.isRemoved() && !theirs.isRemoved(),
                "Only wild wolves and the player's own wolves transform");
        var direwolves = h.getLevel()
                .getEntitiesOfClass(
                        CompanionEntity.class,
                        h.getBounds().inflate(2),
                        pet -> pet.species() == CompanionSpecies.DIREWOLF);
        h.assertTrue(direwolves.size() == 2, "Two wolves must have become Direwolves");
        var tamed =
                direwolves.stream().filter(CompanionEntity::isTame).findFirst().orElseThrow();
        h.assertTrue(
                tamed.isOwnedBy(owner) && "Fenrir".equals(tamed.getCustomName().getString()),
                "A tamed wolf comes out tamed and keeps its name");
        h.assertTrue(tamed.hasEquipment(Items.WOLF_ARMOR), "A tamed wolf's armour carries over as equipment");
        h.assertTrue(direwolves.stream().anyMatch(pet -> !pet.isTame()), "A wild wolf comes out wild");
        h.assertTrue(
                new ItemStack(KindredItems.GOLDEN_BONE.get()).is(CompanionSpecies.DIREWOLF.tamingTag()),
                "Golden Bone must resolve as the Direwolf taming item");
        h.assertTrue(
                new ItemStack(Items.WOLF_ARMOR).is(CompanionSpecies.DIREWOLF.equipmentTag()),
                "Wolf armour must resolve as Direwolf equipment");
        h.getLevel().getServer().getPlayerList().remove(stranger);
        finish(h, owner);
    }

    private static void direwolfDig(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.DIREWOLF, owner, true);
        pet.setCommand(CompanionCommand.FOLLOW);
        BlockPos ground = h.absolutePos(new BlockPos(4, 0, 4));
        h.getLevel().setBlockAndUpdate(ground, Blocks.GRAVEL.defaultBlockState());
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(CompanionEntity.DIG_LOOT);
        int treasure = 0;
        for (long seed = 1; seed <= 64; seed++) {
            LootParams params = new LootParams.Builder(h.getLevel())
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(ground))
                    .withParameter(LootContextParams.THIS_ENTITY, pet)
                    .create(LootContextParamSets.GIFT);
            var loot = table.getRandomItems(params, seed);
            h.assertTrue(loot.size() == 1, "A dig must yield exactly one stack");
            if (!loot.getFirst().is(Items.BONE)
                    && !loot.getFirst().is(Items.FLINT)
                    && !loot.getFirst().is(Items.WHEAT_SEEDS)
                    && !loot.getFirst().is(Items.BEETROOT_SEEDS)) treasure++;
        }
        h.assertTrue(treasure > 0, "Gravel must sometimes roll the trail ruins archaeology tables");
        h.runAfterDelay(5, () -> {
            h.assertTrue(pet.isIdle(), "A standing companion with no target must count as idle");
            CompanionAbilities.NOT_ANOTHER_HOLE.serverTick(pet, owner);
            h.assertTrue(
                    pet.isDigging() && !pet.isAbilityReady(CompanionAbilities.NOT_ANOTHER_HOLE.id()),
                    "Digging must start on diggable ground and begin the cooldown");
            h.assertTrue(
                    pet.abilityCooldownTicks(CompanionAbilities.NOT_ANOTHER_HOLE.id()) == (180 - 4) * 20,
                    "Level one digs every 176 seconds");
            h.runAfterDelay(45, () -> {
                h.assertTrue(!pet.isDigging(), "Digging must finish after two seconds");
                h.assertTrue(
                        !h.getLevel()
                                .getEntitiesOfClass(
                                        ItemEntity.class, pet.getBoundingBox().inflate(3))
                                .isEmpty(),
                        "Finishing a dig must drop an item");
                finish(h, owner);
            });
        });
    }

    private static void direwolfArmour(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity pet = pet(h, CompanionSpecies.DIREWOLF, owner, true);
        pet.setBondPoints(CompanionBondMath.pointsAtLevelStart(5));
        pet.setEquipment(new ItemStack(Items.WOLF_ARMOR));
        h.assertTrue(
                pet.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR), "Wolf armour is worn in the body slot");
        h.runAfterDelay(2, () -> {
            h.assertTrue(
                    Math.abs(pet.getAttributeValue(Attributes.ARMOR) - (CompanionSpecies.DIREWOLF.armour() + 11))
                            < 0.001,
                    "Vanilla must apply the wolf armour's 11 armour through the body slot");
            CompanionStats stored = CompanionStats.of(pet.species(), CompanionSnapshot.of(pet));
            h.assertTrue(
                    Math.abs(stored.modified().get(2) - pet.getAttributeValue(Attributes.ARMOR)) < 0.001,
                    "Resting stats must include wolf armour");
            h.assertTrue(pet.hasAbility(CompanionAbilities.BEST_FRIEND), "Best Friend unlocks at bond five");
            var victim = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(6, 1, 4));
            var event = new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(victim, owner, 10);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            h.assertTrue(event.getDroppedExperience() == 15, "Best Friend adds half again to experience drops");
            finish(h, owner);
        });
    }

    private static void quokkaSpawns(GameTestHelper h) {
        var jungle = h.getLevel()
                .registryAccess()
                .getOrThrow(net.minecraft.world.level.biome.Biomes.JUNGLE)
                .value();
        var spawns = jungle.getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
        h.assertTrue(
                spawns.stream()
                        .anyMatch(entry -> entry.value().type() == KindredEntities.type(CompanionSpecies.QUOKKA)
                                && entry.weight() == 2
                                && entry.value().minCount() == 1
                                && entry.value().maxCount() == 3),
                "Jungles must load the weight-two Quokka biome modifier");
        h.assertTrue(
                new ItemStack(Items.FERN).is(CompanionSpecies.QUOKKA.tamingTag()),
                "Shrubs tag must resolve as taming food");
        h.assertTrue(
                new ItemStack(KindredItems.QUOKKA_SNACK.get()).is(CompanionSpecies.QUOKKA.equipmentTag()),
                "Snack must resolve as Quokka equipment");
        h.assertTrue(CompanionSpecies.QUOKKA.storageSlots() == 27, "Quokka has three storage rows");
        h.succeed();
    }

    private static void quokkaTame(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity wild =
                h.spawnWithNoFreeWill(KindredEntities.type(CompanionSpecies.QUOKKA), new BlockPos(4, 1, 4));
        double chance = KindredConfig.COMMON.tamingChance.get();
        try {
            KindredConfig.COMMON.tamingChance.set(1.0);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.FERN));
            wild.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(wild.isOwnedBy(owner), "Using a shrub tames the wild Quokka through its normal interaction");
            owner.setItemInHand(
                    net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(KindredItems.QUOKKA_SNACK.get()));
            wild.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(
                    wild.hasEquipment(KindredItems.QUOKKA_SNACK.get()), "Owner can equip the snack by interaction");
            owner.setItemInHand(
                    net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(KindredItems.BATTERY.get()));
            wild.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(
                    wild.hasEquipment(KindredItems.QUOKKA_SNACK.get()), "Equipment from another species is rejected");
        } finally {
            KindredConfig.COMMON.tamingChance.set(chance);
        }
        finish(h, owner);
    }

    private static void quokkaAgeTicks(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity quokka = pet(h, CompanionSpecies.QUOKKA, owner, true);
        quokka.addExperience(Integer.MAX_VALUE);
        quokka.setDisabledAbilityNames(List.of("always_happy"));
        var calf = h.spawn(EntityType.COW, new BlockPos(5, 1, 4));
        var locked = h.spawn(EntityType.COW, new BlockPos(6, 1, 4));
        calf.setAge(-1000);
        locked.setAge(-1000);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_DANDELION));
        locked.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(locked.isAgeLocked(), "Fixture golden dandelion must lock the calf's age");
        h.runAfterDelay(1, () -> {
            int age = calf.getAge();
            int lockedAge = locked.getAge();
            quokka.setDisabledAbilityNames(List.of());
            h.runAfterDelay(10, () -> {
                h.assertTrue(
                        calf.getAge() == age + 30, "Ten world ticks must advance baby growth by exactly thirty ticks");
                h.assertTrue(locked.getAge() == lockedAge, "Always Happy respects golden-dandelion age locks");
                finish(h, owner);
            });
        });
    }

    private static void particleCodecs(GameTestHelper h) {
        var codec = net.minecraft.core.particles.ParticleTypes.CODEC;
        var stream = net.minecraft.core.particles.ParticleTypes.STREAM_CODEC;
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            for (var effect : KindredParticles.effects()) {
                var particle = effect.type().get();
                var json = codec.encodeStart(ops, particle).getOrThrow();
                h.assertTrue(
                        codec.parse(ops, json).getOrThrow().getType() == particle,
                        "Custom particle must survive save/command codec: "
                                + effect.type().getId());
                stream.encode(buffer, particle);
                h.assertTrue(
                        stream.decode(buffer).getType() == particle && buffer.readableBytes() == 0,
                        "Custom particle must survive server-to-client serialization: "
                                + effect.type().getId());
            }
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    private static void soundPackets(GameTestHelper h) {
        var registry = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT;
        for (var species : CompanionSpecies.values()) {
            var sounds = species.sounds();
            for (var supplier : List.of(
                    sounds.ambient(),
                    sounds.hurt(),
                    sounds.death(),
                    sounds.attack(),
                    sounds.specialAttack(),
                    sounds.interact())) {
                var sound = supplier.get();
                h.assertTrue(
                        sound.location().getNamespace().equals(KindredSpirits.MOD_ID)
                                && registry.containsKey(sound.location()),
                        species + " must resolve registered custom sounds after server startup");
            }
        }
        var codec = net.minecraft.network.protocol.game.ClientboundSoundPacket.STREAM_CODEC;
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            for (var holder : KindredSounds.SOUND_EVENTS.getEntries()) {
                var packet = new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        holder, net.minecraft.sounds.SoundSource.NEUTRAL, 4.5, 2.0, -3.25, 0.7f, 1.0f, 42L);
                codec.encode(buffer, packet);
                var decoded = codec.decode(buffer);
                h.assertTrue(
                        decoded.getSound().value() == holder.get() && buffer.readableBytes() == 0,
                        "Sound event must survive server-to-client registry serialization: " + holder.getId());
                h.assertTrue(
                        decoded.getX() == 4.5
                                && decoded.getY() == 2.0
                                && decoded.getZ() == -3.25
                                && decoded.getVolume() == 0.7f
                                && decoded.getPitch() == 1.0f,
                        "Positional audio and playback levels must survive the sound packet");
            }
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    private static void dragonParticles(GameTestHelper h) {
        ServerPlayer owner = player(h);
        CompanionEntity dragon = pet(h, CompanionSpecies.BABY_DRAGON, owner, true);
        var target = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(7, 1, 4));
        dragon.setTarget(target);
        CompanionAbilities.DRAGON_BREATH.serverTick(dragon, owner);
        var clouds = h.getLevel()
                .getEntitiesOfClass(
                        AreaEffectCloud.class, target.getBoundingBox().inflate(3));
        h.assertTrue(
                clouds.size() == 1 && clouds.getFirst().getParticle().getType() == KindredParticles.DRAGON_SMOKE.get(),
                "Dragon Breath must create its custom animated smoke pool");
        var cloud = clouds.getFirst();
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        cloud.save(output);
        h.assertTrue(
                !output.buildResult().contains("potion_contents"),
                "The custom pool stays visual-only, with no potion effects");
        var loaded = EntityType.AREA_EFFECT_CLOUD.create(h.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(
                TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(
                loaded.getParticle().getType() == KindredParticles.DRAGON_SMOKE.get(),
                "Custom cloud appearance survives saving");
        finish(h, owner);
    }
}
