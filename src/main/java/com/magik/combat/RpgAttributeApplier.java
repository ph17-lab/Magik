package com.magik.combat;

import com.magik.player.PlayerRpg;
import com.magik.player.RpgAttribute;
import com.magik.player.RpgStats;
import com.magik.skills.SkillTrees;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Translates RPG attribute investments and passive skills into vanilla
 * attribute modifiers (max health, movement speed, attack speed, knockback
 * resistance). Modifiers are transient (never saved to NBT) and re-applied
 * from the capability whenever it changes, so they can never stack or leak.
 */
public final class RpgAttributeApplier {

    private static final UUID VITALITY_HEALTH_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e01");
    private static final UUID AGILITY_SPEED_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e02");
    private static final UUID AGILITY_ATTACK_SPEED_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e03");
    private static final UUID REINFORCED_LIFE_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e04");
    private static final UUID SWORD_MASTER_SPEED_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e05");
    private static final UUID FORTRESS_KB_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e06");
    private static final UUID DAGGER_MOVE_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e07");
    private static final UUID DAGGER_ATTACK_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e08");
    private static final UUID SHADOW_MASTER_MOVE_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e09");
    private static final UUID SHADOW_MASTER_ATTACK_UUID = UUID.fromString("c8b9f8f0-1a2b-4c3d-9e4f-5a6b7c8d9e0a");

    /** Extra max health granted by the Reinforced Life passive. */
    public static final double REINFORCED_LIFE_HEALTH = 4.0D;
    /** Attack speed bonus granted by the Sword Master passive. */
    public static final double SWORD_MASTER_ATTACK_SPEED = 0.10D;
    /** Knockback resistance granted by Unbreakable Fortress. */
    public static final double FORTRESS_KNOCKBACK_RESIST = 0.4D;

    private RpgAttributeApplier() {
    }

    /** Re-applies every modifier from scratch. Safe to call repeatedly. */
    public static void apply(Player player, PlayerRpg rpg) {
        set(player, Attributes.MAX_HEALTH, VITALITY_HEALTH_UUID, "magik.vitality",
                2.0D * rpg.getAttribute(RpgAttribute.VITALITY), AttributeModifier.Operation.ADDITION);

        set(player, Attributes.MAX_HEALTH, REINFORCED_LIFE_UUID, "magik.reinforced_life",
                rpg.hasSkill(SkillTrees.REINFORCED_LIFE) ? REINFORCED_LIFE_HEALTH : 0.0D,
                AttributeModifier.Operation.ADDITION);

        set(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED_UUID, "magik.agility_speed",
                0.01D * rpg.getAttribute(RpgAttribute.AGILITY), AttributeModifier.Operation.MULTIPLY_TOTAL);

        set(player, Attributes.ATTACK_SPEED, AGILITY_ATTACK_SPEED_UUID, "magik.agility_attack",
                0.015D * rpg.getAttribute(RpgAttribute.AGILITY), AttributeModifier.Operation.MULTIPLY_TOTAL);

        set(player, Attributes.ATTACK_SPEED, SWORD_MASTER_SPEED_UUID, "magik.sword_master",
                rpg.hasSkill(SkillTrees.SWORD_MASTER) ? SWORD_MASTER_ATTACK_SPEED : 0.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);

        set(player, Attributes.KNOCKBACK_RESISTANCE, FORTRESS_KB_UUID, "magik.fortress",
                rpg.hasSkill(SkillTrees.UNBREAKABLE_FORTRESS) ? FORTRESS_KNOCKBACK_RESIST : 0.0D,
                AttributeModifier.Operation.ADDITION);

        // --- Adaga (dagger) tree buffs ---
        long gameTime = player.level().getGameTime();
        // Ghost Steps (+30%) or Shadow Veil (+60%) movement surge (take the higher).
        double daggerMove = 0.0D;
        if (gameTime < rpg.getShadowVeilUntil()) {
            daggerMove = 0.60D;
        } else if (gameTime < rpg.getGhostStepsUntil()) {
            daggerMove = 0.30D;
        }
        set(player, Attributes.MOVEMENT_SPEED, DAGGER_MOVE_UUID, "magik.dagger_move",
                daggerMove, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // Dagger Dance attack-speed surge.
        set(player, Attributes.ATTACK_SPEED, DAGGER_ATTACK_UUID, "magik.dagger_dance",
                gameTime < rpg.getDaggerDanceUntil() ? 0.50D : 0.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        // Shadow Master permanent passive (attack speed + movement).
        boolean master = rpg.hasSkill(SkillTrees.SHADOW_MASTER);
        set(player, Attributes.MOVEMENT_SPEED, SHADOW_MASTER_MOVE_UUID, "magik.shadow_master_move",
                master ? 0.10D : 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        set(player, Attributes.ATTACK_SPEED, SHADOW_MASTER_ATTACK_UUID, "magik.shadow_master_attack",
                master ? 0.15D : 0.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // Never leave the player above their (possibly reduced) max health.
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        RpgStats.clampResources(rpg, player);
    }

    private static void set(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                            UUID uuid, String name, double amount, AttributeModifier.Operation operation) {
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
