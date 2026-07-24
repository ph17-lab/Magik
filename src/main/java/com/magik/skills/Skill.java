package com.magik.skills;

import com.magik.config.SkillBalance;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Immutable definition of a progression-tree node. Every skill requires:
 * a completed {@link #getQuestId() quest}, its prerequisite node, a minimum
 * level and a number of skill points ({@link #getPointCost()}).
 *
 * <p>Effects are read by the relevant gameplay hooks (mining, farming, combat,
 * ...). New skills are declared in {@link SkillTrees}.</p>
 */
public final class Skill {

    public enum Type {PASSIVE, ACTIVE}

    private final String id;
    private final SkillTrees.Tree tree;
    private final Type type;
    private final int tier;
    private final int branch;
    @Nullable
    private final String prerequisite;
    @Nullable
    private final String questId;
    private final int requiredLevel;
    private final int pointCost;

    Skill(String id, SkillTrees.Tree tree, Type type, int tier, int branch,
          @Nullable String prerequisite, @Nullable String questId, int requiredLevel, int pointCost) {
        this.id = id;
        this.tree = tree;
        this.type = type;
        this.tier = tier;
        this.branch = branch;
        this.prerequisite = prerequisite;
        this.questId = questId;
        this.requiredLevel = requiredLevel;
        this.pointCost = pointCost;
    }

    public String getId() {
        return id;
    }

    public SkillTrees.Tree getTree() {
        return tree;
    }

    public Type getType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    public int getBranch() {
        return branch;
    }

    @Nullable
    public String getPrerequisite() {
        return prerequisite;
    }

    @Nullable
    public String getQuestId() {
        return questId;
    }

    public int getRequiredLevel() {
        return SkillBalance.requiredLevel(id, requiredLevel);
    }

    public int getPointCost() {
        return Math.max(1, pointCost);
    }

    public Component getDisplayName() {
        return Component.translatable("skill.magik." + id);
    }

    public Component getDescription() {
        return Component.translatable("skill.magik." + id + ".desc");
    }
}
