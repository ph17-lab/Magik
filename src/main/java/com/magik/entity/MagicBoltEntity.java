package com.magik.entity;

import com.magik.registry.ModEntities;
import com.magik.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/**
 * The spell projectile used by the Arcane Staff and the Fireball skill.
 * Damage is computed by the caster (already scaled by Intelligence) and the
 * visual variant only affects particles/ignition.
 */
public class MagicBoltEntity extends ThrowableItemProjectile {

    public enum Variant {
        ARCANE(new Vector3f(0.80F, 0.41F, 1.00F), new Vector3f(0.47F, 0.16F, 0.86F)),
        FIRE(new Vector3f(1.00F, 0.65F, 0.20F), new Vector3f(1.00F, 0.24F, 0.04F)),
        FROST(new Vector3f(0.55F, 0.88F, 1.00F), new Vector3f(0.16F, 0.51F, 0.92F)),
        ELECTRIC(new Vector3f(1.00F, 0.94F, 0.43F), new Vector3f(1.00F, 0.75F, 0.12F)),
        HOLY(new Vector3f(1.00F, 0.90F, 0.59F), new Vector3f(0.92F, 0.71F, 0.24F)),
        SUPREME(new Vector3f(1.00F, 0.35F, 0.82F), new Vector3f(0.59F, 0.12F, 0.78F));

        /** Two-tone spell colors used by the dust trail and impact burst. */
        public final Vector3f primary;
        public final Vector3f secondary;

        Variant(Vector3f primary, Vector3f secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

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
            spawnTrail();
        } else if (tickCount > 100) {
            discard(); // Never let stray bolts live forever.
        }
    }

    /**
     * A double-helix of colored dust twisting around the flight path, plus the
     * element's accent particle at the core - a proper spell trail instead of
     * the single vanilla puff.
     */
    private void spawnTrail() {
        Variant variant = getVariant();
        Vec3 motion = getDeltaMovement();
        Vec3 dir = motion.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : motion.normalize();
        Vec3 reference = Math.abs(dir.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 axisA = dir.cross(reference).normalize();
        Vec3 axisB = dir.cross(axisA).normalize();

        float angle = tickCount * 0.9F;
        double radius = 0.28D;
        for (int arm = 0; arm < 2; arm++) {
            double theta = angle + arm * Math.PI;
            Vec3 offset = axisA.scale(Math.cos(theta) * radius).add(axisB.scale(Math.sin(theta) * radius));
            Vector3f color = arm == 0 ? variant.primary : variant.secondary;
            level().addParticle(new DustParticleOptions(color, 0.85F),
                    getX() + offset.x, getY() + 0.1D + offset.y, getZ() + offset.z,
                    0.0D, 0.0D, 0.0D);
        }
        if (tickCount % 2 == 0) {
            level().addParticle(accentParticle(variant), getX(), getY() + 0.1D, getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static ParticleOptions accentParticle(Variant variant) {
        return switch (variant) {
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
            impactBurst(result.getLocation());
            discard();
        }
    }

    /**
     * Element-flavored explosion: an expanding dust ring on the impact plane,
     * a vertical spark fountain and the element's accent particles.
     */
    private void impactBurst(Vec3 pos) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Variant variant = getVariant();

        for (int i = 0; i < 16; i++) {
            double theta = (Math.PI * 2.0D / 16.0D) * i;
            double rx = Math.cos(theta);
            double rz = Math.sin(theta);
            Vector3f color = i % 2 == 0 ? variant.primary : variant.secondary;
            level.sendParticles(new DustParticleOptions(color, 1.1F),
                    pos.x + rx * 0.5D, pos.y + 0.1D, pos.z + rz * 0.5D,
                    1, rx * 0.35D, 0.05D, rz * 0.35D, 0.35D);
        }
        level.sendParticles(accentParticle(variant),
                pos.x, pos.y + 0.15D, pos.z, 12, 0.25D, 0.3D, 0.25D, 0.08D);

        if (variant == Variant.FIRE || variant == Variant.SUPREME) {
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.2D, pos.z, 4, 0.2D, 0.2D, 0.2D, 0.0D);
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.2D, pos.z, 8, 0.3D, 0.3D, 0.3D, 0.02D);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
