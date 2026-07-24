package com.magik.progression;

import com.magik.MagikMod;
import com.magik.skills.Quests;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Automatic quest detection: hooks vanilla-style actions (crafting, mining,
 * farming, chopping, building, fishing, killing, exploring) and completes the
 * matching quest, which then unlocks the paired skill for purchase.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class QuestEvents {

    private static final Map<UUID, ResourceLocation> LAST_BIOME = new WeakHashMap<>();
    private static final Map<UUID, Boolean> IN_STRUCTURE = new WeakHashMap<>();

    private QuestEvents() {
    }

    private static String path(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null ? id.getPath() : "";
    }

    private static String path(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null ? id.getPath() : "";
    }

    // ------------------------------------------------------------------
    // Crafting
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String p = path(event.getCrafting().getItem());
        if (p.endsWith("sword")) {
            Quests.complete(player, Quests.CRAFT_SWORD);
        }
        if (p.endsWith("shield")) {
            Quests.complete(player, Quests.CRAFT_SHIELD);
        }
        if (p.equals("anvil")) {
            Quests.complete(player, Quests.CRAFT_ANVIL);
        }
        if (p.endsWith("pickaxe")) {
            Quests.complete(player, Quests.CRAFT_PICKAXE);
        }
        if (p.endsWith("axe") && !p.endsWith("pickaxe")) {
            Quests.complete(player, Quests.CRAFT_AXE);
        }
        if (p.endsWith("fishing_rod")) {
            Quests.complete(player, Quests.CRAFT_ROD);
        }
        if (p.equals("compass") || p.equals("recovery_compass")) {
            Quests.complete(player, Quests.CRAFT_COMPASS);
        }
        if (p.equals("scaffolding")) {
            Quests.complete(player, Quests.CRAFT_SCAFFOLD);
        }
        if (p.contains("spell_book")) {
            Quests.complete(player, Quests.IRONS_SPELLBOOK);
        }
        if (p.contains("scroll")) {
            Quests.complete(player, Quests.IRONS_SCROLL);
        }
        if (p.contains("ink")) {
            Quests.complete(player, Quests.IRONS_INK);
        }
    }

    // ------------------------------------------------------------------
    // Mining / chopping / harvesting
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        String p = path(state.getBlock());

        if (p.contains("coal_ore")) {
            Quests.complete(player, Quests.MINE_COAL);
        } else if (p.contains("iron_ore")) {
            Quests.complete(player, Quests.MINE_IRON);
        } else if (p.contains("diamond_ore")) {
            Quests.complete(player, Quests.MINE_DIAMOND);
        } else if (p.equals("obsidian") || p.equals("crying_obsidian")) {
            Quests.complete(player, Quests.MINE_OBSIDIAN);
        } else if (p.equals("stone") || p.equals("cobblestone") || p.equals("deepslate")
                || p.equals("cobbled_deepslate")) {
            Quests.complete(player, Quests.MINE_STONE);
        }

        if (p.endsWith("_log") || p.endsWith("_stem") || p.endsWith("_wood")) {
            Quests.progress(player, Quests.CHOP_TREE, 1);
            Quests.progress(player, Quests.CHOP_50_LOGS, 50);
        }

        // Fully grown wheat.
        if (p.equals("wheat")) {
            Quests.complete(player, Quests.HARVEST_WHEAT);
        }
    }

    // ------------------------------------------------------------------
    // Building / planting
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String p = path(event.getPlacedBlock().getBlock());
        if (p.equals("wheat") || p.contains("wheat")) {
            Quests.complete(player, Quests.PLANT_WHEAT);
        }
        Quests.progress(player, Quests.PLACE_100, 100);
        Quests.progress(player, Quests.PLACE_500, 500);
    }

    // ------------------------------------------------------------------
    // Combat
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof LivingEntity) {
            Quests.progress(player, Quests.KILL_10, 10);
            Quests.progress(player, Quests.KILL_25, 25);
            Quests.progress(player, Quests.KILL_50, 50);
            Quests.progress(player, Quests.KILL_100, 100);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Quests.complete(player, Quests.TAKE_DAMAGE);
            if (player.isBlocking()) {
                Quests.complete(player, Quests.BLOCK_DAMAGE);
            }
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            Quests.complete(player, Quests.BREED_ANIMALS);
        }
    }

    // ------------------------------------------------------------------
    // Fishing
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Quests.progress(player, Quests.CATCH_FISH, 1);
        Quests.progress(player, Quests.CATCH_25_FISH, 25);
        for (ItemStack drop : event.getDrops()) {
            String p = path(drop.getItem());
            if (p.contains("enchanted_book") || p.contains("name_tag") || p.contains("nautilus")
                    || p.contains("saddle") || p.contains("bow")) {
                Quests.complete(player, Quests.CATCH_TREASURE);
            }
        }
    }

    // ------------------------------------------------------------------
    // Exploration (biome / structure / altitude), throttled
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (gameTime % 20 != 0) {
            return;
        }
        UUID uuid = player.getUUID();
        BlockPos pos = player.blockPosition();

        if (pos.getY() >= 200) {
            Quests.complete(player, Quests.CLIMB_HIGH);
        }

        ResourceLocation biome = player.level().getBiome(pos).unwrapKey()
                .map(k -> k.location()).orElse(null);
        if (biome != null && !biome.equals(LAST_BIOME.get(uuid))) {
            LAST_BIOME.put(uuid, biome);
            Quests.progress(player, Quests.DISCOVER_BIOME, 1);
            Quests.progress(player, Quests.DISCOVER_3_BIOMES, 3);
            Quests.progress(player, Quests.DISCOVER_5_BIOMES, 5);
        }

        if (player.level() instanceof ServerLevel serverLevel && gameTime % 40 == 0) {
            boolean inStructure = !serverLevel.structureManager().getAllStructuresAt(pos).isEmpty();
            boolean was = IN_STRUCTURE.getOrDefault(uuid, false);
            if (inStructure && !was) {
                Quests.progress(player, Quests.DISCOVER_STRUCTURE, 1);
                Quests.progress(player, Quests.DISCOVER_2_STRUCTURES, 2);
            }
            IN_STRUCTURE.put(uuid, inStructure);
        }

        // Iron's Spells: holding a spellbook counts as learning magic.
        for (ItemStack hand : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            String p = path(hand.getItem());
            if (p.contains("spell_book")) {
                Quests.complete(player, Quests.IRONS_SPELLBOOK);
                Quests.complete(player, Quests.IRONS_LEARN_SPELL);
            }
        }
    }
}
