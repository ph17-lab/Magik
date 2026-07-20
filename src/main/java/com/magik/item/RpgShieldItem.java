package com.magik.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * RPG shield family: each shield defines durability, a passive armor bonus
 * while held, a flat reduction applied to damage that leaks through a block,
 * knockback resistance and an optional effect while actively blocking
 * (e.g. the Obsidian Shield grants fire resistance).
 */
public class RpgShieldItem extends ShieldItem implements RpgGear {

    private static final UUID ARMOR_UUID = UUID.fromString("7f10c1e2-4a5b-4c6d-8e9f-0a1b2c3d4e5f");
    private static final UUID KB_UUID = UUID.fromString("7f10c1e2-4a5b-4c6d-8e9f-0a1b2c3d4e60");

    private final double armorBonus;
    private final float flatBlockReduction;
    private final double knockbackResistance;
    @Nullable
    private final Supplier<MobEffect> blockingEffect;
    private final Supplier<Ingredient> repairIngredient;
    private final ItemRequirements requirements;

    public RpgShieldItem(double armorBonus, float flatBlockReduction, double knockbackResistance,
                         @Nullable Supplier<MobEffect> blockingEffect, Supplier<Ingredient> repairIngredient,
                         ItemRequirements requirements, Properties properties) {
        super(properties);
        this.armorBonus = armorBonus;
        this.flatBlockReduction = flatBlockReduction;
        this.knockbackResistance = knockbackResistance;
        this.blockingEffect = blockingEffect;
        this.repairIngredient = repairIngredient;
        this.requirements = requirements;
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return requirements;
    }

    public float getFlatBlockReduction() {
        return flatBlockReduction;
    }

    /** Passive bonuses while the shield is held in either hand. */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        if (armorBonus > 0.0D) {
            builder.put(Attributes.ARMOR, new AttributeModifier(ARMOR_UUID,
                    "magik.shield_armor", armorBonus, AttributeModifier.Operation.ADDITION));
        }
        if (knockbackResistance > 0.0D) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KB_UUID,
                    "magik.shield_kb", knockbackResistance, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    /** Optional buff while actively blocking (refreshed every second). */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide && blockingEffect != null && remainingTicks % 20 == 0) {
            entity.addEffect(new MobEffectInstance(blockingEffect.get(), 40, 0, true, false));
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairIngredient.get().test(repairCandidate);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId(stack) + ".tooltip"));
        if (flatBlockReduction > 0.0F) {
            tooltip.add(Component.translatable("tooltip.magik.block_power", (int) flatBlockReduction));
        }
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
