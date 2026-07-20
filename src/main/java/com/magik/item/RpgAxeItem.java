package com.magik.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Combat axe: very high damage, slow swing, elevated crit chance.
 * Counts as a heavy weapon for the Armamento Pesado tree.
 */
public class RpgAxeItem extends AxeItem implements HeavyWeapon, RpgGear {

    private final ItemRequirements requirements;
    private final float critBonus;

    public RpgAxeItem(Tier tier, float attackDamage, float attackSpeed,
                      ItemRequirements requirements, float critBonus, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.requirements = requirements;
        this.critBonus = critBonus;
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return requirements;
    }

    @Override
    public float getCritBonus() {
        return critBonus;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.magik.heavy_weapon.tooltip"));
        if (critBonus > 0.0F) {
            tooltip.add(Component.translatable("tooltip.magik.crit_bonus", (int) (critBonus * 100)));
        }
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
