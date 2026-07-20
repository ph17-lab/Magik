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
 * RPG sword with level/attribute requirements and an optional passive crit
 * bonus. Benefits from Agility (attack speed), Strength (damage) and the
 * Espadachim tree passives.
 */
public class RpgSwordItem extends SwordItem implements RpgGear {

    private final ItemRequirements requirements;
    private final float critBonus;

    public RpgSwordItem(Tier tier, int attackDamage, float attackSpeed,
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
        String key = getDescriptionId(stack) + ".tooltip";
        tooltip.add(Component.translatable(key));
        if (critBonus > 0.0F) {
            tooltip.add(Component.translatable("tooltip.magik.crit_bonus", (int) (critBonus * 100)));
        }
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
