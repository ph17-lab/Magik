package com.magik.skills;

import com.magik.config.SkillBalance;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Immutable definition of a skill tree node. Costs and cooldowns can be
 * overridden by server owners through the JSON balance file
 * ({@link SkillBalance}), which is consulted by the getters.
 *
 * <p>Actual effects are executed in {@link SkillCasting} (actives) or read by
 * the relevant game hooks (passives). New skills are added in {@link SkillTrees}.</p>
 */
public final class Skill {

    public enum Type {ACTIVE, PASSIVE}

    private final String id;
    private final SkillTrees.Tree tree;
    private final Type type;
    /** Vertical position inside the tree (0 = root). */
    private final int tier;
    /** Horizontal branch inside the tree (0 = left, 1 = right). */
    private final int branch;
    @Nullable
    private final String prerequisite;
    private final int requiredLevel;
    private final float manaCost;
    private final float staminaCost;
    private final int cooldownTicks;

    Skill(String id, SkillTrees.Tree tree, Type type, int tier, int branch, @Nullable String prerequisite,
          int requiredLevel, float manaCost, float staminaCost, int cooldownTicks) {
        this.id = id;
        this.tree = tree;
        this.type = type;
        this.tier = tier;
        this.branch = branch;
        this.prerequisite = prerequisite;
        this.requiredLevel = requiredLevel;
        this.manaCost = manaCost;
        this.staminaCost = staminaCost;
        this.cooldownTicks = cooldownTicks;
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

    public int getRequiredLevel() {
        return SkillBalance.requiredLevel(id, requiredLevel);
    }

    public float getManaCost() {
        return SkillBalance.manaCost(id, manaCost);
    }

    public float getStaminaCost() {
        return SkillBalance.staminaCost(id, staminaCost);
    }

    public int getCooldownTicks() {
        return SkillBalance.cooldownTicks(id, cooldownTicks);
    }

    public Component getDisplayName() {
        return Component.translatable("skill.magik." + id);
    }

    public Component getDescription() {
        return Component.translatable("skill.magik." + id + ".desc");
    }
}
