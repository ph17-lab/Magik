package com.magik.skills;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
}
