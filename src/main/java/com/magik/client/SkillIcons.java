package com.magik.client;

import com.magik.MagikMod;
import com.magik.skills.SkillTrees;
import net.minecraft.resources.ResourceLocation;

/**
 * Skill icon textures (64x64, under {@code textures/gui/skills/<id>.png}),
 * shared by the HUD skill slots and the skill tree screen.
 */
public final class SkillIcons {

    public static final int SIZE = 64;

    private SkillIcons() {
    }

    public static ResourceLocation get(String skillId) {
        if (SkillTrees.get(skillId) == null) {
            skillId = "w_power";
        }
        return MagikMod.id("textures/gui/skills/" + skillId + ".png");
    }
}
