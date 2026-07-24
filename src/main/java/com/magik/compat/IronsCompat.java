package com.magik.compat;

import com.magik.player.PlayerRpg;
import com.magik.skills.SkillTrees;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Optional integration with Iron's Spells 'n Spellbooks. Everything is looked
 * up by registry id and guarded by {@link #LOADED}, so the mod never references
 * Iron's classes directly and causes no errors when it is absent.
 *
 * <p>The Arcane tree boosts the player's Iron's attributes: max mana, mana
 * regen, spell power, cooldown/cast-time reduction and spell resistance.</p>
 */
public final class IronsCompat {

    public static final boolean LOADED = ModList.get().isLoaded("irons_spellbooks");

    private static final UUID MANA_UUID = UUID.fromString("a1b2c3d4-0001-4000-8000-000000000001");
    private static final UUID MANA_REGEN_UUID = UUID.fromString("a1b2c3d4-0002-4000-8000-000000000002");
    private static final UUID POWER_UUID = UUID.fromString("a1b2c3d4-0003-4000-8000-000000000003");
    private static final UUID COOLDOWN_UUID = UUID.fromString("a1b2c3d4-0004-4000-8000-000000000004");
    private static final UUID CAST_UUID = UUID.fromString("a1b2c3d4-0005-4000-8000-000000000005");
    private static final UUID RESIST_UUID = UUID.fromString("a1b2c3d4-0006-4000-8000-000000000006");

    private IronsCompat() {
    }

    /** Re-applies the Arcane tree's Iron's attribute bonuses from scratch. */
    public static void applyArcane(Player player, PlayerRpg rpg) {
        if (!LOADED) {
            return;
        }
        set(player, "max_mana", MANA_UUID, rpg.hasSkill(SkillTrees.AR_MANA) ? 50.0D : 0.0D,
                AttributeModifier.Operation.ADDITION);
        set(player, "mana_regen", MANA_REGEN_UUID, rpg.hasSkill(SkillTrees.AR_MANA_REGEN) ? 0.25D : 0.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        set(player, "spell_power", POWER_UUID, rpg.hasSkill(SkillTrees.AR_POWER) ? 0.20D : 0.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        set(player, "cooldown_reduction", COOLDOWN_UUID, rpg.hasSkill(SkillTrees.AR_COOLDOWN) ? 0.15D : 0.0D,
                AttributeModifier.Operation.ADDITION);
        set(player, "cast_time_reduction", CAST_UUID, rpg.hasSkill(SkillTrees.AR_CAST_SPEED) ? 0.15D : 0.0D,
                AttributeModifier.Operation.ADDITION);
        set(player, "spell_resist", RESIST_UUID, rpg.hasSkill(SkillTrees.AR_RESIST) ? 0.15D : 0.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void set(Player player, String attrPath, UUID uuid, double amount,
                            AttributeModifier.Operation op) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(
                new ResourceLocation("irons_spellbooks", attrPath));
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(uuid, "magik.arcane." + attrPath, amount, op));
        }
    }
}
