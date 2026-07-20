package com.magik.entity;

import com.magik.registry.ModEntities;
import com.magik.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * The spell projectile used by the Arcane Staff and the Fireball skill.
 * Damage is computed by the caster (already scaled by Intelligence) and the
 * visual variant only affects particles/ignition.
 */
public class MagicBoltEntity extends ThrowableItemProjectile {

    public enum Variant {ARCANE, FIRE, FROST, ELECTRIC, HOLY, SUPREME}

    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(MagicBoltEntity.class, EntityDataSerializers.BYTE);

    /** Server-side only; the projectile dies on first hit, so no need to sync. */
    private float damage = 5.0F;

    public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, Level level) {
        super(type, level);
    }

    public MagicBoltEntity(Level level, LivingEntity shooter, float damage, Variant variant) {
        super(ModEntities.MAGIC_BOLT.get(), shooter, level);
        this.damage = damage;
        this.entityData.set(VARIANT, (byte) variant.ordinal());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) Variant.ARCANE.ordinal());
    }

    public Variant getVariant() {
        int ordinal = this.entityData.get(VARIANT);
        return Variant.values()[Math.floorMod(ordinal, Variant.values().length)];
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MANA_CRYSTAL.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(trailParticle(), getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (tickCount > 100) {
            discard(); // Never let stray bolts live forever.
        }
    }

    private ParticleOptions trailParticle() {
        return switch (getVariant()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
            case HOLY -> ParticleTypes.END_ROD;
            case SUPREME -> ParticleTypes.DRAGON_BREATH;
            default -> ParticleTypes.WITCH;
        };
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        target.hurt(damageSources().indirectMagic(this, getOwner()), damage);
        switch (getVariant()) {
            case FIRE -> target.setSecondsOnFire(4);
            case FROST -> slow(target, 100, 2);
            case ELECTRIC -> {
                // Chains a weaker spark to one nearby enemy.
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.getEntitiesOfClass(LivingEntity.class,
                                    target.getBoundingBox().inflate(3.0D),
                                    e -> e != target && e != getOwner() && e.isAlive())
                            .stream().findFirst().ifPresent(chained -> {
                                chained.hurt(damageSources().indirectMagic(this, getOwner()), damage * 0.5F);
                                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                        chained.getX(), chained.getY(0.5D), chained.getZ(),
                                        15, 0.3D, 0.3D, 0.3D, 0.2D);
                            });
                }
            }
            case HOLY -> {
                if (getOwner() instanceof LivingEntity owner && owner.getHealth() < owner.getMaxHealth()) {
                    owner.heal(1.5F);
                }
            }
            case SUPREME -> {
                target.setSecondsOnFire(3);
                slow(target, 80, 1);
            }
            default -> {
            }
        }
    }

    private void slow(Entity target, int ticks, int amplifier) {
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3); // Item break particles.
            discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
