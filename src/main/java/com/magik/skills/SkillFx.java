package com.magik.skills;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Server-side particle compositions that give every skill a custom, colorful
 * identity instead of the stock vanilla puffs: slash arcs, shockwave rings,
 * rising helixes and charge glows, all built from two-tone colored dust.
 */
public final class SkillFx {

    // Tree palettes (primary, secondary).
    public static final Vector3f SWORD_A = new Vector3f(0.90F, 0.18F, 0.18F);
    public static final Vector3f SWORD_B = new Vector3f(0.92F, 0.92F, 0.96F);
    public static final Vector3f HEAVY_A = new Vector3f(1.00F, 0.62F, 0.12F);
    public static final Vector3f HEAVY_B = new Vector3f(0.45F, 0.42F, 0.40F);
    public static final Vector3f ARCANE_A = new Vector3f(0.78F, 0.38F, 1.00F);
    public static final Vector3f ARCANE_B = new Vector3f(0.44F, 0.14F, 0.86F);
    public static final Vector3f ARCHER_A = new Vector3f(0.36F, 0.90F, 0.38F);
    public static final Vector3f ARCHER_B = new Vector3f(0.80F, 1.00F, 0.55F);
    public static final Vector3f DEFENSE_A = new Vector3f(0.30F, 0.62F, 1.00F);
    public static final Vector3f DEFENSE_B = new Vector3f(0.70F, 0.88F, 1.00F);
    public static final Vector3f GOLD = new Vector3f(1.00F, 0.84F, 0.25F);
    public static final Vector3f HEAL_GREEN = new Vector3f(0.35F, 0.95F, 0.45F);
    public static final Vector3f LIGHTNING_A = new Vector3f(0.35F, 0.62F, 1.00F);
    public static final Vector3f LIGHTNING_B = new Vector3f(0.78F, 0.90F, 1.00F);
    public static final Vector3f GUARD_A = new Vector3f(0.24F, 0.90F, 0.36F);
    public static final Vector3f GUARD_B = new Vector3f(0.62F, 1.00F, 0.62F);

    private SkillFx() {
    }

