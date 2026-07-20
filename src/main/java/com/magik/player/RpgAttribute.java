package com.magik.player;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The seven RPG attributes a player can invest points into.
 * Attribute effects are implemented in {@link RpgStats} (derived values)
 * and {@link com.magik.combat.RpgAttributeApplier} (vanilla attribute modifiers).
 */
public enum RpgAttribute {
    /** +2 max health per point. */
    VITALITY,
    /** +4% melee damage per point, +2% extra for heavy weapons. */
    STRENGTH,
    /** Damage reduction, +armor and a chance to resist negative effects. */
    RESISTANCE,
    /** +move/attack/tool speed, less fall damage, +max stamina. */
    AGILITY,
    /** +ranged damage, +crit chance, +projectile velocity. */
    PRECISION,
    /** +max mana, +magic damage, cheaper and faster spells. */
    INTELLIGENCE,
    /** +block breaking speed per point. */
    MINING;

    private final String key = name().toLowerCase(Locale.ROOT);

    public String getId() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("attribute.magik." + key);
    }

    public Component getDescription() {
        return Component.translatable("attribute.magik." + key + ".desc");
    }

    public static RpgAttribute byId(String id) {
        for (RpgAttribute attribute : values()) {
            if (attribute.key.equals(id)) {
                return attribute;
            }
        }
        return null;
    }
}
