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
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

        // Advanced Arcane spells are the staff's attack kit: a staff must be in hand.
        if (skill.getTree() == SkillTrees.Tree.ADVANCED_ARCANE && !holdingStaff(player)) {
            player.displayClientMessage(Component.translatable("message.magik.need_staff"), true);
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
                    SkillFx.ARCANE_A, SkillFx.ARCANE_B, SoundEvents.ILLUSIONER_CAST_SPELL);
            case SkillTrees.SHORT_TELEPORT -> castTeleport(player);
            case SkillTrees.WEAPON_SUMMON -> castWeaponSummon(player, gameTime);

            // --- Espadachim ---
            case SkillTrees.SWORD_COMBO -> {
                rpg.startCombo(gameTime + 160);
                SkillFx.slashArc(level(player), player, 1.6D, SkillFx.SWORD_A, SkillFx.SWORD_B);
                playSound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F);
                yield true;
            }
            case SkillTrees.SPIN_SLASH -> castAoeMelee(player, rpg, 3.5D, 5.0F, 0.4F,
                    SkillFx.SWORD_A, SkillFx.SWORD_B, SoundEvents.PLAYER_ATTACK_SWEEP, false);
            case SkillTrees.CHARGED_STRIKE -> {
                rpg.setChargedStrikeUntil(gameTime + 100);
                SkillFx.charge(level(player), player, SkillFx.GOLD);
                playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F);
                yield true;
            }
            case SkillTrees.AREA_SLASH -> castCone(player, rpg, 4.5D, 7.0F,
                    SkillFx.SWORD_A, SkillFx.SWORD_B, SoundEvents.PLAYER_ATTACK_SWEEP);
            case SkillTrees.DASH -> castDash(player, rpg);
            case SkillTrees.COUNTER_ATTACK -> {
                rpg.setParryUntil(gameTime + 30);
                SkillFx.ring(level(player), player.position().add(0.0D, 1.0D, 0.0D), 0.9D,
                        SkillFx.DEFENSE_B, SkillFx.SWORD_B, 1.1F, 0.15D);
                playSound(player, SoundEvents.SHIELD_BLOCK, 1.5F);
                yield true;
            }

            // --- Arqueiro ---
            case SkillTrees.EXPLOSIVE_ARROW -> armArrow(player, rpg, PlayerRpg.ArrowEffect.EXPLOSIVE, gameTime);
            case SkillTrees.FROST_ARROW -> armArrow(player, rpg, PlayerRpg.ArrowEffect.FROST, gameTime);
            case SkillTrees.ELECTRIC_ARROW -> armArrow(player, rpg, PlayerRpg.ArrowEffect.ELECTRIC, gameTime);
            case SkillTrees.MULTI_ARROW -> castMultiArrow(player, rpg);

            // --- Armamento Pesado ---
            case SkillTrees.GIANT_CLEAVE -> castCone(player, rpg, 4.0D, 9.0F,
                    SkillFx.HEAVY_A, SkillFx.HEAVY_B, SoundEvents.PLAYER_ATTACK_STRONG);
            case SkillTrees.HAMMER_SLAM -> castHammerSlam(player, rpg);
            case SkillTrees.AREA_STRIKE -> castAoeMelee(player, rpg, 3.5D, 6.0F, 0.9F,
                    SkillFx.HEAVY_A, SkillFx.HEAVY_B, SoundEvents.GENERIC_EXPLODE, false);
            case SkillTrees.TOTAL_DESTRUCTION -> castAoeMelee(player, rpg, 5.0D, 14.0F, 1.2F,
                    SkillFx.HEAVY_A, SkillFx.HEAVY_B, SoundEvents.GENERIC_EXPLODE, true);

            // --- Defesa ---
            case SkillTrees.PROTECTIVE_SHIELD -> castAbsorption(player, 2, 600,
                    SkillFx.DEFENSE_A, SkillFx.DEFENSE_B, SoundEvents.BEACON_ACTIVATE);

            // --- Arcano Avançado ---
            case SkillTrees.SHADOW_ORB -> castShadowOrb(player, rpg);
            case SkillTrees.ARCANE_LANCE -> castArcaneLance(player, rpg);
            case SkillTrees.ETHEREAL_EXPLOSION -> castEtherealExplosion(player, rpg);
            case SkillTrees.METEOR_SHOWER -> castMeteorShower(player, rpg);
            case SkillTrees.ARCANE_BARRIER -> castArcaneBarrier(player);
            case SkillTrees.FLOATING_SPHERES -> {
                rpg.setFloatingSpheresUntil(gameTime + 400);
                SkillFx.ring(level(player), player.position().add(0.0D, 1.4D, 0.0D), 1.2D,
                        SkillFx.ARCANE_A, SkillFx.ARCANE_B, 1.2F, 0.1D);
                playSound(player, SoundEvents.EVOKER_CAST_SPELL, 1.3F);
                yield true;
            }
            case SkillTrees.CHAOS_RAY -> castChaosRay(player, rpg);
            case SkillTrees.GRAVITY_NOVA -> castGravityNova(player, rpg);
            case SkillTrees.DIMENSIONAL_RIFT -> castDimensionalRift(player, rpg);
            case SkillTrees.OFFENSIVE_TELEPORT -> castOffensiveTeleport(player, rpg);
            case SkillTrees.ARCANE_BLADES -> castArcaneBlades(player, rpg);
            case SkillTrees.NULLIFICATION_FIELD -> castNullificationField(player);
            case SkillTrees.ARCANE_CHAIN -> castArcaneChain(player, rpg);
            case SkillTrees.VOID_PRESENCE -> {
                rpg.setVoidPresenceUntil(gameTime + 300);
                SkillFx.sphereBurst(level(player), player.position().add(0.0D, 1.0D, 0.0D),
                        SkillFx.ARCANE_B, SkillFx.ARCANE_A, 32, 0.2D);
                playSound(player, SoundEvents.WITHER_SPAWN, 1.8F);
                yield true;
            }
            case SkillTrees.ARCANE_COMET -> castArcaneComet(player, rpg);

            default -> false;
        };
    }

    private static boolean holdingStaff(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof com.magik.item.StaffItem
                || player.getOffhandItem().getItem() instanceof com.magik.item.StaffItem;
    }

    /** Void Presence: +30% spell damage while the buff is active. */
    public static float voidBonus(PlayerRpg rpg, long gameTime) {
        return gameTime < rpg.getVoidPresenceUntil() ? 1.3F : 1.0F;
    }

    // ------------------------------------------------------------------
    // Arcano
    // ------------------------------------------------------------------

    private static boolean shootBolt(ServerPlayer player, PlayerRpg rpg, float baseDamage,
                                     MagicBoltEntity.Variant variant, SoundEvent sound, float pitch) {
        float damage = baseDamage * RpgStats.magicDamageMultiplier(rpg)
                * voidBonus(rpg, player.level().getGameTime());
        MagicBoltEntity bolt = new MagicBoltEntity(player.level(), player, damage, variant);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.5F);
        player.level().addFreshEntity(bolt);
        // Muzzle flash in the element's colors at the cast point.
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(1.0D));
        level(player).sendParticles(new DustParticleOptions(variant.primary, 1.3F),
                muzzle.x, muzzle.y, muzzle.z, 8, 0.12D, 0.12D, 0.12D, 0.05D);
        level(player).sendParticles(new DustParticleOptions(variant.secondary, 1.0F),
                muzzle.x, muzzle.y, muzzle.z, 6, 0.2D, 0.2D, 0.2D, 0.08D);
        playSound(player, sound, pitch);
        return true;
    }

    /** Calls down a custom BLUE bolt from the sky onto the aimed position. */
    private static boolean castLightning(ServerPlayer player, PlayerRpg rpg) {
        Vec3 target = rayTarget(player, 24.0D);
        float damage = 8.0F * RpgStats.magicDamageMultiplier(rpg);
        ServerLevel level = (ServerLevel) player.level();

        SkillFx.skyBolt(level, target);
        level.playSound(null, BlockPos.containing(target), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 1.0F, 1.5F);
        level.playSound(null, BlockPos.containing(target), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 0.8F, 1.2F);
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
        SkillFx.shockwave(level, player.position(), 5.0D, SkillFx.ARCANE_A, SkillFx.ARCANE_B);
        SkillFx.sphereBurst(level, player.position().add(0.0D, 1.0D, 0.0D),
                SkillFx.ARCANE_A, SkillFx.ARCANE_B, 48, 0.45D);
        level.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY(1.0D), player.getZ(), 40, 2.0D, 1.0D, 2.0D, 0.15D);
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                player.getX(), player.getY(0.5D), player.getZ(), 60, 3.0D, 0.8D, 3.0D, 0.1D);
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY(1.0D), player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
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
        SkillFx.helix(level(player), player, SkillFx.HEAL_GREEN, SkillFx.GOLD);
        feedback(player, ParticleTypes.HEART, 4, SoundEvents.PLAYER_LEVELUP, 1.6F);
        return true;
    }

    private static boolean castAbsorption(ServerPlayer player, int amplifier, int duration,
                                          org.joml.Vector3f colorA, org.joml.Vector3f colorB,
                                          SoundEvent sound) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        SkillFx.helix(level(player), player, colorA, colorB);
        SkillFx.ring(level(player), player.position().add(0.0D, 0.1D, 0.0D), 1.4D,
                colorA, colorB, 1.2F, 0.1D);
        playSound(player, sound, 1.2F);
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
        SkillFx.ring(level(player), player.position().add(0.0D, 1.2D, 0.0D), 1.6D,
                SkillFx.ARCANE_A, SkillFx.SWORD_B, 1.2F, 0.1D);
        feedback(player, ParticleTypes.END_ROD, 25, SoundEvents.EVOKER_CAST_SPELL, 0.9F);
        return true;
    }

    /**
     * Ticked from RpgEvents every tick while active: three purple spectral
     * blades orbit the caster and periodically lunge at a nearby enemy.
     */
    public static void tickWeaponSummon(ServerPlayer player, PlayerRpg rpg, long gameTime) {
        ServerLevel level = (ServerLevel) player.level();

        // Orbiting blades (drawn every other tick to stay light).
        if (gameTime % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                double theta = gameTime * 0.18D + i * (Math.PI * 2.0D / 3.0D);
                Vec3 base = player.position().add(
                        Math.cos(theta) * 1.4D, 0.7D, Math.sin(theta) * 1.4D);
                SkillFx.blade(level, base, SkillFx.ARCANE_B, SkillFx.ARCANE_A);
            }
        }

        // Strike once a second: a blade slashes through a random nearby enemy.
        if (gameTime % 20 != 0) {
            return;
        }
        float damage = 4.0F * RpgStats.magicDamageMultiplier(rpg);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(4.0D), hostileTo(player));
        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(player.getRandom().nextInt(targets.size()));
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            SkillFx.bladeSlash(level, player.position(), target, SkillFx.ARCANE_A, SkillFx.ARCANE_B);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    // ------------------------------------------------------------------
    // Espadachim / Armamento Pesado
    // ------------------------------------------------------------------

    /** 360-degree melee burst around the player with a colored shockwave. */
    private static boolean castAoeMelee(ServerPlayer player, PlayerRpg rpg, double radius, float baseDamage,
                                        float knockback, org.joml.Vector3f colorA, org.joml.Vector3f colorB,
                                        SoundEvent sound, boolean heavyScaling) {
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
        SkillFx.shockwave(level, player.position(), radius, colorA, colorB);
        if (heavyScaling) {
            level.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY(0.5D), player.getZ(), 3, radius * 0.4D, 0.3D, radius * 0.4D, 0.0D);
        }
        playSound(player, sound, 0.9F);
        return true;
    }

    /** Frontal cone strike (Area Slash / Giant Cleave) with a crescent slash arc. */
    private static boolean castCone(ServerPlayer player, PlayerRpg rpg, double range, float baseDamage,
                                    org.joml.Vector3f colorA, org.joml.Vector3f colorB, SoundEvent sound) {
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
        SkillFx.slashArc(level, player, range * 0.55D, colorA, colorB);
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
        SkillFx.shockwave(level, player.position(), 4.0D, SkillFx.HEAVY_A, SkillFx.HEAVY_B);
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY(), player.getZ(), 3, 1.2D, 0.2D, 1.2D, 0.0D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY(0.2D), player.getZ(), 8, 1.5D, 0.1D, 1.5D, 0.01D);
        playSound(player, SoundEvents.ANVIL_LAND, 0.6F);
        return true;
    }

    /** Stamina-powered dash in the player's look direction (Espadachim skill). */
    private static boolean castDash(ServerPlayer player, PlayerRpg rpg) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.0D, look.z).normalize().scale(1.6D).add(0.0D, 0.25D, 0.0D);
        player.setDeltaMovement(dash);
        player.hurtMarked = true; // Forces the velocity packet to the client.
        // A trail of silver-red afterimage dust left along the launch direction.
        ServerLevel level = level(player);
        for (int i = 0; i < 8; i++) {
            Vec3 behind = player.position().subtract(dash.normalize().scale(i * 0.35D));
            level.sendParticles(new DustParticleOptions(
                            i % 2 == 0 ? SkillFx.SWORD_B : SkillFx.SWORD_A, 1.0F),
                    behind.x, behind.y + 0.6D, behind.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        feedback(player, ParticleTypes.CLOUD, 6, SoundEvents.ENDER_DRAGON_FLAP, 1.5F);
        return true;
    }

    // ------------------------------------------------------------------
    // Arqueiro
    // ------------------------------------------------------------------

    /** Arms the next fired arrow with a special effect (10 second window). */
    private static boolean armArrow(ServerPlayer player, PlayerRpg rpg,
                                    PlayerRpg.ArrowEffect effect, long gameTime) {
        rpg.setNextArrowEffect(effect, gameTime + 200);
        SkillFx.charge(level(player), player, SkillFx.ARCHER_A);
        playSound(player, SoundEvents.ARROW_HIT_PLAYER, 0.8F);
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
        SkillFx.slashArc(level(player), player, 1.2D, SkillFx.ARCHER_A, SkillFx.ARCHER_B);
        playSound(player, SoundEvents.ARROW_SHOOT, 0.9F);
        return true;
    }

    // ------------------------------------------------------------------
    // Arcano Avançado (staff attack kit)
    // ------------------------------------------------------------------

    /** Orbe das Sombras: slow homing orb that explodes in an area. */
    private static boolean castShadowOrb(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 8.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        MagicBoltEntity orb = new MagicBoltEntity(player.level(), player, damage,
                MagicBoltEntity.Variant.ARCANE).homing().withAoe(3.0F);
        orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.9F, 0.3F);
        player.level().addFreshEntity(orb);
        playSound(player, SoundEvents.WITHER_SHOOT, 1.6F);
        return true;
    }

    /** Lança Arcana: an instant piercing beam that damages EVERYTHING in its path. */
    private static boolean castArcaneLance(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 9.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        ServerLevel level = level(player);
        Vec3 start = player.getEyePosition();
        Vec3 end = rayTarget(player, 14.0D);

        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(1.0D), hostileTo(player))) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (distanceToSegment(center, start, end) <= 1.2D) {
                target.hurt(player.damageSources().indirectMagic(player, player), damage);
                hits++;
            }
        }
        SkillFx.beam(level, start.add(player.getLookAngle().scale(0.8D)), end,
                SkillFx.ARCANE_A, SkillFx.ARCANE_B, 1.5F);
        level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 8, 0.2D, 0.2D, 0.2D, 0.1D);
        playSound(player, SoundEvents.SHULKER_SHOOT, 1.5F);
        return hits >= 0;
    }

    /** Explosão Etérea: an energy burst that hurls enemies away. */
    private static boolean castEtherealExplosion(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 8.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        ServerLevel level = level(player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(5.0D), hostileTo(player))) {
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            Vec3 push = target.position().subtract(player.position()).normalize()
                    .scale(1.6D).add(0.0D, 0.5D, 0.0D);
            target.setDeltaMovement(push);
            target.hurtMarked = true;
        }
        SkillFx.shockwave(level, player.position(), 5.0D, SkillFx.ARCANE_A, SkillFx.ARCANE_B);
        SkillFx.sphereBurst(level, player.position().add(0.0D, 1.0D, 0.0D),
                SkillFx.ARCANE_A, SkillFx.ARCANE_B, 40, 0.55D);
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY(1.0D), player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        playSound(player, SoundEvents.GENERIC_EXPLODE, 1.4F);
        return true;
    }

    /** Chuva de Meteoros: purple meteors rain over the aimed area. */
    private static boolean castMeteorShower(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 7.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        Vec3 target = rayTarget(player, 20.0D);
        ServerLevel level = level(player);
        for (int i = 0; i < 8; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5D) * 10.0D;
            double oz = (player.getRandom().nextDouble() - 0.5D) * 10.0D;
            MagicBoltEntity meteor = new MagicBoltEntity(level, player, damage,
                    MagicBoltEntity.Variant.SUPREME).withAoe(2.5F);
            meteor.setPos(target.x + ox, target.y + 14.0D + player.getRandom().nextDouble() * 4.0D,
                    target.z + oz);
            meteor.setDeltaMovement((player.getRandom().nextDouble() - 0.5D) * 0.15D, -1.3D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.15D);
            level.addFreshEntity(meteor);
        }
        playSound(player, SoundEvents.DRAGON_FIREBALL_EXPLODE, 0.7F);
        return true;
    }

    /** Barreira Arcana: blocks and absorbs incoming damage for a while. */
    private static boolean castArcaneBarrier(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1));
        SkillFx.helix(level(player), player, SkillFx.ARCANE_A, SkillFx.ARCANE_B);
        SkillFx.ring(level(player), player.position().add(0.0D, 0.1D, 0.0D), 1.6D,
                SkillFx.ARCANE_A, SkillFx.ARCANE_B, 1.4F, 0.05D);
        playSound(player, SoundEvents.BEACON_ACTIVATE, 0.8F);
        return true;
    }

    /** Esferas Flutuantes tick: three orbs orbit the caster and zap enemies. */
    public static void tickFloatingSpheres(ServerPlayer player, PlayerRpg rpg, long gameTime) {
        ServerLevel level = level(player);
        if (gameTime % 2 == 0) {
            for (int i = 0; i < 3; i++) {
                Vec3 orb = spherePosition(player, gameTime, i);
                level.sendParticles(new DustParticleOptions(SkillFx.ARCANE_A, 1.5F),
                        orb.x, orb.y, orb.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
                level.sendParticles(new DustParticleOptions(SkillFx.ARCANE_B, 0.9F),
                        orb.x, orb.y, orb.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
            }
        }
        if (gameTime % 25 != 0) {
            return;
        }
        float damage = 3.5F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(6.0D), hostileTo(player));
        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(player.getRandom().nextInt(targets.size()));
            Vec3 orb = spherePosition(player, gameTime, player.getRandom().nextInt(3));
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            SkillFx.jaggedLine(level, orb,
                    target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                    SkillFx.ARCANE_A, SkillFx.ARCANE_B);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.7F, 1.6F);
        }
    }

    private static Vec3 spherePosition(ServerPlayer player, long gameTime, int index) {
        double theta = gameTime * 0.12D + index * (Math.PI * 2.0D / 3.0D);
        return player.position().add(Math.cos(theta) * 1.5D,
                1.4D + Math.sin(gameTime * 0.08D + index) * 0.25D, Math.sin(theta) * 1.5D);
    }

    /** Raio do Caos: a chaotic bolt that arcs between up to five enemies. */
    private static boolean castChaosRay(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 10.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        ServerLevel level = level(player);
        Vec3 start = player.getEyePosition();
        Vec3 reach = rayTarget(player, 16.0D);

        LivingEntity first = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, reach).inflate(1.5D), hostileTo(player)).stream()
                .filter(e -> distanceToSegment(e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D),
                        start, reach) <= 1.5D)
                .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (first == null) {
            SkillFx.jaggedLine(level, start.add(player.getLookAngle().scale(0.8D)), reach,
                    SkillFx.LIGHTNING_A, SkillFx.ARCANE_A);
            playSound(player, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.8F);
            return true;
        }

        java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
        LivingEntity current = first;
        Vec3 from = start.add(player.getLookAngle().scale(0.8D));
        float chainDamage = damage;
        for (int jump = 0; jump < 5 && current != null; jump++) {
            Vec3 hitPoint = current.position().add(0.0D, current.getBbHeight() * 0.5D, 0.0D);
            current.hurt(player.damageSources().indirectMagic(player, player), chainDamage);
            SkillFx.jaggedLine(level, from, hitPoint, SkillFx.LIGHTNING_A, SkillFx.ARCANE_A);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    hitPoint.x, hitPoint.y, hitPoint.z, 10, 0.3D, 0.3D, 0.3D, 0.2D);
            struck.add(current);
            from = hitPoint;
            chainDamage *= 0.7F;
            LivingEntity previous = current;
            current = level.getEntitiesOfClass(LivingEntity.class,
                            previous.getBoundingBox().inflate(5.0D),
                            e -> hostileTo(player).test(e) && !struck.contains(e)).stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(previous)))
                    .orElse(null);
        }
        playSound(player, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.6F);
        return true;
    }

    /** Nova Gravidade: drags enemies to the aimed point and slows them. */
    private static boolean castGravityNova(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 4.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        Vec3 center = rayTarget(player, 16.0D);
        ServerLevel level = level(player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(BlockPos.containing(center)).inflate(6.0D), hostileTo(player))) {
            Vec3 pull = center.subtract(target.position()).scale(0.30D).add(0.0D, 0.15D, 0.0D);
            target.setDeltaMovement(pull);
            target.hurtMarked = true;
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            slowTarget(target, 120, 2);
        }
        SkillFx.swirl(level, center, 5.0D, SkillFx.ARCANE_A, SkillFx.ARCANE_B);
        level.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 0.5D, center.z,
                60, 2.0D, 0.5D, 2.0D, 0.5D);
        playSound(player, SoundEvents.ENDERMAN_TELEPORT, 0.6F);
        return true;
    }

    /** Fissura Dimensional: a rift zone that burns enemies inside for 8s. */
    private static boolean castDimensionalRift(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 3.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        Vec3 center = rayTarget(player, 16.0D);
        RIFTS.add(new Rift(level(player), player.getUUID(), center, gameTime + 160, damage));
        playSound(player, SoundEvents.END_PORTAL_SPAWN, 1.6F);
        return true;
    }

    /** Teleporte Ofensivo: blink to the aimed enemy and blast the arrival. */
    private static boolean castOffensiveTeleport(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 9.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        ServerLevel level = level(player);
        Vec3 start = player.getEyePosition();
        Vec3 reach = rayTarget(player, 18.0D);
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, reach).inflate(1.5D), hostileTo(player)).stream()
                .filter(e -> distanceToSegment(e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D),
                        start, reach) <= 1.5D)
                .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
        if (target == null) {
            return false; // No target in sight: free re-cast.
        }
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY(1.0D), player.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.2D);
        Vec3 arrival = target.position().subtract(
                target.position().subtract(player.position()).normalize().scale(1.5D));
        player.teleportTo(arrival.x, target.getY(), arrival.z);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(3.0D), hostileTo(player))) {
            nearby.hurt(player.damageSources().indirectMagic(player, player), damage);
        }
        SkillFx.shockwave(level, player.position(), 3.0D, SkillFx.ARCANE_A, SkillFx.ARCANE_B);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY(1.0D), player.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.2D);
        playSound(player, SoundEvents.ENDERMAN_TELEPORT, 1.2F);
        return true;
    }

    /** Lâminas Arcana: a wave of energy blades sweeping forward in a line. */
    private static boolean castArcaneBlades(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 8.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        ServerLevel level = level(player);
        Vec3 look = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z).normalize();
        Vec3 side = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();

        java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
        for (int step = 1; step <= 10; step++) {
            Vec3 point = player.position().add(look.scale(step));
            for (int lane = -1; lane <= 1; lane++) {
                Vec3 blade = point.add(side.scale(lane * 1.2D));
                if (step % 2 == lane % 2 + 1 || step % 3 == 0) {
                    SkillFx.blade(level, blade.add(0.0D, 0.3D, 0.0D),
                            SkillFx.ARCANE_A, SkillFx.ARCANE_B);
                }
            }
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(BlockPos.containing(point)).inflate(1.6D),
                    e -> hostileTo(player).test(e) && !struck.contains(e))) {
                target.hurt(player.damageSources().indirectMagic(player, player), damage);
                struck.add(target);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY(0.6D), target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        playSound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F);
        return true;
    }

    /** Campo de Anulação: strips enemy buffs and weakens them inside the circle. */
    private static boolean castNullificationField(ServerPlayer player) {
        ServerLevel level = level(player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(6.0D), hostileTo(player))) {
            java.util.List<net.minecraft.world.effect.MobEffect> beneficial =
                    target.getActiveEffects().stream()
                            .filter(instance -> instance.getEffect().isBeneficial())
                            .map(MobEffectInstance::getEffect)
                            .collect(Collectors.toList());
            beneficial.forEach(target::removeEffect);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
        }
        // Rune circle on the ground.
        SkillFx.ring(level, player.position().add(0.0D, 0.1D, 0.0D), 6.0D,
                SkillFx.ARCANE_B, SkillFx.ARCANE_A, 1.2F, 0.0D);
        SkillFx.ring(level, player.position().add(0.0D, 0.12D, 0.0D), 4.0D,
                SkillFx.ARCANE_A, SkillFx.ARCANE_B, 1.0F, 0.0D);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY(1.0D), player.getZ(), 80, 3.0D, 0.3D, 3.0D, 0.5D);
        playSound(player, SoundEvents.BEACON_DEACTIVATE, 1.4F);
        return true;
    }

    /** Corrente Arcana: energy chains that root enemies in place. */
    private static boolean castArcaneChain(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 4.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        ServerLevel level = level(player);
        Vec3 hand = player.position().add(0.0D, 1.2D, 0.0D);
        int chained = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(8.0D), hostileTo(player))) {
            if (chained >= 4) {
                break;
            }
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
            slowTarget(target, 140, 5);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            SkillFx.jaggedLine(level, hand,
                    target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                    SkillFx.ARCANE_B, SkillFx.ARCANE_A);
            chained++;
        }
        playSound(player, SoundEvents.CHAIN_PLACE, 0.6F);
        return chained > 0;
    }

    /** Presença do Vazio tick: the dark aura burns everything near the caster. */
    public static void tickVoidPresence(ServerPlayer player, PlayerRpg rpg, long gameTime) {
        ServerLevel level = level(player);
        if (gameTime % 4 == 0) {
            double theta = gameTime * 0.3D;
            level.sendParticles(new DustParticleOptions(SkillFx.ARCANE_B, 1.3F),
                    player.getX() + Math.cos(theta) * 1.8D, player.getY(0.3D),
                    player.getZ() + Math.sin(theta) * 1.8D, 1, 0.05D, 0.3D, 0.05D, 0.0D);
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY(0.2D), player.getZ(), 2, 0.8D, 0.1D, 0.8D, 0.01D);
        }
        if (gameTime % 20 != 0) {
            return;
        }
        float damage = 2.5F * RpgStats.magicDamageMultiplier(rpg);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(4.0D), hostileTo(player))) {
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
        }
    }

    /** Cometa Arcano: a heavy comet that detonates on impact. */
    private static boolean castArcaneComet(ServerPlayer player, PlayerRpg rpg) {
        long gameTime = player.level().getGameTime();
        float damage = 14.0F * RpgStats.magicDamageMultiplier(rpg) * voidBonus(rpg, gameTime);
        MagicBoltEntity comet = new MagicBoltEntity(player.level(), player, damage,
                MagicBoltEntity.Variant.SUPREME).withAoe(4.0F);
        comet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.2F);
        player.level().addFreshEntity(comet);
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(1.0D));
        level(player).sendParticles(new DustParticleOptions(SkillFx.ARCANE_A, 1.6F),
                muzzle.x, muzzle.y, muzzle.z, 12, 0.2D, 0.2D, 0.2D, 0.1D);
        playSound(player, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.0F);
        return true;
    }

    // ------------------------------------------------------------------
    // Dimensional rifts (ticked zones)
    // ------------------------------------------------------------------

    private record Rift(ServerLevel level, java.util.UUID owner, Vec3 center, long until, float damage) {
    }

    private static final List<Rift> RIFTS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Called every server level tick from RpgEvents. */
    public static void tickRifts(ServerLevel level) {
        long gameTime = level.getGameTime();
        for (Rift rift : RIFTS) {
            if (rift.level() != level) {
                continue;
            }
            if (gameTime >= rift.until()) {
                RIFTS.remove(rift);
                continue;
            }
            if (gameTime % 5 == 0) {
                SkillFx.swirl(level, rift.center(), 3.0D, SkillFx.ARCANE_B, SkillFx.ARCANE_A);
                level.sendParticles(ParticleTypes.PORTAL,
                        rift.center().x, rift.center().y + 0.4D, rift.center().z,
                        10, 1.2D, 0.3D, 1.2D, 0.1D);
            }
            if (gameTime % 10 == 0) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(rift.owner());
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.containing(rift.center())).inflate(3.0D),
                        e -> e.isAlive() && (owner == null || e != owner))) {
                    target.hurt(owner != null
                            ? level.damageSources().indirectMagic(owner, owner)
                            : level.damageSources().magic(), rift.damage());
                }
            }
        }
    }

    private static void slowTarget(LivingEntity target, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier));
    }

    /** Distance from a point to the segment [a, b]. */
    private static double distanceToSegment(Vec3 point, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Mth.clamp(point.subtract(a).dot(ab) / ab.lengthSqr(), 0.0D, 1.0D);
        return point.distanceTo(a.add(ab.scale(t)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static ServerLevel level(ServerPlayer player) {
        return (ServerLevel) player.level();
    }

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
