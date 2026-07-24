package com.magik.player;

import com.magik.skills.SkillTrees;
import net.minecraft.network.chat.Component;

/**
 * Progression titles. There are no classes: a player's title is derived from
 * their level and the tree they have invested in the most.
 */
public final class PlayerTitles {

    private PlayerTitles() {
    }

    public static Component getTitle(PlayerRpg rpg) {
        return Component.translatable("title.magik." + getTitleKey(rpg));
    }

    public static String getTitleKey(PlayerRpg rpg) {
        int level = rpg.getLevel();
        if (level < 5) {
            return "novice";
        }
        if (level >= 50) {
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
        if (dominant == null) {
            return "adventurer";
        }
        return switch (dominant) {
            case WARRIOR -> "warrior";
            case MINER -> "miner";
            case FARMER -> "farmer";
            case LUMBERJACK -> "lumberjack";
            case BUILDER -> "builder";
            case FISHER -> "fisher";
            case EXPLORER -> "explorer";
            case ARCANE -> "arcanist";
        };
    }
}
