package com.magik.player;

import com.magik.config.MagikServerConfig;
import com.magik.config.SkillBalance;
import com.magik.skills.SkillTrees;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Every derived-stat formula of the mod in one documented place. Both the
 * server logic and the client HUD/tooltips read from here, so displayed
 * numbers always match the real effects. Attribute-based values stay, while
 * skill bonuses now come from the Vanilla+ progression trees.
 */
public final class RpgStats {

    private RpgStats() {
    }

    // --- Progression ---

    public static int xpForNextLevel(int level) {
        double base = MagikServerConfig.XP_CURVE_BASE.get();
        double exponent = MagikServerConfig.XP_CURVE_EXPONENT.get();
        return (int) Math.round(base * Math.pow(level, exponent));
    }

    // --- Mana / stamina (still used by the HUD and Arcane tree) ---

    public static float maxMana(PlayerRpg rpg, Player player) {
        return (float) (MagikServerConfig.BASE_MAX_MANA.get()
                + MagikServerConfig.MANA_PER_INTELLIGENCE.get() * rpg.getAttribute(RpgAttribute.INTELLIGENCE));
    }

    public static float manaRegenPerTick(PlayerRpg rpg) {
        float regen = 0.05F + 0.004F * rpg.getAttribute(RpgAttribute.INTELLIGENCE);
        return regen * SkillBalance.manaRegenMultiplier();
    }

    public static final int STAMINA_REGEN_DELAY_TICKS = 30;

    public static float maxStamina(PlayerRpg rpg) {
        return (float) (MagikServerConfig.BASE_MAX_STAMINA.get()
                + MagikServerConfig.STAMINA_PER_AGILITY.get() * rpg.getAttribute(RpgAttribute.AGILITY));
    }

    public static float staminaRegenPerTick(PlayerRpg rpg) {
        return 0.8F * (1.0F + 0.01F * rpg.getAttribute(RpgAttribute.AGILITY))
                * SkillBalance.staminaRegenMultiplier();
    }

    public static void clampResources(PlayerRpg rpg, Player player) {
        rpg.setMana(Mth.clamp(rpg.getMana(), 0.0F, maxMana(rpg, player)));
        rpg.setStamina(Mth.clamp(rpg.getStamina(), 0.0F, maxStamina(rpg)));
    }

    // --- Warrior (vanilla combat) ---

    public static final float CRIT_MULTIPLIER = 1.5F;
    public static final int OUT_OF_COMBAT_TICKS = 100;

    /** Flat extra melee damage multiplier from the Warrior tree. */
    public static float warriorDamageMultiplier(PlayerRpg rpg) {
        float m = 1.0F + 0.03F * rpg.getAttribute(RpgAttribute.STRENGTH);
        if (rpg.hasSkill(SkillTrees.W_POWER)) {
            m += 0.15F;
        }
        return m;
    }

    /** Critical chance [0..0.75]. */
    public static float critChance(PlayerRpg rpg) {
        float chance = 0.01F * rpg.getAttribute(RpgAttribute.PRECISION);
        if (rpg.hasSkill(SkillTrees.W_CRIT)) {
            chance += 0.15F;
        }
        return Math.min(0.75F, chance);
    }

    /** Fraction [0..0.85] of incoming damage removed by Resistance + Warrior toughness. */
    public static float damageReduction(PlayerRpg rpg) {
        int resistance = rpg.getAttribute(RpgAttribute.RESISTANCE);
        float reduction = resistance / (resistance + 60.0F);
        if (rpg.hasSkill(SkillTrees.W_TOUGH)) {
            reduction += (1.0F - reduction) * 0.10F;
        }
        return Math.min(0.85F, reduction);
    }

    /** Extra reduction while blocking (Warrior block + parry). */
    public static float blockingReduction(PlayerRpg rpg) {
        float r = 0.0F;
        if (rpg.hasSkill(SkillTrees.W_BLOCK)) {
            r += 0.25F;
        }
        if (rpg.hasSkill(SkillTrees.W_PARRY)) {
            r += 0.25F;
        }
        return r;
    }

    public static float effectResistChance(PlayerRpg rpg) {
        return Math.min(0.6F, 0.005F * rpg.getAttribute(RpgAttribute.RESISTANCE));
    }

    public static float fallDamageMultiplier(PlayerRpg rpg) {
        return Math.max(0.5F, 1.0F - 0.02F * rpg.getAttribute(RpgAttribute.AGILITY));
    }

    // --- Miner ---

    /** Block breaking speed multiplier (Mining attribute + Miner speed). */
    public static float miningSpeedMultiplier(PlayerRpg rpg) {
        float m = 1.0F + 0.03F * rpg.getAttribute(RpgAttribute.MINING)
                + 0.004F * rpg.getAttribute(RpgAttribute.AGILITY);
        if (rpg.hasSkill(SkillTrees.MI_SPEED)) {
            m += 0.30F;
        }
        return m;
    }
}
