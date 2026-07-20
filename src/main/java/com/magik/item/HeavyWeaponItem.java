package com.magik.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sword-class heavy weapon (warhammers, greatswords): slow, brutal, with an
 * extra damage bonus from Strength and the Armamento Pesado passives.
 */
public class HeavyWeaponItem extends SwordItem implements HeavyWeapon, RpgGear {

    private final ItemRequirements requirements;

    public HeavyWeaponItem(Tier tier, int attackDamage, float attackSpeed,
                           ItemRequirements requirements, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.requirements = requirements;
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return requirements;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.magik.heavy_weapon.tooltip"));
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
