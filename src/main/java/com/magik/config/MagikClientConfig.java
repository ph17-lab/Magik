package com.magik.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only configuration: HUD element positions and visibility.
 * Every HUD element can be repositioned by the player through these values
 * (Mods menu -> Magik RPG -> Config, or config/magik-client.toml).
 */
public final class MagikClientConfig {

    public static final ForgeConfigSpec SPEC;

    /** Position of the vitals panel (health / mana / stamina bars). */
    public static final ForgeConfigSpec.IntValue VITALS_X;
    public static final ForgeConfigSpec.IntValue VITALS_Y;
    /** Position of the RPG level + XP bar, relative to the vitals panel. */
    public static final ForgeConfigSpec.IntValue XP_BAR_X;
    public static final ForgeConfigSpec.IntValue XP_BAR_Y;

    public static final ForgeConfigSpec.BooleanValue SHOW_VITALS;
    public static final ForgeConfigSpec.BooleanValue SHOW_XP_BAR;
    public static final ForgeConfigSpec.BooleanValue SHOW_SKILL_SLOTS;
    public static final ForgeConfigSpec.BooleanValue HIDE_FULL_BARS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("hud");
        VITALS_X = b.comment("Screen offset (pixels from top-left) of the vitals panel.")
                .defineInRange("vitalsX", 4, 0, 10000);
        VITALS_Y = b.defineInRange("vitalsY", 4, 0, 10000);
        XP_BAR_X = b.comment("Screen offset of the RPG level badge and XP bar.")
                .defineInRange("xpBarX", 4, 0, 10000);
        XP_BAR_Y = b.defineInRange("xpBarY", 46, 0, 10000);
        SHOW_VITALS = b.define("showVitals", true);
        SHOW_XP_BAR = b.define("showXpBar", true);
        SHOW_SKILL_SLOTS = b.comment("Show the 4 skill slots above the hotbar.")
                .define("showSkillSlots", true);
        HIDE_FULL_BARS = b.comment("Hide mana/stamina bars while they are full and no skill is on cooldown.")
                .define("hideFullBars", false);
        b.pop();

        SPEC = b.build();
    }

    private MagikClientConfig() {
    }
}
