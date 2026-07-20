package com.magik.skills;

import com.magik.player.PlayerRpg;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Static registry of every skill in the mod, organized in five free-form
 * "class" trees. There are no locked classes: any player can invest in any
 * tree, building their own hybrid build. Titles (Aprendiz ... Lenda) are
 * derived from level and tree investment in {@link com.magik.player.PlayerTitles}.
 *
 * <p>Each tree has two branches of four tiers. Unlocking a node requires a
 * skill point, a minimum level and the previous node of its branch.</p>
 *
 * <p>To add a new skill: one {@code add(...)} line here, lang entries, an icon
 * texture under {@code textures/gui/skills/<id>.png}, and its effect (in
 * {@link SkillCasting} for actives, or the relevant hook for passives).</p>
 */
public final class SkillTrees {

    /** The five archetype trees of the free class system. */
    public enum Tree {
        ARCANE, SWORDSMAN, ARCHER, HEAVY, DEFENSE;

        public Component getDisplayName() {
            return Component.translatable("skilltree.magik." + name().toLowerCase(Locale.ROOT));
        }
    }

    // Arcano - all actives, all mana.
    public static final String FIREBALL = "fireball";
    public static final String LIGHTNING = "lightning";
    public static final String ICE_SHARD = "ice_shard";
    public static final String ARCANE_EXPLOSION = "arcane_explosion";
    public static final String HEAL = "heal";
    public static final String ARCANE_SHIELD = "arcane_shield";
    public static final String SHORT_TELEPORT = "short_teleport";
    public static final String WEAPON_SUMMON = "weapon_summon";

    // Espadachim - stamina.
    public static final String SWORD_COMBO = "sword_combo";
    public static final String SPIN_SLASH = "spin_slash";
    public static final String CHARGED_STRIKE = "charged_strike";
    public static final String AREA_SLASH = "area_slash";
    public static final String DASH = "dash";
    public static final String COUNTER_ATTACK = "counter_attack";
    public static final String SWORD_CRIT = "sword_crit";
    public static final String SWORD_MASTER = "sword_master";

    // Arqueiro - stamina.
    public static final String ZOOM = "zoom";
    public static final String EXPLOSIVE_ARROW = "explosive_arrow";
    public static final String FROST_ARROW = "frost_arrow";
    public static final String ELECTRIC_ARROW = "electric_arrow";
    public static final String LONG_RANGE = "long_range";
    public static final String RAPID_FIRE = "rapid_fire";
    public static final String BOW_CRIT = "bow_crit";
    public static final String MULTI_ARROW = "multi_arrow";

    // Armamento Pesado - stamina.
    public static final String HEAVY_BLADES = "heavy_blades";
    public static final String GIANT_CLEAVE = "giant_cleave";
    public static final String HAMMER_SLAM = "hammer_slam";
    public static final String TOTAL_DESTRUCTION = "total_destruction";
    public static final String BRUTAL_FORCE = "brutal_force";
    public static final String SHIELD_BREAK = "shield_break";
    public static final String STUNNING_BLOWS = "stunning_blows";
    public static final String AREA_STRIKE = "area_strike";

    // Defesa - stamina.
    public static final String REINFORCED_LIFE = "reinforced_life";
    public static final String DAMAGE_REDUCTION = "damage_reduction";
    public static final String REGENERATION = "regeneration";
    public static final String UNBREAKABLE_FORTRESS = "unbreakable_fortress";
    public static final String IMPROVED_BLOCK = "improved_block";
    public static final String FIRE_RESISTANCE = "fire_resistance";
    public static final String BLAST_RESISTANCE = "blast_resistance";
    public static final String PROTECTIVE_SHIELD = "protective_shield";

    /** Minimum player level per tier (0..3). */
    private static final int[] TIER_LEVELS = {2, 6, 12, 20};

    private static final Map<String, Skill> SKILLS = new LinkedHashMap<>();