    /** A horizontal ring of dust bursting outward - shockwaves and novas. */
    public static void ring(ServerLevel level, Vec3 center, double radius,
                            Vector3f colorA, Vector3f colorB, float size, double outSpeed) {
        int points = Math.max(12, (int) (radius * 10.0D));
        for (int i = 0; i < points; i++) {
            double theta = (Math.PI * 2.0D / points) * i;
            double rx = Math.cos(theta);
            double rz = Math.sin(theta);
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? colorA : colorB, size),
                    center.x + rx * radius, center.y, center.z + rz * radius,
                    1, rx * 0.3D, 0.02D, rz * 0.3D, outSpeed);
        }
    }

    /** Two staggered rings for a stronger shockwave read. */
    public static void shockwave(ServerLevel level, Vec3 center, double radius,
                                 Vector3f colorA, Vector3f colorB) {
        ring(level, center.add(0.0D, 0.15D, 0.0D), radius * 0.55D, colorA, colorB, 1.4F, 0.5D);
        ring(level, center.add(0.0D, 0.05D, 0.0D), radius, colorB, colorA, 1.1F, 0.35D);
    }

    /** A crescent slash arc in front of the player, following their facing. */
    public static void slashArc(ServerLevel level, ServerPlayer player, double range,
                                Vector3f colorA, Vector3f colorB) {
        double yaw = Math.toRadians(player.getYRot());
        // Facing angle in the XZ plane; the arc spans ~120 degrees around it.
        double facing = Math.atan2(-Math.sin(yaw), Math.cos(yaw));
        Vec3 eye = player.position().add(0.0D, 1.2D, 0.0D);
        int points = 14;
        for (int i = 0; i < points; i++) {
            double t = (i / (double) (points - 1)) - 0.5D;      // -0.5 .. 0.5
            double theta = facing + t * (Math.PI * 2.0D / 3.0D);
            double lift = 0.35D - Math.abs(t) * 0.7D;           // arc curves upward at the middle
            double r = range * (0.8D + 0.2D * Math.cos(t * Math.PI));
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? colorA : colorB, 1.3F),
                    eye.x + Math.cos(theta) * r, eye.y + lift, eye.z + Math.sin(theta) * r,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** A rising two-strand helix around the player - buffs, heals, charges. */
    public static void helix(ServerLevel level, ServerPlayer player,
                             Vector3f colorA, Vector3f colorB) {
        for (int i = 0; i < 20; i++) {
            double progress = i / 20.0D;
            double theta = progress * Math.PI * 4.0D;
            double radius = 0.8D - progress * 0.35D;
            double y = player.getY() + progress * 2.2D;
            for (int arm = 0; arm < 2; arm++) {
                double armTheta = theta + arm * Math.PI;
                level.sendParticles(new DustParticleOptions(arm == 0 ? colorA : colorB, 1.0F),
                        player.getX() + Math.cos(armTheta) * radius, y,
                        player.getZ() + Math.sin(armTheta) * radius,
                        1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
        }
    }

    /** A tight sparkling glow around the player's weapon hand - charge-ups. */
    public static void charge(ServerLevel level, ServerPlayer player, Vector3f color) {
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.position().add(look.scale(0.6D)).add(0.0D, 1.1D, 0.0D);
        level.sendParticles(new DustParticleOptions(color, 1.2F),
                hand.x, hand.y, hand.z, 14, 0.25D, 0.25D, 0.25D, 0.0D);
    }

    /** A jagged blue bolt striking down from the sky onto the target point. */
    public static void skyBolt(ServerLevel level, Vec3 target) {
        RandomSource random = level.random;
        double ox = 0.0D;
        double oz = 0.0D;
        for (double y = 18.0D; y >= 0.2D; y -= 0.4D) {
            // Jitter narrows toward the ground so the bolt converges on target.
            double sway = y / 18.0D;
            ox = Mth.clamp(ox + (random.nextDouble() - 0.5D) * 0.6D * sway, -1.6D, 1.6D);
            oz = Mth.clamp(oz + (random.nextDouble() - 0.5D) * 0.6D * sway, -1.6D, 1.6D);
            double px = target.x + ox * sway;
            double pz = target.z + oz * sway;
            level.sendParticles(new DustParticleOptions(LIGHTNING_A, 1.9F),
                    px, target.y + y, pz, 1, 0.03D, 0.03D, 0.03D, 0.0D);
            level.sendParticles(new DustParticleOptions(LIGHTNING_B, 1.1F),
                    px, target.y + y, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.FLASH, target.x, target.y + 0.5D, target.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.x, target.y + 0.3D, target.z,
                25, 0.6D, 0.5D, 0.6D, 0.25D);
        ring(level, target.add(0.0D, 0.1D, 0.0D), 1.8D, LIGHTNING_A, LIGHTNING_B, 1.4F, 0.4D);
    }

    /** An expanding sphere of dust flying outward - the arcane nova core. */
    public static void sphereBurst(ServerLevel level, Vec3 center,
                                   Vector3f colorA, Vector3f colorB, int points, double speed) {
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < points; i++) {
            double vy = 1.0D - (i / (double) (points - 1)) * 2.0D;
            double r = Math.sqrt(1.0D - vy * vy);
            double theta = golden * i;
            double vx = Math.cos(theta) * r;
            double vz = Math.sin(theta) * r;
            // count=0 makes the offsets act as a velocity vector.
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? colorA : colorB, 1.3F),
                    center.x, center.y, center.z, 0, vx, vy * 0.6D, vz, speed);
        }
    }

    /** A vertical spectral blade (edge, tip and crossguard) drawn in dust. */
    public static void blade(ServerLevel level, Vec3 base, Vector3f edge, Vector3f glow) {
        for (int i = 0; i <= 5; i++) {
            level.sendParticles(new DustParticleOptions(i >= 4 ? glow : edge, 0.95F),
                    base.x, base.y + i * 0.22D, base.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        level.sendParticles(new DustParticleOptions(glow, 0.8F),
                base.x + 0.14D, base.y + 0.22D, base.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(new DustParticleOptions(glow, 0.8F),
                base.x - 0.14D, base.y + 0.22D, base.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** A diagonal spectral slash cutting through the target. */
    public static void bladeSlash(ServerLevel level, Vec3 from, LivingEntity target,
                                  Vector3f colorA, Vector3f colorB) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 dir = center.subtract(from).normalize();
        Vec3 side = dir.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 start = center.add(side.scale(0.6D)).add(0.0D, 0.9D, 0.0D);
        Vec3 end = center.subtract(side.scale(0.6D)).add(0.0D, -0.5D, 0.0D);
        for (int i = 0; i <= 8; i++) {
            Vec3 point = start.lerp(end, i / 8.0D);
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? colorA : colorB, 1.1F),
                    point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** A jagged energy line between two points (chains, chaos rays). */
    public static void jaggedLine(ServerLevel level, Vec3 from, Vec3 to,
                                  Vector3f colorA, Vector3f colorB) {
        RandomSource random = level.random;
        double length = from.distanceTo(to);
        int segments = Math.max(4, (int) (length * 2.5D));
        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = from.lerp(to, i / (double) segments);
            if (i < segments) {
                point = point.add((random.nextDouble() - 0.5D) * 0.5D,
                        (random.nextDouble() - 0.5D) * 0.5D,
                        (random.nextDouble() - 0.5D) * 0.5D);
            }
            int dots = 2;
            for (int d = 0; d < dots; d++) {
                Vec3 dot = previous.lerp(point, d / (double) dots);
                level.sendParticles(new DustParticleOptions((i + d) % 2 == 0 ? colorA : colorB, 1.2F),
                        dot.x, dot.y, dot.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            previous = point;
        }
    }

    /** A straight beam of dust between two points (lances, blades). */
    public static void beam(ServerLevel level, Vec3 from, Vec3 to,
                            Vector3f colorA, Vector3f colorB, float size) {
        double length = from.distanceTo(to);
        int points = Math.max(6, (int) (length * 3.0D));
        for (int i = 0; i <= points; i++) {
            Vec3 point = from.lerp(to, i / (double) points);
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? colorA : colorB, size),
                    point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    /** An inward swirl on the ground - gravity wells and rifts. */
    public static void swirl(ServerLevel level, Vec3 center, double radius,
                             Vector3f colorA, Vector3f colorB) {
        for (int i = 0; i < 24; i++) {
            double progress = i / 24.0D;
            double theta = progress * Math.PI * 3.0D;
            double r = radius * (1.0D - progress * 0.8D);
            Vec3 pos = center.add(Math.cos(theta) * r, 0.15D + progress * 0.5D, Math.sin(theta) * r);
            Vec3 inward = center.subtract(pos).normalize();
            level.sendParticles(new DustParticleOptions(i % 2 == 0 ? colorA : colorB, 1.2F),
                    pos.x, pos.y, pos.z, 0, inward.x, 0.05D, inward.z, 0.25D);
        }
    }

    /** A green barrier dome projected in front of a blocking player. */
    public static void guardDome(ServerLevel level, ServerPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        double facing = Math.atan2(-Math.sin(yaw), Math.cos(yaw));
        Vec3 chest = player.position().add(0.0D, 1.1D, 0.0D);
        for (int row = 0; row < 3; row++) {
            double lift = (row - 1) * 0.45D;
            double radius = 1.1D * Math.cos(lift * 0.7D);
            for (int i = 0; i < 5; i++) {
                double t = (i / 4.0D) - 0.5D;                 // -0.5 .. 0.5
                double theta = facing + t * (Math.PI * 0.7D); // ~126 degree shield face
                level.sendParticles(new DustParticleOptions((row + i) % 2 == 0 ? GUARD_A : GUARD_B, 1.0F),
                        chest.x + Math.cos(theta) * radius, chest.y + lift,
                        chest.z + Math.sin(theta) * radius,
                        1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
    }
}
