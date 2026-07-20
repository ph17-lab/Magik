package com.magik.item;

import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgStats;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * RPG bow family: each bow defines its own draw time, projectile velocity,
 * damage bonus, durability and requirements. The Rapid Fire passive further
 * accelerates drawing, and Precision scales damage/crit globally.
 *
 * <p>Draw time is implemented by rescaling the elapsed use ticks before
 * delegating to the vanilla release logic, so all enchantments (Power,
 * Infinity, Flame...) keep working untouched.</p>
 */
public class RpgBowItem extends BowItem implements RpgGear {

    /** Vanilla's full-charge time in ticks. */
    private static final float VANILLA_DRAW_TICKS = 20.0F;

    private final int drawTicks;
    private final float velocityMultiplier;
    private final float damageBonus;
    private final ItemRequirements requirements;

    public RpgBowItem(int drawTicks, float velocityMultiplier, float damageBonus,
                      ItemRequirements requirements, Properties properties) {
        super(properties);
        this.drawTicks = drawTicks;
        this.velocityMultiplier = velocityMultiplier;
        this.damageBonus = damageBonus;
        this.requirements = requirements;
    }

    @Override
    public ItemRequirements getRpgRequirements() {
        return requirements;
    }

    /** Extra velocity applied by ArrowEvents when this bow fires. */
    public float getVelocityMultiplier() {
        return velocityMultiplier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer
                && !RpgGear.checkAndWarn(serverPlayer, player.getItemInHand(hand))) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        float speed = VANILLA_DRAW_TICKS / drawTicks;
        if (entity instanceof Player player) {
            speed *= PlayerRpgProvider.get(player).map(RpgStats::drawSpeedMultiplier).orElse(1.0F);
        }
        int elapsed = getUseDuration(stack) - timeLeft;
        int scaled = Math.round(elapsed * speed);
        super.releaseUsing(stack, level, entity, getUseDuration(stack) - scaled);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        if (damageBonus > 0.0F) {
            arrow.setBaseDamage(arrow.getBaseDamage() * (1.0F + damageBonus));
        }
        return arrow;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId(stack) + ".tooltip"));
        tooltip.add(Component.translatable("tooltip.magik.bow_stats",
                String.format("%.1f", drawTicks / 20.0F),
                String.format("%+d", (int) ((velocityMultiplier - 1.0F) * 100)),
                (int) (damageBonus * 100)));
        RpgGear.appendRequirementTooltip(stack, tooltip);
    }
}
