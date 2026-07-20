package com.magik.combat;

import com.magik.MagikMod;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Archer tree integration for arrows:
 * <ul>
 *   <li>On spawn: Long Range velocity boost and tagging with the player's
 *       armed special effect (explosive / frost / electric).</li>
 *   <li>On impact: executes the tagged special effect.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class ArrowEvents {

    private static final String EFFECT_TAG = "magik:arrow_effect";

    private ArrowEvents() {
    }

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer shooter)) {
            return;
        }
        PlayerRpg rpg = PlayerRpgProvider.get(shooter).orElse(null);
        if (rpg == null) {
            return;
        }

        // Long Range / Precision: faster arrows fly farther and straighter.
        float multiplier = RpgStats.arrowVelocityMultiplier(rpg);
        if (shooter.getMainHandItem().getItem() instanceof com.magik.item.RpgBowItem bow) {
            multiplier *= bow.getVelocityMultiplier();
        } else if (shooter.getOffhandItem().getItem() instanceof com.magik.item.RpgBowItem bow) {
            multiplier *= bow.getVelocityMultiplier();
        }
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(multiplier));

        // Special arrow buffs armed by active skills: tag this arrow, consume the buff.
        long gameTime = shooter.level().getGameTime();
        PlayerRpg.ArrowEffect effect = rpg.getNextArrowEffect(gameTime);
        if (effect != PlayerRpg.ArrowEffect.NONE) {
            arrow.getPersistentData().putString(EFFECT_TAG, effect.name());
            rpg.clearNextArrowEffect();
            arrow.setGlowingTag(true);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || arrow.level().isClientSide
                || !arrow.getPersistentData().contains(EFFECT_TAG)) {
            return;
        }
        String name = arrow.getPersistentData().getString(EFFECT_TAG);
        arrow.getPersistentData().remove(EFFECT_TAG);

        PlayerRpg.ArrowEffect effect;
        try {
            effect = PlayerRpg.ArrowEffect.valueOf(name);
        } catch (IllegalArgumentException e) {
            return;
        }
        ServerLevel level = (ServerLevel) arrow.level();
        Vec3 pos = event.getRayTraceResult().getLocation();
        float magic = arrow.getOwner() instanceof ServerPlayer shooter
                ? PlayerRpgProvider.get(shooter).map(RpgStats::rangedDamageMultiplier).orElse(1.0F)
                : 1.0F;

        switch (effect) {
            case EXPLOSIVE -> {
                // Block-friendly explosion: pure area damage + knockback.
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(pos, pos).inflate(3.0D), e -> e != arrow.getOwner() && e.isAlive())) {
                    target.hurt(level.damageSources().explosion(arrow, arrow.getOwner()), 6.0F * magic);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 4, 0.5D, 0.5D, 0.5D, 0.0D);
                level.playSound(null, arrow.blockPosition(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            case FROST -> {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(pos, pos).inflate(2.5D), e -> e != arrow.getOwner() && e.isAlive())) {
                    target.hurt(level.damageSources().freeze(), 3.0F * magic);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 30, 1.0D, 0.6D, 1.0D, 0.05D);
                level.playSound(null, arrow.blockPosition(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.6F);
            }
            case ELECTRIC -> {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(pos, pos).inflate(3.5D), e -> e != arrow.getOwner() && e.isAlive())) {
                    target.hurt(level.damageSources().lightningBolt(), 5.0F * magic);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 40, 1.2D, 0.8D, 1.2D, 0.15D);
                level.playSound(null, arrow.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F, 1.5F);
            }
            default -> {
            }
        }
    }
}
