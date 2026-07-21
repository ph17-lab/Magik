package com.magik.combat;

import com.magik.MagikMod;
import com.magik.entity.MagicBoltEntity;
import com.magik.item.Dagger;
import com.magik.item.HeavyWeapon;
import com.magik.item.RpgGear;
import com.magik.skills.SkillTrees;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Combat hooks that turn RPG attributes and passives into gameplay:
 * Strength (melee/heavy damage), Precision (ranged damage + crits),
 * Resistance (mitigation, effect resist), Agility (fall damage),
 * Mining Speed (block breaking), plus the Espadachim combat state
 * (combo, charged strike, parry/riposte), heavy stuns/armor pierce and
 * the Defesa mitigation passives.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class CombatEvents {

    private CombatEvents() {
    }

    /** Blocks attacks with gear whose level/attribute requirements are not met. */
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

    // ------------------------------------------------------------------
    // Outgoing damage (player attacking)
    // ------------------------------------------------------------------

    private static void handleOutgoing(LivingHurtEvent event, DamageSource source, ServerPlayer attacker) {
        PlayerRpg rpg = PlayerRpgProvider.get(attacker).orElse(null);
        if (rpg == null) {
            return;
        }
        long gameTime = attacker.level().getGameTime();
        rpg.setLastCombatTime(gameTime);
        float amount = event.getAmount();
        Entity direct = source.getDirectEntity();
        boolean melee = direct == attacker;
        boolean ranged = direct instanceof Projectile && !(direct instanceof MagicBoltEntity);
        boolean dagger = melee && attacker.getMainHandItem().getItem() instanceof Dagger;
        boolean sword = melee && attacker.getMainHandItem().getItem() instanceof SwordItem
                && !(attacker.getMainHandItem().getItem() instanceof HeavyWeapon) && !dagger;
        boolean heavy = melee && attacker.getMainHandItem().getItem() instanceof HeavyWeapon;
        boolean forceCrit = false;

        if (melee) {
            amount *= RpgStats.meleeDamageMultiplier(rpg);
            if (heavy) {
                amount *= 1.0F + RpgStats.heavyWeaponBonus(rpg);
            }
            if (sword) {
                amount *= 1.0F + RpgStats.swordDamageBonus(rpg);
            }
            if (dagger) {
                // Dual-wielding two daggers rewards the rogue with extra bite.
                if (attacker.getOffhandItem().getItem() instanceof Dagger) {
                    amount *= 1.15F;
                }
                // Shadow Veil: the first strike from stealth always crits and
                // ends the veil.
                if (gameTime < rpg.getShadowVeilUntil()) {
                    forceCrit = true;
                    rpg.setShadowVeilUntil(0L);
                    attacker.removeEffect(MobEffects.INVISIBILITY);
                    com.magik.combat.RpgAttributeApplier.apply(attacker, rpg);
                }
            }

            // Sword Combo: escalating damage per hit while the combo is active.
            if (gameTime <= rpg.getComboUntil()) {
                amount *= 1.0F + RpgStats.COMBO_DAMAGE_PER_STACK * rpg.getComboStacks();
                rpg.incrementCombo(RpgStats.COMBO_MAX_STACKS);
            }

            // Charged Strike: one buffed hit, then the buff is consumed.
            if (gameTime <= rpg.getChargedStrikeUntil()) {
                amount *= 2.0F;
                rpg.setChargedStrikeUntil(0L);
                knockBack(event.getEntity(), attacker, 1.0F);
                spawnBurst(attacker, event.getEntity(), ParticleTypes.SWEEP_ATTACK, 6);
            }

            // Riposte: bonus damage right after a successful parry.
            if (gameTime <= rpg.getRiposteUntil()) {
                amount *= 2.0F;
                rpg.setRiposteUntil(0L);
                spawnBurst(attacker, event.getEntity(), ParticleTypes.ENCHANTED_HIT, 10);
            }

            // Stunning Blows: heavy hits may stun.
            if (heavy && attacker.getRandom().nextFloat() < RpgStats.stunChance(rpg)) {
                stun(event.getEntity(), 40);
            }

            // Shield Break: heavy hits pierce armor and disable raised shields.
            if (heavy) {
                float pierce = RpgStats.armorPierce(rpg);
                if (pierce > 0.0F) {
                    amount += event.getEntity().getArmorValue() * 0.04F * pierce * amount;
                }
                if (event.getEntity() instanceof Player blockingPlayer && blockingPlayer.isBlocking()) {
                    blockingPlayer.disableShield(true);
                }
            }
        } else if (ranged) {
            amount *= RpgStats.rangedDamageMultiplier(rpg);
        }

        // Precision-based critical strikes (melee and ranged alike). Shadow
        // Master lends daggers extra crit; a veiled strike always crits.
        float critChance = RpgStats.critChance(rpg, sword, ranged) + RpgGear.heldCritBonus(attacker);
        if (dagger && rpg.hasSkill(SkillTrees.SHADOW_MASTER)) {
            critChance += 0.15F;
        }
        if ((melee || ranged) && (forceCrit || attacker.getRandom().nextFloat() < critChance)) {
            amount *= RpgStats.CRIT_MULTIPLIER;
            spawnBurst(attacker, event.getEntity(), ParticleTypes.CRIT, 12);
            attacker.level().playSound(null, event.getEntity().blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.1F);
        }
        event.setAmount(amount);
    }

    // ------------------------------------------------------------------
    // Incoming damage (player defending)
    // ------------------------------------------------------------------

    private static void handleIncoming(LivingHurtEvent event, DamageSource source, ServerPlayer victim) {
        PlayerRpg rpg = PlayerRpgProvider.get(victim).orElse(null);
        if (rpg == null) {
            return;
        }
        long gameTime = victim.level().getGameTime();
        rpg.setLastCombatTime(gameTime);

        // Counter Attack: perfect parry negates the hit and arms a riposte.
        if (source.getEntity() != null && gameTime <= rpg.getParryUntil()) {
            event.setCanceled(true);
            rpg.setParryUntil(0L);
            rpg.setRiposteUntil(gameTime + 100);
            victim.level().playSound(null, victim.blockPosition(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.3F);
            spawnBurst(victim, victim, ParticleTypes.ENCHANTED_HIT, 12);
            return;
        }

        float amount = event.getAmount();
        amount *= 1.0F - RpgStats.damageReduction(rpg);
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= RpgStats.fireDamageMultiplier(rpg);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            amount *= RpgStats.blastDamageMultiplier(rpg);
        }
        if (victim.isBlocking()) {
            amount *= 1.0F - RpgStats.blockingReduction(rpg);
            amount = Math.max(0.0F, amount - RpgGear.heldShieldFlatReduction(victim));
        }
        event.setAmount(amount);
    }

    // ------------------------------------------------------------------
    // Other hooks
    // ------------------------------------------------------------------

    /** Shadow Master: killing an enemy slips the rogue into enhanced stealth. */
    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            PlayerRpgProvider.get(killer).ifPresent(rpg -> {
                if (rpg.hasSkill(SkillTrees.SHADOW_MASTER)
                        && killer.getMainHandItem().getItem() instanceof Dagger) {
                    long gameTime = killer.level().getGameTime();
                    rpg.setStealthUntil(gameTime + 100);
                    killer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
                    killer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));
                }
            });
        }
    }

    /** Agility softens falls (never more than -50%). */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRpgProvider.get(player).ifPresent(rpg ->
                    event.setDamageMultiplier(event.getDamageMultiplier() * RpgStats.fallDamageMultiplier(rpg)));
        }
    }

    /** Resistance (and Unbreakable Fortress) can shrug off harmful effects. */
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

    /** Mining Speed (and a touch of Agility) accelerates block breaking. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerRpgProvider.get(event.getEntity()).ifPresent(rpg ->
                event.setNewSpeed(event.getNewSpeed() * RpgStats.miningSpeedMultiplier(rpg)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Applies a short stun (heavy slow + weakness) with feedback particles. */
    public static void stun(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY(1.9D), target.getZ(), 8, 0.3D, 0.1D, 0.3D, 0.05D);
        }
    }

    private static void knockBack(LivingEntity target, Player from, float strength) {
        var push = target.position().subtract(from.position()).normalize().scale(strength);
        target.push(push.x, 0.3D, push.z);
    }

    private static void spawnBurst(ServerPlayer viewerSource, Entity at,
                                   net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (viewerSource.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    at.getX(), at.getY(0.75D), at.getZ(), count, 0.3D, 0.3D, 0.3D, 0.15D);
        }
    }
}
