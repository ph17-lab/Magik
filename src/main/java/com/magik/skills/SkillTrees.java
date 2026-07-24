package com.magik.skills;

import com.magik.player.PlayerRpg;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Vanilla+ progression trees. There are no classes and no restrictions:
 * the player can invest in every tree at once, shaping their own build.
 *
 * <p>Each node requires a completed {@link Quests quest}, its prerequisite,
 * a minimum level and skill points. The <b>Arcane</b> tree only exists when
 * Iron's Spells 'n Spellbooks is installed.</p>
 */
public final class SkillTrees {

    /** The eight progression trees. */
    public enum Tree {
        WARRIOR, MINER, FARMER, LUMBERJACK, BUILDER, FISHER, EXPLORER, ARCANE;

        public Component getDisplayName() {
            return Component.translatable("skilltree.magik." + name().toLowerCase(Locale.ROOT));
        }

        /** Arcane is hidden unless Iron's Spells 'n Spellbooks is present. */
        public boolean isAvailable() {
            return this != ARCANE || ModList.get().isLoaded("irons_spellbooks");
        }
    }

    // ---- Warrior (vanilla combat only) ----
    public static final String W_POWER = "w_power";
    public static final String W_CRIT = "w_crit";
    public static final String W_SPEED = "w_speed";
    public static final String W_LIFESTEAL = "w_lifesteal";
    public static final String W_EXECUTE = "w_execute";
    public static final String W_TOUGH = "w_tough";
    public static final String W_BLOCK = "w_block";
    public static final String W_PARRY = "w_parry";
    public static final String W_DURABILITY = "w_durability";
    public static final String W_BERSERK = "w_berserk";

    // ---- Miner ----
    public static final String MI_SPEED = "mi_speed";
    public static final String MI_XP = "mi_xp";
    public static final String MI_FORTUNE = "mi_fortune";
    public static final String MI_DOUBLE = "mi_double";
    public static final String MI_DETECTOR = "mi_detector";
    public static final String MI_DURABILITY = "mi_durability";
    public static final String MI_REACH = "mi_reach";

    // ---- Farmer ----
    public static final String FA_GROWTH = "fa_growth";
    public static final String FA_REPLANT = "fa_replant";
    public static final String FA_AREA = "fa_area";
    public static final String FA_YIELD = "fa_yield";
    public static final String FA_BREED = "fa_breed";

    // ---- Lumberjack ----
    public static final String LU_SPEED = "lu_speed";
    public static final String LU_FELLER = "lu_feller";
    public static final String LU_YIELD = "lu_yield";
    public static final String LU_DURABILITY = "lu_durability";

    // ---- Builder ----
    public static final String BU_REACH = "bu_reach";
    public static final String BU_SPEED = "bu_speed";
    public static final String BU_SCAFFOLD = "bu_scaffold";

    // ---- Fisher ----
    public static final String FI_SPEED = "fi_speed";
    public static final String FI_TREASURE = "fi_treasure";
    public static final String FI_RARE = "fi_rare";
    public static final String FI_DURABILITY = "fi_durability";

    // ---- Explorer (unlocks tools, not stats) ----
    public static final String EX_MINIMAP = "ex_minimap";
    public static final String EX_ZOOM = "ex_zoom";
    public static final String EX_COORDS = "ex_coords";
    public static final String EX_DIRECTION = "ex_direction";
    public static final String EX_WAYPOINTS = "ex_waypoints";
    public static final String EX_FULLMAP = "ex_fullmap";
    public static final String EX_BIOME_COMPASS = "ex_biome_compass";
    public static final String EX_STRUCT_COMPASS = "ex_struct_compass";

    // ---- Arcane (Iron's Spells) ----
    public static final String AR_MANA = "ar_mana";
    public static final String AR_MANA_REGEN = "ar_mana_regen";
    public static final String AR_POWER = "ar_power";
    public static final String AR_COOLDOWN = "ar_cooldown";
    public static final String AR_CAST_SPEED = "ar_cast_speed";
    public static final String AR_RESIST = "ar_resist";

    private static final Map<String, Skill> SKILLS = new LinkedHashMap<>();

