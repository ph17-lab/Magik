package com.magik.item;

import com.magik.player.PlayerRpg;
import com.magik.player.RpgAttribute;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Level and attribute requirements of a piece of RPG gear.
 * Enforced server-side (attacks, item use, worn armor) and shown in tooltips.
 */
public record ItemRequirements(int level, Map<RpgAttribute, Integer> attributes) {

    public static final ItemRequirements NONE = new ItemRequirements(0, Collections.emptyMap());

    public static ItemRequirements level(int level) {
        return new ItemRequirements(level, Collections.emptyMap());
    }

    public static ItemRequirements of(int level, Object... attributePairs) {
        EnumMap<RpgAttribute, Integer> map = new EnumMap<>(RpgAttribute.class);
        for (int i = 0; i < attributePairs.length; i += 2) {
            map.put((RpgAttribute) attributePairs[i], (Integer) attributePairs[i + 1]);
        }
        return new ItemRequirements(level, Collections.unmodifiableMap(map));
    }

    public boolean isMetBy(PlayerRpg rpg) {
        if (rpg.getLevel() < level) {
            return false;
        }
        for (Map.Entry<RpgAttribute, Integer> entry : attributes.entrySet()) {
            if (rpg.getAttribute(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return level <= 0 && attributes.isEmpty();
    }
}
