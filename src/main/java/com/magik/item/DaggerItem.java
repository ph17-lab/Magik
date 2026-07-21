package com.magik.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fast, light rogue blade. Low base damage and a very quick swing, high crit
 * synergy when a dagger is held in both hands. Powers the Adaga skill tree.
 */
public class DaggerItem extends SwordItem implements Dagger, RpgGear {

    private final ItemRequirements requirements;
    private final float critBonus;

    public DaggerItem(Tier tier, int attackDamage, float attackSpeed,
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
        tooltip.add(Component.translatable(getDescriptionId(stack) + ".tooltip"));
        tooltip.add(Component.translatable("tooltip.magik.dagger_dual").withStyle(ChatFormatting.DARK_PURPLE));
        if (critBonus > 0.0F) {
            tooltip.add(Component.translatable("tooltip.magik.crit_bonus", (int) (critBonus * 100)));
        }
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