    static {
        // --- Arcano ---
        branch(Tree.ARCANE, 0,
                active(FIREBALL, 25, 0, 80),
                active(LIGHTNING, 30, 0, 160),
                active(ICE_SHARD, 25, 0, 100),
                active(ARCANE_EXPLOSION, 45, 0, 300));
        branch(Tree.ARCANE, 1,
                active(HEAL, 35, 0, 300),
                active(ARCANE_SHIELD, 40, 0, 500),
                active(SHORT_TELEPORT, 30, 0, 160),
                active(WEAPON_SUMMON, 60, 0, 900));

        // --- Espadachim ---
        branch(Tree.SWORDSMAN, 0,
                active(SWORD_COMBO, 0, 25, 300),
                active(SPIN_SLASH, 0, 30, 160),
                active(CHARGED_STRIKE, 0, 35, 240),
                active(AREA_SLASH, 0, 40, 300));
        branch(Tree.SWORDSMAN, 1,
                active(DASH, 0, 25, 40),
                active(COUNTER_ATTACK, 0, 20, 300),
                passive(SWORD_CRIT),
                passive(SWORD_MASTER));

        // --- Arqueiro ---
        branch(Tree.ARCHER, 0,
                passive(ZOOM),
                active(EXPLOSIVE_ARROW, 0, 30, 240),
                active(FROST_ARROW, 0, 25, 200),
                active(ELECTRIC_ARROW, 0, 30, 240));
        branch(Tree.ARCHER, 1,
                passive(LONG_RANGE),
                passive(RAPID_FIRE),
                passive(BOW_CRIT),
                active(MULTI_ARROW, 0, 35, 300));

        // --- Armamento Pesado ---
        branch(Tree.HEAVY, 0,
                passive(HEAVY_BLADES),
                active(GIANT_CLEAVE, 0, 35, 240),
                active(HAMMER_SLAM, 0, 40, 400),
                active(TOTAL_DESTRUCTION, 0, 60, 900));
        branch(Tree.HEAVY, 1,
                passive(BRUTAL_FORCE),
                passive(SHIELD_BREAK),
                passive(STUNNING_BLOWS),
                active(AREA_STRIKE, 0, 40, 300));

        // --- Defesa ---
        branch(Tree.DEFENSE, 0,
                passive(REINFORCED_LIFE),
                passive(DAMAGE_REDUCTION),
                passive(REGENERATION),
                passive(UNBREAKABLE_FORTRESS));
        branch(Tree.DEFENSE, 1,
                passive(IMPROVED_BLOCK),
                passive(FIRE_RESISTANCE),
                passive(BLAST_RESISTANCE),
                active(PROTECTIVE_SHIELD, 0, 50, 800));
    }

    // --- Declaration helpers -------------------------------------------------

    private record Proto(String id, Skill.Type type, float mana, float stamina, int cooldown) {
    }

    private static Proto active(String id, float mana, float stamina, int cooldown) {
        return new Proto(id, Skill.Type.ACTIVE, mana, stamina, cooldown);
    }

    private static Proto passive(String id) {
        return new Proto(id, Skill.Type.PASSIVE, 0, 0, 0);
    }

    /** Registers a 4-tier branch where each node requires the previous one. */
    private static void branch(Tree tree, int branchIndex, Proto... protos) {
        String previous = null;
        for (int tier = 0; tier < protos.length; tier++) {
            Proto proto = protos[tier];
            SKILLS.put(proto.id(), new Skill(proto.id(), tree, proto.type(), tier, branchIndex,
                    previous, TIER_LEVELS[Math.min(tier, TIER_LEVELS.length - 1)],
                    proto.mana(), proto.stamina(), proto.cooldown()));
            previous = proto.id();
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

    /** Number of skills the player unlocked in a given tree. */
    public static int unlockedInTree(PlayerRpg rpg, Tree tree) {
        int count = 0;
        for (Skill skill : SKILLS.values()) {
            if (skill.getTree() == tree && rpg.hasSkill(skill.getId())) {
                count++;
            }
        }
        return count;
    }

    /** Server-authoritative check whether a player may unlock a skill right now. */
    public static boolean canUnlock(PlayerRpg rpg, Skill skill) {
        if (rpg.hasSkill(skill.getId()) || rpg.getSkillPoints() <= 0) {
            return false;
        }
        if (rpg.getLevel() < skill.getRequiredLevel()) {
            return false;
        }
        return skill.getPrerequisite() == null || rpg.hasSkill(skill.getPrerequisite());
    }
}
