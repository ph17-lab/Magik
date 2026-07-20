package com.magik.item;

import com.magik.entity.MagicBoltEntity;
import com.magik.network.MagikNetwork;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Magic staff family: right click fires a {@link MagicBoltEntity} of the
 * staff's element, consuming mana. Each staff defines damage, mana cost,
 * cast speed (built-in cooldown) and durability; Intelligence scales damage
 * and discounts mana, and each element applies its own on-hit effect
 * (fire ignites, ice slows, lightning chains sparks, holy heals the caster...).
 */
public class StaffItem extends Item implements RpgGear {

    private final MagicBoltEntity.Variant variant;
    private final float baseDamage;
    private final float baseManaCost;
    private final int castCooldownTicks;
    private final ItemRequirements requirements;

    public StaffItem(MagicBoltEntity.Variant variant, float baseDamage, float baseManaCost,
                     int castCooldownTicks, ItemRequirements requirements, Properties properties) {
        super(properties);
        this.variant = variant;
        this.baseDamage = baseDamage;
        this.baseManaCost = baseManaCost;
        this.castCooldownTicks = castCooldownTicks;
        this.requirements = requirements;
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return requirements;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }
        if (!RpgGear.checkAndWarn(serverPlayer, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        PlayerRpg rpg = PlayerRpgProvider.get(serverPlayer).orElse(null);
        if (rpg == null) {
            return InteractionResultHolder.pass(stack);
        }
        float cost = baseManaCost * RpgStats.manaCostMultiplier(rpg);
        if (!rpg.consumeMana(cost)) {
            serverPlayer.displayClientMessage(Component.translatable("message.magik.no_mana"), true);
            return InteractionResultHolder.fail(stack);
        }

        float damage = baseDamage * RpgStats.magicDamageMultiplier(rpg);
        MagicBoltEntity bolt = new MagicBoltEntity(level, serverPlayer, damage, variant);
        bolt.shootFromRotation(serverPlayer, serverPlayer.getXRot(), serverPlayer.getYRot(), 0.0F, 2.0F, 0.6F);
        level.addFreshEntity(bolt);

        level.playSound(null, serverPlayer.blockPosition(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.7F, castPitch());
        serverPlayer.getCooldowns().addCooldown(this, castCooldownTicks);
        stack.hurtAndBreak(1, serverPlayer, p -> p.broadcastBreakEvent(hand));
        MagikNetwork.syncVitals(serverPlayer, rpg);
        return InteractionResultHolder.consume(stack);
    }

    private float castPitch() {
        return switch (variant) {
            case FIRE -> 0.9F;
            case FROST -> 1.6F;
            case ELECTRIC -> 1.9F;
            case HOLY -> 1.3F;
            case SUPREME -> 0.7F;
            default -> 1.4F;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId(stack) + ".tooltip"));
        tooltip.add(Component.translatable("tooltip.magik.staff_stats",
                (int) baseDamage, (int) baseManaCost, String.format("%.1f", castCooldownTicks / 20.0F)));
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
