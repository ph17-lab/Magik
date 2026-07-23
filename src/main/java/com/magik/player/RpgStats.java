package com.magik.player;

import com.magik.config.MagikServerConfig;
import com.magik.config.SkillBalance;
import com.magik.skills.SkillTrees;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Every derived-stat formula of the mod in a single, documented place.
 * Both sides (server logic and client HUD/tooltips) read from here, so the
 * displayed numbers always match the actual gameplay effects.
 */
public final class RpgStats {

    private RpgStats() {
    }

    // ------------------------------------------------------------------
    // Progression
    // ------------------------------------------------------------------

    /** XP required to advance from {@code level} to {@code level + 1}. */
    public static int xpForNextLevel(int level) {
        double base = MagikServerConfig.XP_CURVE_BASE.get();
        double exponent = MagikServerConfig.XP_CURVE_EXPONENT.get();
        return (int) Math.round(base * Math.pow(level, exponent));
    }

    // ------------------------------------------------------------------
    // Mana
    // ------------------------------------------------------------------

    /** Bonus max mana granted by wearing the full Arcanite set. */
    public static final float ARCANITE_SET_MANA_BONUS = 25.0F;

    public static float maxMana(PlayerRpg rpg, Player player) {
        float max = (float) (MagikServerConfig.BASE_MAX_MANA.get()
                + MagikServerConfig.MANA_PER_INTELLIGENCE.get() * rpg.getAttribute(RpgAttribute.INTELLIGENCE));
        return max;
    }

    /** Mana restored per tick. Intelligence speeds it up. */
    public static float manaRegenPerTick(PlayerRpg rpg) {
        float regen = 0.05F + 0.004F * rpg.getAttribute(RpgAttribute.INTELLIGENCE);
        return regen * SkillBalance.manaRegenMultiplier();
    }

    /** Multiplier applied to every mana cost (intelligence discount, capped at -40%). */
    public static float manaCostMultiplier(PlayerRpg rpg) {
        return Math.max(0.6F, 1.0F - 0.005F * rpg.getAttribute(RpgAttribute.INTELLIGENCE));
    }

    /** Multiplier applied to spell cooldowns (intelligence discount, capped at -40%). */
    public static float cooldownMultiplier(PlayerRpg rpg) {
        return Math.max(0.6F, 1.0F - 0.005F * rpg.getAttribute(RpgAttribute.INTELLIGENCE));
    }

    /** Multiplier applied to all magic damage dealt by the player. */
    public static float magicDamageMultiplier(PlayerRpg rpg) {
        return 1.0F + 0.04F * rpg.getAttribute(RpgAttribute.INTELLIGENCE);
    }

    // ------------------------------------------------------------------
    // Stamina
    // ------------------------------------------------------------------

    /** Ticks after spending stamina before regeneration resumes. */
    public static final int STAMINA_REGEN_DELAY_TICKS = 30;

    public static float maxStamina(PlayerRpg rpg) {
        return (float) (MagikServerConfig.BASE_MAX_STAMINA.get()
                + MagikServerConfig.STAMINA_PER_AGILITY.get() * rpg.getAttribute(RpgAttribute.AGILITY));
    }

    public static float staminaRegenPerTick(PlayerRpg rpg) {
        return 0.8F * (1.0F + 0.01F * rpg.getAttribute(RpgAttribute.AGILITY))
                * SkillBalance.staminaRegenMultiplier();
    }

    // ------------------------------------------------------------------
    // Melee combat
    // ------------------------------------------------------------------

    /** Multiplier for direct melee damage (Strength). */
    public static float meleeDamageMultiplier(PlayerRpg rpg) {
        return 1.0F + 0.04F * rpg.getAttribute(RpgAttribute.STRENGTH);
    }

    /** Extra multiplier when attacking with a heavy weapon (Strength + passives). */
    public static float heavyWeaponBonus(PlayerRpg rpg) {
        float bonus = 0.02F * rpg.getAttribute(RpgAttribute.STRENGTH);
        if (rpg.hasSkill(SkillTrees.HEAVY_BLADES)) {
            bonus += 0.15F;
        }
        if (rpg.hasSkill(SkillTrees.BRUTAL_FORCE)) {
            bonus += 0.15F;
        }
        return bonus;
    }

