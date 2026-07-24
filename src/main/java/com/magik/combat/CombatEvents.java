package com.magik.combat;

import com.magik.MagikMod;
import com.magik.item.RpgGear;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgStats;
import com.magik.skills.SkillTrees;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Warrior tree + attribute combat hooks (vanilla-only combat, no custom
 * weapons): scaled melee damage, critical strikes, execution and berserk
 * bonuses, block/parry, lifesteal on kill, damage mitigation and mining speed.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class CombatEvents {

    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !RpgGear.checkAndWarn(player, player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker) {
            handleOutgoing(event, source, attacker);
        }
        if (event.getEntity() instanceof ServerPlayer victim && !event.isCanceled()) {
            handleIncoming(event, source, victim);
        }
    }

    private static void handleOutgoing(LivingHurtEvent event, DamageSource source, ServerPlayer attacker) {
        PlayerRpg rpg = PlayerRpgProvider.get(attacker).orElse(null);
        if (rpg == null) {
            return;
        }
        rpg.setLastCombatTime(attacker.level().getGameTime());
        boolean melee = source.getDirectEntity() == attacker;
        if (!melee) {
            return;
        }
        LivingEntity target = event.getEntity();
        float amount = event.getAmount() * RpgStats.warriorDamageMultiplier(rpg);

        // Execution: heavy bonus against low-health enemies.
        if (rpg.hasSkill(SkillTrees.W_EXECUTE)
                && target.getHealth() < target.getMaxHealth() * 0.30F) {
            amount *= 1.6F;
            spawn(attacker, target, ParticleTypes.DAMAGE_INDICATOR, 10);
        }
        // Berserk: bonus damage while the attacker is at low health.
        if (rpg.hasSkill(SkillTrees.W_BERSERK)
                && attacker.getHealth() < attacker.getMaxHealth() * 0.40F) {
            amount *= 1.25F;
        }
        // Critical strike.
        if (attacker.getRandom().nextFloat() < RpgStats.critChance(rpg) + RpgGear.heldCritBonus(attacker)) {
            amount *= RpgStats.CRIT_MULTIPLIER;
            spawn(attacker, target, ParticleTypes.CRIT, 12);
            attacker.level().playSound(null, target.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.1F);
        }
        // Durability: chance to spare the weapon's wear.
        if (rpg.hasSkill(SkillTrees.W_DURABILITY) && attacker.getRandom().nextFloat() < 0.35F) {
            restoreDurability(attacker.getMainHandItem());
        }
        event.setAmount(amount);
    }

    private static void handleIncoming(LivingHurtEvent event, DamageSource source, ServerPlayer victim) {
        PlayerRpg rpg = PlayerRpgProvider.get(victim).orElse(null);
        if (rpg == null) {
            return;
        }
        rpg.setLastCombatTime(victim.level().getGameTime());

        // Perfect parry: while blocking, a chance to negate the hit entirely.
        if (victim.isBlocking() && rpg.hasSkill(SkillTrees.W_PARRY)
                && source.getEntity() != null && victim.getRandom().nextFloat() < 0.30F) {
            event.setCanceled(true);
            if (source.getEntity() instanceof LivingEntity foe) {
                var push = foe.position().subtract(victim.position()).normalize().scale(0.6D);
                foe.push(push.x, 0.2D, push.z);
            }
            victim.level().playSound(null, victim.blockPosition(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.4F);
            spawn(victim, victim, ParticleTypes.ENCHANTED_HIT, 10);
            return;
        }

        float amount = event.getAmount() * (1.0F - RpgStats.damageReduction(rpg));
        if (victim.isBlocking()) {
            amount *= 1.0F - RpgStats.blockingReduction(rpg);
        }
        event.setAmount(amount);
    }

    /** Lifesteal: killing an enemy with a Warrior build heals the player. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerRpgProvider.get(player).ifPresent(rpg -> {
                if (rpg.hasSkill(SkillTrees.W_LIFESTEAL) && player.getHealth() < player.getMaxHealth()) {
                    player.heal(4.0F);
                    spawn(player, player, ParticleTypes.HEART, 4);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRpgProvider.get(player).ifPresent(rpg ->
                    event.setDamageMultiplier(event.getDamageMultiplier() * RpgStats.fallDamageMultiplier(rpg)));
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player
                && !event.getEffectInstance().getEffect().isBeneficial()) {
            PlayerRpg rpg = PlayerRpgProvider.get(player).orElse(null);
            if (rpg != null && player.getRandom().nextFloat() < RpgStats.effectResistChance(rpg)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerRpgProvider.get(event.getEntity()).ifPresent(rpg ->
                event.setNewSpeed(event.getNewSpeed() * RpgStats.miningSpeedMultiplier(rpg)));
    }

    // --- helpers ---

    public static void restoreDurability(ItemStack stack) {
        if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    private static void spawn(ServerPlayer viewer, Entity at, net.minecraft.core.particles.ParticleOptions p, int n) {
        if (viewer.level() instanceof ServerLevel level) {
            level.sendParticles(p, at.getX(), at.getY(0.7D), at.getZ(), n, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }
}
