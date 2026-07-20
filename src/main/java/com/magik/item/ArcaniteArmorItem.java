package com.magik.item;

import com.magik.player.RpgStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Arcanite armor: the mod's RPG gear set. Wearing all four pieces grants
 * bonus max mana (see {@link RpgStats#ARCANITE_SET_MANA_BONUS}).
 */
public class ArcaniteArmorItem extends ArmorItem implements RpgGear {

    private static final ItemRequirements REQUIREMENTS =
            ItemRequirements.of(20, com.magik.player.RpgAttribute.INTELLIGENCE, 10);

    public ArcaniteArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return REQUIREMENTS;
    }

    /** True when the player wears the complete Arcanite set. */
    public static boolean hasFullSet(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            if (!(player.getItemBySlot(slot).getItem() instanceof ArcaniteArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.magik.arcanite_armor.tooltip",
                (int) RpgStats.ARCANITE_SET_MANA_BONUS));
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
