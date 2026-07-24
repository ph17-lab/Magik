package com.magik.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side (per world, synced to clients) balancing configuration.
 * All progression tuning knobs live here so server owners can rebalance
 * the mod without touching code.
 */
public final class MagikServerConfig {

    public static final ForgeConfigSpec SPEC;

    // --- Progression curve ---
    public static final ForgeConfigSpec.IntValue MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue XP_CURVE_BASE;
    public static final ForgeConfigSpec.DoubleValue XP_CURVE_EXPONENT;
    public static final ForgeConfigSpec.IntValue ATTRIBUTE_POINTS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue SKILL_POINTS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue MAX_ATTRIBUTE;

    // --- XP sources ---
    public static final ForgeConfigSpec.DoubleValue KILL_XP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue CRAFT_XP;
    public static final ForgeConfigSpec.IntValue SMELT_XP;
    public static final ForgeConfigSpec.IntValue ORE_XP;
    public static final ForgeConfigSpec.IntValue RARE_ORE_XP;
    public static final ForgeConfigSpec.IntValue ADVANCEMENT_XP;
    public static final ForgeConfigSpec.IntValue SKILL_USE_XP;

    // --- Resource pools ---
    public static final ForgeConfigSpec.DoubleValue BASE_MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue MANA_PER_INTELLIGENCE;
    public static final ForgeConfigSpec.DoubleValue BASE_MAX_STAMINA;
    public static final ForgeConfigSpec.DoubleValue STAMINA_PER_AGILITY;
    public static final ForgeConfigSpec.DoubleValue DASH_STAMINA_COST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("progression");
        MAX_LEVEL = b.comment("Maximum level a player can reach.")
                .defineInRange("maxLevel", 50, 1, 10000);
        XP_CURVE_BASE = b.comment("XP required to go from level L to L+1 is: base * L ^ exponent.",
                        "Low levels stay fast while high levels become much more demanding.")
                .defineInRange("xpCurveBase", 80.0D, 1.0D, 100000.0D);
        XP_CURVE_EXPONENT = b.defineInRange("xpCurveExponent", 1.55D, 1.0D, 4.0D);
        ATTRIBUTE_POINTS_PER_LEVEL = b.comment("Attribute points granted on each level up.")
                .defineInRange("attributePointsPerLevel", 3, 0, 100);
        MAX_ATTRIBUTE = b.comment("Maximum value a single attribute can reach.")
                .defineInRange("maxAttribute", 100, 1, 10000);
        SKILL_POINTS_PER_LEVEL = b.comment("Skill points granted on each level up.")
                .defineInRange("skillPointsPerLevel", 5, 0, 100);
        b.pop();

        b.push("xpSources");
        KILL_XP_MULTIPLIER = b.comment("Kill XP = (3 + maxHealth * 0.6) * multiplier.")
                .defineInRange("killXpMultiplier", 1.0D, 0.0D, 100.0D);
        CRAFT_XP = b.defineInRange("craftXp", 2, 0, 1000);
        SMELT_XP = b.defineInRange("smeltXp", 3, 0, 1000);
        ORE_XP = b.defineInRange("oreXp", 4, 0, 1000);
        RARE_ORE_XP = b.comment("XP for diamond, emerald and ancient debris.")
                .defineInRange("rareOreXp", 12, 0, 1000);
        ADVANCEMENT_XP = b.defineInRange("advancementXp", 25, 0, 10000);
        SKILL_USE_XP = b.defineInRange("skillUseXp", 3, 0, 1000);
        b.pop();

        b.push("resources");
        BASE_MAX_MANA = b.defineInRange("baseMaxMana", 100.0D, 1.0D, 100000.0D);
        MANA_PER_INTELLIGENCE = b.defineInRange("manaPerIntelligence", 5.0D, 0.0D, 1000.0D);
        BASE_MAX_STAMINA = b.defineInRange("baseMaxStamina", 100.0D, 1.0D, 100000.0D);
        STAMINA_PER_AGILITY = b.defineInRange("staminaPerAgility", 2.0D, 0.0D, 1000.0D);
        DASH_STAMINA_COST = b.defineInRange("dashStaminaCost", 25.0D, 0.0D, 100000.0D);
        b.pop();

        SPEC = b.build();
    }

    private MagikServerConfig() {
    }
}
