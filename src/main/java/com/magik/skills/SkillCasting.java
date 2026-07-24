package com.magik.skills;

import net.minecraft.server.level.ServerPlayer;

/**
 * The Vanilla+ progression skills are passive (they hook mining, farming,
 * combat, ...), so there is no active-cast pipeline anymore. This remains as a
 * harmless entry point for the (now unused) skill-slot cast packet.
 */
public final class SkillCasting {

    private SkillCasting() {
    }

    public static void cast(ServerPlayer player, String skillId) {
        // No active skills in the progression system.
    }
}
