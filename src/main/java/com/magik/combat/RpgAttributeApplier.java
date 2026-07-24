package com.magik.combat;

import com.magik.compat.IronsCompat;
import com.magik.player.PlayerRpg;
import com.magik.player.RpgAttribute;
import com.magik.player.RpgStats;
import com.magik.skills.SkillTrees;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * Translates attributes and progression skills into vanilla attribute
 * modifiers (health, movement/attack speed, block reach). Modifiers are
 * transient and re-applied from scratch on every change, so they never stack
 * or leak into NBT.
 */
public final class RpgAttributeApplier {

    private static final UUID VITALITY_HEALTH = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e01");
    private static final UUID AGILITY_SPEED = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e02");
    private static final UUID AGILITY_ATTACK = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e03");
    private static final UUID W_TOUGH_HEALTH = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e04");
    private static final UUID W_SPEED_ATTACK = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e05");
    private static final UUID W_POWER_DAMAGE = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e06");
    private static final UUID REACH_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e07");

    private RpgAttributeApplier() {
    }

    public static void apply(Player player, PlayerRpg rpg) {
        set(player, Attributes.MAX_HEALTH, VITALITY_HEALTH, "magik.vitality",
                2.0D * rpg.getAttribute(RpgAttribute.VITALITY), AttributeModifier.Operation.ADDITION);
        set(player, Attributes.MAX_HEALTH, W_TOUGH_HEALTH, "magik.tough",
                rpg.hasSkill(SkillTrees.W_TOUGH) ? 6.0D : 0.0D, AttributeModifier.Operation.ADDITION);

        set(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED, "magik.agility_speed",
                0.01D * rpg.getAttribute(RpgAttribute.AGILITY), AttributeModifier.Operation.MULTIPLY_TOTAL);

        set(player, Attributes.ATTACK_SPEED, AGILITY_ATTACK, "magik.agility_attack",
                0.01D * rpg.getAttribute(RpgAttribute.AGILITY), AttributeModifier.Operation.MULTIPLY_TOTAL);
        set(player, Attributes.ATTACK_SPEED, W_SPEED_ATTACK, "magik.w_speed",
                rpg.hasSkill(SkillTrees.W_SPEED) ? 0.15D : 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);

        set(player, Attributes.ATTACK_DAMAGE, W_POWER_DAMAGE, "magik.w_power",
                rpg.hasSkill(SkillTrees.W_POWER) ? 2.0D : 0.0D, AttributeModifier.Operation.ADDITION);

        // Builder + Miner reach (Forge block-interaction range).
        double reach = 0.0D;
        if (rpg.hasSkill(SkillTrees.BU_REACH)) {
            reach += 2.0D;
        }
        if (rpg.hasSkill(SkillTrees.MI_REACH)) {
            reach += 1.0D;
        }
        Attribute reachAttr = ForgeMod.BLOCK_REACH.get();
        set(player, reachAttr, REACH_UUID, "magik.reach", reach, AttributeModifier.Operation.ADDITION);

        // Optional Iron's Spells arcane bonuses.
        IronsCompat.applyArcane(player, rpg);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        RpgStats.clampResources(rpg, player);
    }

    private static void set(Player player, Attribute attribute, UUID uuid, String name,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
        }
    }
}
