package com.magik.skills;

import com.magik.network.MagikNetwork;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Quest identifiers plus helpers to complete them. Each skill is gated behind a
 * quest; quests are detected automatically by {@link com.magik.progression.QuestEvents}
 * and follow the natural flow of vanilla progression.
 */
public final class Quests {

    // Warrior
    public static final String CRAFT_SWORD = "craft_sword";
    public static final String CRAFT_SHIELD = "craft_shield";
    public static final String CRAFT_ANVIL = "craft_anvil";
    public static final String KILL_10 = "kill_10";
    public static final String KILL_25 = "kill_25";
    public static final String KILL_50 = "kill_50";
    public static final String KILL_100 = "kill_100";
    public static final String TAKE_DAMAGE = "take_damage";
    public static final String BLOCK_DAMAGE = "block_damage";

    // Miner
    public static final String MINE_STONE = "mine_stone";
    public static final String MINE_COAL = "mine_coal";
    public static final String MINE_IRON = "mine_iron";
    public static final String MINE_DIAMOND = "mine_diamond";
    public static final String MINE_OBSIDIAN = "mine_obsidian";
    public static final String CRAFT_PICKAXE = "craft_pickaxe";

    // Farmer
    public static final String PLANT_WHEAT = "plant_wheat";
    public static final String HARVEST_WHEAT = "harvest_wheat";
    public static final String MAKE_BREAD = "make_bread";
    public static final String BREED_ANIMALS = "breed_animals";

    // Lumberjack
    public static final String CHOP_TREE = "chop_tree";
    public static final String CHOP_50_LOGS = "chop_50_logs";
    public static final String CRAFT_AXE = "craft_axe";

    // Builder
    public static final String PLACE_100 = "place_100";
    public static final String PLACE_500 = "place_500";
    public static final String CRAFT_SCAFFOLD = "craft_scaffold";

    // Fisher
    public static final String CATCH_FISH = "catch_fish";
    public static final String CATCH_TREASURE = "catch_treasure";
    public static final String CATCH_25_FISH = "catch_25_fish";
    public static final String CRAFT_ROD = "craft_rod";

    // Explorer
    public static final String DISCOVER_BIOME = "discover_biome";
    public static final String DISCOVER_3_BIOMES = "discover_3_biomes";
    public static final String DISCOVER_5_BIOMES = "discover_5_biomes";
    public static final String DISCOVER_STRUCTURE = "discover_structure";
    public static final String DISCOVER_2_STRUCTURES = "discover_2_structures";
    public static final String CLIMB_HIGH = "climb_high";
    public static final String CRAFT_COMPASS = "craft_compass";

    // Arcane (Iron's Spells)
    public static final String IRONS_SPELLBOOK = "irons_spellbook";
    public static final String IRONS_LEARN_SPELL = "irons_learn_spell";
    public static final String IRONS_SCROLL = "irons_scroll";
    public static final String IRONS_INK = "irons_ink";

    private Quests() {
    }

    /** Completes a quest for the player (idempotent), announcing it once. */
    public static void complete(ServerPlayer player, String questId) {
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            if (rpg.completeQuest(questId)) {
                announce(player, questId);
                MagikNetwork.syncFull(player, rpg);
            }
        });
    }

    /** Increments a counting quest; completes it when the threshold is reached. */
    public static void progress(ServerPlayer player, String questId, int threshold) {
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            if (rpg.hasCompletedQuest(questId)) {
                return;
            }
            int value = rpg.addQuestProgress(questId, 1);
            if (value >= threshold) {
                rpg.completeQuest(questId);
                announce(player, questId);
            }
            MagikNetwork.syncFull(player, rpg);
        });
    }

    private static void announce(ServerPlayer player, String questId) {
        player.displayClientMessage(Component.translatable("quest.magik.complete",
                Component.translatable("quest.magik." + questId)).withStyle(ChatFormatting.GOLD), false);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.5F);
    }
}
