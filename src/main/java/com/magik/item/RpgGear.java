package com.magik.item;

import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Map;

/**
 * Implemented by every piece of RPG gear that has level/attribute
 * requirements or passive combat bonuses. Static helpers centralize
 * server-side enforcement and tooltip rendering.
 */
public interface RpgGear {

    ItemRequirements getRpgRequirements();

    /** Extra critical strike chance while attacking with this item held. */
    default float getCritBonus() {
        return 0.0F;
    }

    // ------------------------------------------------------------------
    // Enforcement helpers
    // ------------------------------------------------------------------

    static boolean meets(PlayerRpg rpg, ItemStack stack) {
        return !(stack.getItem() instanceof RpgGear gear) || gear.getRpgRequirements().isMetBy(rpg);
    }

    /**
     * Server-side gate for attacking/using RPG gear.
     *
     * @return true when the item may be used.
     */
    static boolean checkAndWarn(ServerPlayer player, ItemStack stack) {
        PlayerRpg rpg = PlayerRpgProvider.get(player).orElse(null);
        if (rpg == null || meets(rpg, stack)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.magik.requirements_not_met")
                .withStyle(ChatFormatting.RED), true);
        return false;
    }

    /** Moves worn armor back to the inventory when its requirements are unmet. */
    static void enforceArmorRequirements(ServerPlayer player, PlayerRpg rpg) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && !meets(rpg, stack)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                player.displayClientMessage(Component.translatable("message.magik.requirements_not_met")
                        .withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /** Crit chance bonus of the currently held weapon. */
    static float heldCritBonus(Player player) {
        return player.getMainHandItem().getItem() instanceof RpgGear gear ? gear.getCritBonus() : 0.0F;
    }

    /** Flat damage reduction of the shield currently raised, if any. */
    static float heldShieldFlatReduction(Player player) {
        return player.getUseItem().getItem() instanceof RpgShieldItem shield
                ? shield.getFlatBlockReduction() : 0.0F;
    }

    // ------------------------------------------------------------------
    // Tooltips
    // ------------------------------------------------------------------

    /** Appends colored requirement lines (green = met, red = unmet on the client). */
    static void appendRequirementTooltip(ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof RpgGear gear) || gear.getRpgRequirements().isEmpty()) {
            return;
        }
        ItemRequirements requirements = gear.getRpgRequirements();
        PlayerRpg rpg = clientRpg();
        if (requirements.level() > 0) {
            boolean met = rpg == null || rpg.getLevel() >= requirements.level();
            tooltip.add(Component.translatable("tooltip.magik.requires_level", requirements.level())
                    .withStyle(met ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        }
        for (Map.Entry<RpgAttribute, Integer> entry : requirements.attributes().entrySet()) {
            boolean met = rpg == null || rpg.getAttribute(entry.getKey()) >= entry.getValue();
            tooltip.add(Component.translatable("tooltip.magik.requires_attribute",
                            entry.getValue(), entry.getKey().getDisplayName())
                    .withStyle(met ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        }
    }

    /** The local player's RPG data; null on a dedicated server. */
    private static PlayerRpg clientRpg() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return null;
        }
        return com.magik.client.ClientRpgData.get();
    }
}
