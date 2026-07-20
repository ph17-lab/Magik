package com.magik.skills;

import com.magik.combat.CombatEvents;
import com.magik.config.MagikServerConfig;
import com.magik.entity.MagicBoltEntity;
import com.magik.network.MagikNetwork;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgAttribute;
import com.magik.player.RpgStats;
import com.magik.progression.RpgXp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Server-side execution of every active skill. All costs, cooldowns and
 * effects are validated and applied here; the client only ever sends
 * "I want to cast X" requests.
 */
public final class SkillCasting {

    private SkillCasting() {
    }

    /**
     * Attempts to cast an active skill. Validates unlock state, cooldown and
     * resource costs (with Intelligence discounts) before executing.
     */
    public static void cast(ServerPlayer player, String skillId) {
        Skill skill = SkillTrees.get(skillId);
        if (skill == null || skill.getType() != Skill.Type.ACTIVE) {
            return;
        }
        PlayerRpg rpg = PlayerRpgProvider.get(player).orElse(null);
        if (rpg == null || !rpg.hasSkill(skillId)) {
            return;
        }

        long gameTime = player.level().getGameTime();
        if (rpg.isOnCooldown(skillId, gameTime)) {
            return;
        }

        float manaCost = skill.getManaCost() * RpgStats.manaCostMultiplier(rpg);
        float staminaCost = skill.getStaminaCost();
        if (rpg.getMana() < manaCost || rpg.getStamina() < staminaCost) {
            player.displayClientMessage(Component.translatable(
                    manaCost > 0 ? "message.magik.no_mana" : "message.magik.no_stamina"), true);
            return;
        }

        if (!execute(player, rpg, skillId)) {
            return;
        }

        rpg.consumeMana(manaCost);
        rpg.consumeStamina(staminaCost, gameTime);
        int cooldown = (int) (skill.getCooldownTicks() * RpgStats.cooldownMultiplier(rpg));
        rpg.setCooldown(skillId, gameTime + cooldown);
        RpgXp.grant(player, MagikServerConfig.SKILL_USE_XP.get());
        MagikNetwork.syncFull(player, rpg);
    }

