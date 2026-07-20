package com.magik.player;

import com.magik.skills.SkillTrees;
import net.minecraft.network.chat.Component;

/**
 * The free class system's progression titles. A player has no fixed class:
 * the title is derived from their level and the tree they invested in most.
 *
 * <ul>
 *   <li>Level &lt; 10: Aprendiz</li>
 *   <li>Level &gt;= 80: Lenda</li>
 *   <li>Otherwise, by dominant tree: Espadachim/Pesado -&gt; Guerreiro
 *       (Cavaleiro at 40+), Arcano -&gt; Mago (Arquimago at 40+),
 *       Arqueiro -&gt; Arqueiro Mestre, Defesa -&gt; Guardião.</li>
 * </ul>
 */
public final class PlayerTitles {

    private PlayerTitles() {
    }

    public static Component getTitle(PlayerRpg rpg) {
        return Component.translatable("title.magik." + getTitleKey(rpg));
    }

    public static String getTitleKey(PlayerRpg rpg) {
        int level = rpg.getLevel();
        if (level < 10) {
            return "apprentice";
        }
        if (level >= 80) {
            return "legend";
        }

        SkillTrees.Tree dominant = null;
        int best = 0;
        for (SkillTrees.Tree tree : SkillTrees.Tree.values()) {
            int invested = SkillTrees.unlockedInTree(rpg, tree);
            if (invested > best) {
                best = invested;
                dominant = tree;
            }
        }

        boolean veteran = level >= 40;
        if (dominant == null) {
            return "warrior";
        }
        return switch (dominant) {
            case ARCANE -> veteran ? "archmage" : "mage";
            case ARCHER -> "master_archer";
            case DEFENSE -> "guardian";
            case SWORDSMAN, HEAVY -> veteran ? "knight" : "warrior";
        };
    }
}