    static {
        // ============================ WARRIOR ============================
        line(Tree.WARRIOR, 0,
                node(W_POWER, 2, 1, Quests.CRAFT_SWORD),
                node(W_CRIT, 6, 1, Quests.KILL_10),
                node(W_LIFESTEAL, 12, 2, Quests.KILL_25),
                node(W_EXECUTE, 20, 2, Quests.KILL_50),
                node(W_BERSERK, 30, 3, Quests.KILL_100));
        line(Tree.WARRIOR, 1,
                node(W_SPEED, 3, 1, Quests.CRAFT_SWORD),
                node(W_TOUGH, 8, 1, Quests.TAKE_DAMAGE),
                node(W_BLOCK, 12, 2, Quests.CRAFT_SHIELD),
                node(W_PARRY, 22, 2, Quests.BLOCK_DAMAGE),
                node(W_DURABILITY, 16, 2, Quests.CRAFT_ANVIL));

        // ============================ MINER ==============================
        line(Tree.MINER, 0,
                node(MI_SPEED, 2, 1, Quests.MINE_STONE),
                node(MI_XP, 5, 1, Quests.MINE_COAL),
                node(MI_FORTUNE, 10, 2, Quests.MINE_IRON),
                node(MI_DOUBLE, 18, 3, Quests.MINE_DIAMOND));
        line(Tree.MINER, 1,
                node(MI_DURABILITY, 4, 1, Quests.CRAFT_PICKAXE),
                node(MI_DETECTOR, 12, 2, Quests.MINE_IRON),
                node(MI_REACH, 20, 2, Quests.MINE_OBSIDIAN));

        // ============================ FARMER =============================
        line(Tree.FARMER, 0,
                node(FA_GROWTH, 2, 1, Quests.PLANT_WHEAT),
                node(FA_REPLANT, 6, 1, Quests.HARVEST_WHEAT),
                node(FA_AREA, 12, 2, Quests.MAKE_BREAD));
        line(Tree.FARMER, 1,
                node(FA_YIELD, 5, 1, Quests.HARVEST_WHEAT),
                node(FA_BREED, 10, 2, Quests.BREED_ANIMALS));

        // ========================== LUMBERJACK ===========================
        line(Tree.LUMBERJACK, 0,
                node(LU_SPEED, 2, 1, Quests.CHOP_TREE),
                node(LU_FELLER, 8, 2, Quests.CHOP_50_LOGS),
                node(LU_YIELD, 14, 2, Quests.CHOP_TREE));
        line(Tree.LUMBERJACK, 1,
                node(LU_DURABILITY, 4, 1, Quests.CRAFT_AXE));

        // ============================ BUILDER ============================
        line(Tree.BUILDER, 0,
                node(BU_REACH, 3, 1, Quests.PLACE_100),
                node(BU_SPEED, 8, 2, Quests.PLACE_500),
                node(BU_SCAFFOLD, 14, 2, Quests.CRAFT_SCAFFOLD));

        // ============================ FISHER =============================
        line(Tree.FISHER, 0,
                node(FI_SPEED, 2, 1, Quests.CATCH_FISH),
                node(FI_TREASURE, 8, 2, Quests.CATCH_TREASURE),
                node(FI_RARE, 14, 2, Quests.CATCH_25_FISH));
        line(Tree.FISHER, 1,
                node(FI_DURABILITY, 4, 1, Quests.CRAFT_ROD));

        // =========================== EXPLORER ============================
        line(Tree.EXPLORER, 0,
                node(EX_MINIMAP, 3, 1, Quests.DISCOVER_BIOME),
                node(EX_COORDS, 6, 1, Quests.DISCOVER_3_BIOMES),
                node(EX_WAYPOINTS, 12, 2, Quests.DISCOVER_STRUCTURE),
                node(EX_FULLMAP, 20, 3, Quests.DISCOVER_5_BIOMES));
        line(Tree.EXPLORER, 1,
                node(EX_ZOOM, 5, 1, Quests.CLIMB_HIGH),
                node(EX_DIRECTION, 8, 1, Quests.CRAFT_COMPASS),
                node(EX_BIOME_COMPASS, 15, 2, Quests.DISCOVER_3_BIOMES),
                node(EX_STRUCT_COMPASS, 24, 3, Quests.DISCOVER_2_STRUCTURES));

        // ============================ ARCANE =============================
        line(Tree.ARCANE, 0,
                node(AR_MANA, 5, 1, Quests.IRONS_SPELLBOOK),
                node(AR_MANA_REGEN, 10, 2, Quests.IRONS_LEARN_SPELL),
                node(AR_POWER, 18, 3, Quests.IRONS_SCROLL));
        line(Tree.ARCANE, 1,
                node(AR_CAST_SPEED, 8, 1, Quests.IRONS_SPELLBOOK),
                node(AR_COOLDOWN, 14, 2, Quests.IRONS_LEARN_SPELL),
                node(AR_RESIST, 20, 2, Quests.IRONS_INK));
    }

    // --- Declaration helpers -------------------------------------------------

    private record Proto(String id, int level, int cost, String quest) {
    }

    private static Proto node(String id, int level, int cost, String quest) {
        return new Proto(id, level, cost, quest);
    }

    /** Registers a vertical branch where each node requires the previous one. */
    private static void line(Tree tree, int branch, Proto... protos) {
        String previous = null;
        for (int tier = 0; tier < protos.length; tier++) {
            Proto p = protos[tier];
            SKILLS.put(p.id(), new Skill(p.id(), tree, Skill.Type.PASSIVE, tier, branch,
                    previous, p.quest(), p.level(), p.cost()));
            previous = p.id();
        }
    }

    private SkillTrees() {
    }

    // --- Queries -------------------------------------------------------------

    @Nullable
    public static Skill get(String id) {
        return SKILLS.get(id);
    }

    public static Map<String, Skill> all() {
        return Collections.unmodifiableMap(SKILLS);
    }

    public static List<Skill> byTree(Tree tree) {
        return SKILLS.values().stream()
                .filter(skill -> skill.getTree() == tree)
                .collect(Collectors.toList());
    }

    public static int unlockedInTree(PlayerRpg rpg, Tree tree) {
        int count = 0;
        for (Skill skill : SKILLS.values()) {
            if (skill.getTree() == tree && rpg.hasSkill(skill.getId())) {
                count++;
            }
        }
        return count;
    }

    public static int branchCount(Tree tree) {
        int max = 0;
        for (Skill skill : SKILLS.values()) {
            if (skill.getTree() == tree) {
                max = Math.max(max, skill.getBranch());
            }
        }
        return max + 1;
    }

    /** Server-authoritative check whether a player may unlock a skill right now. */
    public static boolean canUnlock(PlayerRpg rpg, Skill skill) {
        if (rpg.hasSkill(skill.getId()) || !skill.getTree().isAvailable()) {
            return false;
        }
        if (rpg.getSkillPoints() < skill.getPointCost() || rpg.getLevel() < skill.getRequiredLevel()) {
            return false;
        }
        if (!rpg.hasCompletedQuest(skill.getQuestId())) {
            return false;
        }
        return skill.getPrerequisite() == null || rpg.hasSkill(skill.getPrerequisite());
    }
}