    /** Extra multiplier when attacking with a sword (Sword Master passive). */
    public static float swordDamageBonus(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.SWORD_MASTER) ? 0.10F : 0.0F;
    }

    /** Fraction of the target's armor ignored by heavy hits (Shield Break). */
    public static float armorPierce(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.SHIELD_BREAK) ? 0.30F : 0.0F;
    }

    /** Chance for heavy weapon hits to stun (Stunning Blows). */
    public static float stunChance(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.STUNNING_BLOWS) ? 0.20F : 0.0F;
    }

    /** Damage bonus per active combo stack (Sword Combo). */
    public static final float COMBO_DAMAGE_PER_STACK = 0.15F;
    public static final int COMBO_MAX_STACKS = 5;

    // ------------------------------------------------------------------
    // Ranged combat
    // ------------------------------------------------------------------

    /** Multiplier for projectile damage (Precision + archer passives). */
    public static float rangedDamageMultiplier(PlayerRpg rpg) {
        float multiplier = 1.0F + 0.03F * rpg.getAttribute(RpgAttribute.PRECISION);
        if (rpg.hasSkill(SkillTrees.ZOOM)) {
            multiplier += 0.10F;
        }
        return multiplier;
    }

    /** Arrow velocity multiplier (Precision + Long Range passive). */
    public static float arrowVelocityMultiplier(PlayerRpg rpg) {
        float multiplier = 1.0F + 0.01F * rpg.getAttribute(RpgAttribute.PRECISION);
        if (rpg.hasSkill(SkillTrees.LONG_RANGE)) {
            multiplier += 0.25F;
        }
        return multiplier;
    }

    /** Bow draw speed multiplier (Rapid Fire passive; used by Magik bows). */
    public static float drawSpeedMultiplier(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.RAPID_FIRE) ? 1.35F : 1.0F;
    }

    /**
     * Chance [0..1] for a critical strike, capped at 75%.
     *
     * @param sword  attacking with a sword (Sword Crit passive)
     * @param ranged attacking with a projectile (Bow Crit passive)
     */
    public static float critChance(PlayerRpg rpg, boolean sword, boolean ranged) {
        float chance = 0.01F * rpg.getAttribute(RpgAttribute.PRECISION);
        if (sword && rpg.hasSkill(SkillTrees.SWORD_CRIT)) {
            chance += 0.10F;
        }
        if (ranged && rpg.hasSkill(SkillTrees.BOW_CRIT)) {
            chance += 0.10F;
        }
        return Math.min(0.75F, chance);
    }

    /** Damage multiplier applied on a critical strike. */
    public static final float CRIT_MULTIPLIER = 1.5F;

    // ------------------------------------------------------------------
    // Defense
    // ------------------------------------------------------------------

    /** Fraction [0..1] of incoming damage removed by Resistance (soft-capped curve). */
    public static float damageReduction(PlayerRpg rpg) {
        int resistance = rpg.getAttribute(RpgAttribute.RESISTANCE);
        float reduction = resistance / (resistance + 60.0F);
        if (rpg.hasSkill(SkillTrees.DAMAGE_REDUCTION)) {
            reduction += (1.0F - reduction) * 0.10F; // -10% of what's left.
        }
        return Math.min(0.80F, reduction);
    }

    /** Extra reduction while actively blocking with a shield (Improved Block). */
    public static float blockingReduction(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.IMPROVED_BLOCK) ? 0.20F : 0.0F;
    }

    /** Multiplier for fire/lava damage (Fire Resistance passive). */
    public static float fireDamageMultiplier(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.FIRE_RESISTANCE) ? 0.5F : 1.0F;
    }

    /** Multiplier for explosion damage (Blast Resistance passive). */
    public static float blastDamageMultiplier(PlayerRpg rpg) {
        return rpg.hasSkill(SkillTrees.BLAST_RESISTANCE) ? 0.6F : 1.0F;
    }

    /** Chance [0..1] to completely resist a harmful potion effect. */
    public static float effectResistChance(PlayerRpg rpg) {
        float chance = 0.005F * rpg.getAttribute(RpgAttribute.RESISTANCE);
        if (rpg.hasSkill(SkillTrees.UNBREAKABLE_FORTRESS)) {
            chance += 0.30F;
        }
        return Math.min(0.75F, chance);
    }

    /** Multiplier for fall damage (Agility, never below 50%). */
    public static float fallDamageMultiplier(PlayerRpg rpg) {
        return Math.max(0.5F, 1.0F - 0.02F * rpg.getAttribute(RpgAttribute.AGILITY));
    }

    /** Ticks without taking/dealing damage before Regeneration starts healing. */
    public static final int OUT_OF_COMBAT_TICKS = 100;

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    /** Multiplier for block breaking speed (Mining Speed + a touch of Agility). */
    public static float miningSpeedMultiplier(PlayerRpg rpg) {
        return 1.0F + 0.04F * rpg.getAttribute(RpgAttribute.MINING)
                + 0.005F * rpg.getAttribute(RpgAttribute.AGILITY);
    }

    /** Clamps mana/stamina against their current maximums (call after gear changes). */
    public static void clampResources(PlayerRpg rpg, Player player) {
        rpg.setMana(Mth.clamp(rpg.getMana(), 0.0F, maxMana(rpg, player)));
        rpg.setStamina(Mth.clamp(rpg.getStamina(), 0.0F, maxStamina(rpg)));
    }
}
