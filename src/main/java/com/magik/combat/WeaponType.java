package com.magik.combat;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Classifies any held weapon so the skill trees empower vanilla weapons and
 * modded ones alike - in particular <b>Spartan Weaponry</b> (daggers,
 * greatswords, battle hammers, halberds, ...), detected by registry id since
 * the mod is an optional dependency we never reference directly.
 */
public enum WeaponType {
    SWORD, DAGGER, HEAVY, BOW, NONE;

    public static WeaponType of(ItemStack stack) {
        if (stack.isEmpty()) {
            return NONE;
        }
        Item item = stack.getItem();
        var key = ForgeRegistries.ITEMS.getKey(item);
        String path = key != null ? key.getPath() : "";

        if (item instanceof BowItem || item instanceof CrossbowItem
                || path.contains("bow") || path.contains("crossbow") || path.contains("sling")) {
            return BOW;
        }
        // Daggers first: they are technically sword-class but need their own tree.
        if (path.contains("dagger") || path.contains("knife") || path.contains("dirk")) {
            return DAGGER;
        }
        // Heavy: big two-handers and blunt weapons (vanilla axes + Spartan Weaponry).
        if (item instanceof AxeItem
                || path.contains("hammer") || path.contains("greatsword") || path.contains("greataxe")
                || path.contains("halberd") || path.contains("battleaxe") || path.contains("battle_axe")
                || path.contains("warhammer") || path.contains("mace") || path.contains("glaive")
                || path.contains("longaxe") || path.contains("club") || path.contains("maul")) {
            return HEAVY;
        }
        if (item instanceof SwordItem
                || path.contains("sword") || path.contains("rapier") || path.contains("katana")
                || path.contains("saber") || path.contains("sabre") || path.contains("machete")
                || path.contains("scimitar") || path.contains("longsword") || path.contains("cutlass")) {
            return SWORD;
        }
        return NONE;
    }

    public static WeaponType mainHand(net.minecraft.world.entity.LivingEntity entity) {
        return of(entity.getMainHandItem());
    }
}
