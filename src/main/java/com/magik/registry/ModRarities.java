package com.magik.registry;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * Extended item rarities beyond vanilla's four, using Forge's extensible enum.
 * Vanilla ladder: COMMON (white) &lt; UNCOMMON (yellow) &lt; RARE (aqua)
 * &lt; EPIC (light purple) &lt; LEGENDARY (gold) &lt; MYTHIC (red).
 */
public final class ModRarities {

    public static final Rarity LEGENDARY = Rarity.create("magik:legendary", ChatFormatting.GOLD);
    public static final Rarity MYTHIC = Rarity.create("magik:mythic", ChatFormatting.RED);

    private ModRarities() {
    }
}