    /** Runs the actual effect. @return false if the cast should be aborted for free. */
    private static boolean execute(ServerPlayer player, PlayerRpg rpg, String skillId) {
        long gameTime = player.level().getGameTime();
        return switch (skillId) {
            // --- Arcano ---
            case SkillTrees.FIREBALL -> shootBolt(player, rpg, 7.0F, MagicBoltEntity.Variant.FIRE,
                    SoundEvents.BLAZE_SHOOT, 1.0F);
            case SkillTrees.ICE_SHARD -> shootBolt(player, rpg, 6.0F, MagicBoltEntity.Variant.FROST,
                    SoundEvents.GLASS_BREAK, 1.4F);
            case SkillTrees.LIGHTNING -> castLightning(player, rpg);
            case SkillTrees.ARCANE_EXPLOSION -> castArcaneExplosion(player, rpg);
            case SkillTrees.HEAL -> castHeal(player, rpg);
            case SkillTrees.ARCANE_SHIELD -> castAbsorption(player, 1, 400,
                    ParticleTypes.ENCHANT, SoundEvents.ILLUSIONER_CAST_SPELL);
            case SkillTrees.SHORT_TELEPORT -> castTeleport(player);
            case SkillTrees.WEAPON_SUMMON -> castWeaponSummon(player, gameTime);

            // --- Espadachim ---
            case SkillTrees.SWORD_COMBO -> {
                rpg.startCombo(gameTime + 160);
                feedback(player, ParticleTypes.SWEEP_ATTACK, 4, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F);
                yield true;
            }
            case SkillTrees.SPIN_SLASH -> castAoeMelee(player, rpg, 3.5D, 5.0F, 0.4F,
                    ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, false);
            case SkillTrees.CHARGED_STRIKE -> {
                rpg.setChargedStrikeUntil(gameTime + 100);
                feedback(player, ParticleTypes.ANGRY_VILLAGER, 6, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F);
                yield true;
            }
            case SkillTrees.AREA_SLASH -> castCone(player, rpg, 4.5D, 7.0F,
                    ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP);
            case SkillTrees.DASH -> castDash(player, rpg);
            case SkillTrees.COUNTER_ATTACK -> {
                rpg.setParryUntil(gameTime + 30);
                feedback(player, ParticleTypes.ENCHANTED_HIT, 8, SoundEvents.SHIELD_BLOCK, 1.5F);
                yield true;
            }

            // --- Arqueiro ---
            case SkillTrees.EXPLOSIVE_ARROW -> armArrow(player, rpg, PlayerRpg.ArrowEffect.EXPLOSIVE, gameTime);
            case SkillTrees.FROST_ARROW -> armArrow(player, rpg, PlayerRpg.ArrowEffect.FROST, gameTime);
            case SkillTrees.ELECTRIC_ARROW -> armArrow(player, rpg, PlayerRpg.ArrowEffect.ELECTRIC, gameTime);
            case SkillTrees.MULTI_ARROW -> castMultiArrow(player, rpg);

            // --- Armamento Pesado ---
            case SkillTrees.GIANT_CLEAVE -> castCone(player, rpg, 4.0D, 9.0F,
                    ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_STRONG);
            case SkillTrees.HAMMER_SLAM -> castHammerSlam(player, rpg);
            case SkillTrees.AREA_STRIKE -> castAoeMelee(player, rpg, 3.5D, 6.0F, 0.9F,
                    ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE, false);
            case SkillTrees.TOTAL_DESTRUCTION -> castAoeMelee(player, rpg, 5.0D, 14.0F, 1.2F,
                    ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE, true);

            // --- Defesa ---
            case SkillTrees.PROTECTIVE_SHIELD -> castAbsorption(player, 2, 600,
                    ParticleTypes.COMPOSTER, SoundEvents.BEACON_ACTIVATE);

            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // Arcano
    // ------------------------------------------------------------------

    private static boolean shootBolt(ServerPlayer player, PlayerRpg rpg, float baseDamage,
                                     MagicBoltEntity.Variant variant, SoundEvent sound, float pitch) {
        float damage = baseDamage * RpgStats.magicDamageMultiplier(rpg);
        MagicBoltEntity bolt = new MagicBoltEntity(player.level(), player, damage, variant);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.5F);
        player.level().addFreshEntity(bolt);
        playSound(player, sound, pitch);
        return true;
    }

    /** Calls down a (visual-only) lightning bolt on the aimed position + magic damage. */
    private static boolean castLightning(ServerPlayer player, PlayerRpg rpg) {
        Vec3 target = rayTarget(player, 24.0D);
        float damage = 8.0F * RpgStats.magicDamageMultiplier(rpg);
        ServerLevel level = (ServerLevel) player.level();

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(BlockPos.containing(target)).inflate(2.0D), hostileTo(player))) {
            entity.hurt(player.damageSources().indirectMagic(player, player), damage);
        }
        return true;
    }

    private static boolean castArcaneExplosion(ServerPlayer player, PlayerRpg rpg) {
        float damage = 9.0F * RpgStats.magicDamageMultiplier(rpg);
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(5.0D), hostileTo(player))) {
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
        }
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                player.getX(), player.getY(0.5D), player.getZ(), 120, 3.0D, 0.8D, 3.0D, 0.1D);
        playSound(player, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.2F);
        return true;
    }

    private static boolean castHeal(ServerPlayer player, PlayerRpg rpg) {
        if (player.getHealth() >= player.getMaxHealth()) {
            return false; // Don't waste mana at full health.
        }
        float healing = 4.0F + 0.25F * rpg.getAttribute(RpgAttribute.INTELLIGENCE);
        player.heal(healing);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        feedback(player, ParticleTypes.HEART, 8, SoundEvents.PLAYER_LEVELUP, 1.6F);
        return true;
    }

    private static boolean castAbsorption(ServerPlayer player, int amplifier, int duration,
                                          ParticleOptions particle, SoundEvent sound) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        feedback(player, particle, 30, sound, 1.2F);
        return true;
    }

    /** Teleports up to 8 blocks in the look direction, stopping at walls. */
    private static boolean castTeleport(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(8.0D));
        BlockHitResult hit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getLocation().subtract(player.getLookAngle().scale(0.5D));

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY(0.5D), player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.1D);
        player.teleportTo(target.x, Math.max(level.getMinBuildHeight(), target.y - player.getEyeHeight()), target.z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY(0.5D), player.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.1D);
        playSound(player, SoundEvents.ENDERMAN_TELEPORT, 1.0F);
        return true;
    }

    /** Summons spectral blades that strike nearby enemies for a while (aura). */
    private static boolean castWeaponSummon(ServerPlayer player, long gameTime) {
        PlayerRpgProvider.get(player).ifPresent(rpg -> rpg.setWeaponSummonUntil(gameTime + 300));
        feedback(player, ParticleTypes.END_ROD, 40, SoundEvents.EVOKER_CAST_SPELL, 0.9F);
        return true;
    }

    /** Ticked from RpgEvents: the summoned blades strike a nearby enemy. */
    public static void tickWeaponSummon(ServerPlayer player, PlayerRpg rpg) {
        ServerLevel level = (ServerLevel) player.level();
        float damage = 4.0F * RpgStats.magicDamageMultiplier(rpg);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(4.0D), hostileTo(player));
        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(player.getRandom().nextInt(targets.size()));
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    target.getX(), target.getY(0.75D), target.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY(1.0D), player.getZ(), 3, 0.8D, 0.5D, 0.8D, 0.02D);
    }

    // ------------------------------------------------------------------
    // Espadachim / Armamento Pesado
    // ------------------------------------------------------------------

    /** 360-degree melee burst around the player. */
    private static boolean castAoeMelee(ServerPlayer player, PlayerRpg rpg, double radius, float baseDamage,
                                        float knockback, ParticleOptions particle, SoundEvent sound,
                                        boolean heavyScaling) {
        float damage = baseDamage * RpgStats.meleeDamageMultiplier(rpg);
        if (heavyScaling) {
            damage *= 1.0F + RpgStats.heavyWeaponBonus(rpg);
        }
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(radius), hostileTo(player))) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            if (knockback > 0.0F) {
                Vec3 push = target.position().subtract(player.position()).normalize()
                        .scale(knockback).add(0.0D, 0.3D, 0.0D);
                target.push(push.x, push.y, push.z);
            }
        }
        level.sendParticles(particle,
                player.getX(), player.getY(0.9D), player.getZ(), 8, radius * 0.5D, 0.3D, radius * 0.5D, 0.0D);
        playSound(player, sound, 0.9F);
        return true;
    }

    /** Frontal cone strike (Area Slash / Giant Cleave). */
    private static boolean castCone(ServerPlayer player, PlayerRpg rpg, double range, float baseDamage,
                                    ParticleOptions particle, SoundEvent sound) {
        float damage = baseDamage * RpgStats.meleeDamageMultiplier(rpg)
                * (1.0F + RpgStats.heavyWeaponBonus(rpg) * 0.5F);
        Vec3 look = player.getLookAngle();
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), hostileTo(player))) {
            Vec3 direction = target.position().subtract(player.position()).normalize();
            if (direction.dot(new Vec3(look.x, 0, look.z).normalize()) > 0.5D) {
                target.hurt(player.damageSources().playerAttack(player), damage);
            }
        }
        Vec3 front = player.position().add(look.scale(2.0D));
        level.sendParticles(particle, front.x, front.y + 1.0D, front.z, 6, 1.2D, 0.4D, 1.2D, 0.0D);
        playSound(player, sound, 0.8F);
        return true;
    }

    private static boolean castHammerSlam(ServerPlayer player, PlayerRpg rpg) {
        float damage = 8.0F * RpgStats.meleeDamageMultiplier(rpg) * (1.0F + RpgStats.heavyWeaponBonus(rpg));
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(4.0D), hostileTo(player))) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            CombatEvents.stun(target, 50);
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY(), player.getZ(), 5, 1.5D, 0.2D, 1.5D, 0.0D);
        playSound(player, SoundEvents.ANVIL_LAND, 0.6F);
        return true;
    }

    /** Stamina-powered dash in the player's look direction (Espadachim skill). */
    private static boolean castDash(ServerPlayer player, PlayerRpg rpg) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.0D, look.z).normalize().scale(1.6D).add(0.0D, 0.25D, 0.0D);
        player.setDeltaMovement(dash);
        player.hurtMarked = true; // Forces the velocity packet to the client.
        feedback(player, ParticleTypes.CLOUD, 12, SoundEvents.ENDER_DRAGON_FLAP, 1.5F);
        return true;
    }

    // ------------------------------------------------------------------
    // Arqueiro
    // ------------------------------------------------------------------

    /** Arms the next fired arrow with a special effect (10 second window). */
    private static boolean armArrow(ServerPlayer player, PlayerRpg rpg,
                                    PlayerRpg.ArrowEffect effect, long gameTime) {
        rpg.setNextArrowEffect(effect, gameTime + 200);
        feedback(player, ParticleTypes.ENCHANT, 12, SoundEvents.ARROW_HIT_PLAYER, 0.8F);
        return true;
    }

    /** Instantly fires a fan of three spectral arrows. */
    private static boolean castMultiArrow(ServerPlayer player, PlayerRpg rpg) {
        for (int i = -1; i <= 1; i++) {
            Arrow arrow = new Arrow(player.level(), player);
            arrow.setBaseDamage(arrow.getBaseDamage() * RpgStats.rangedDamageMultiplier(rpg));
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 8.0F, 0.0F, 2.8F, 1.0F);
            player.level().addFreshEntity(arrow);
        }
        playSound(player, SoundEvents.ARROW_SHOOT, 0.9F);
        return true;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Predicate<LivingEntity> hostileTo(ServerPlayer player) {
        return entity -> entity != player && entity.isAlive()
                && (entity instanceof Mob || entity instanceof ServerPlayer other && player.canHarmPlayer(other));
    }

    private static Vec3 rayTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getLocation();
    }

    private static void feedback(ServerPlayer player, ParticleOptions particle, int count,
                                 SoundEvent sound, float pitch) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    player.getX(), player.getY(1.0D), player.getZ(), count, 0.4D, 0.6D, 0.4D, 0.05D);
        }
        playSound(player, sound, pitch);
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, pitch);
    }
}
