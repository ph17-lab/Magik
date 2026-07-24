package com.magik.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects nearby living entities for the map views: each blip carries its
 * position relative to the map center (0..1 on both axes) and a color telling
 * hostiles, animals, villagers and other players apart.
 */
public final class MobRadar {

    public static final int COLOR_HOSTILE = 0xFFE04A4A;
    public static final int COLOR_ANIMAL = 0xFFEDEDED;
    public static final int COLOR_VILLAGER = 0xFFF2C14A;
    public static final int COLOR_PLAYER = 0xFF5BC0F0;

    private static final int MAX_BLIPS = 120;

    /** A single radar dot: normalized [0..1] map coordinates plus its color. */
    public record Blip(double u, double v, int color) {
    }

    private MobRadar() {
    }

    public static List<Blip> scan(double centerX, double centerZ, int radius) {
        List<Blip> blips = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return blips;
        }
        double span = radius * 2.0D;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (blips.size() >= MAX_BLIPS) {
                break;
            }
            if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) {
                continue;
            }
            if (living.isInvisibleTo(mc.player)) {
                continue;
            }
            double u = (entity.getX() - centerX) / span + 0.5D;
            double v = (entity.getZ() - centerZ) / span + 0.5D;
            if (u < 0.0D || u > 1.0D || v < 0.0D || v > 1.0D) {
                continue;
            }
            blips.add(new Blip(u, v, colorOf(living)));
        }
        return blips;
    }

    private static int colorOf(LivingEntity entity) {
        if (entity instanceof Player) {
            return COLOR_PLAYER;
        }
        if (entity instanceof Monster) {
            return COLOR_HOSTILE;
        }
        if (entity instanceof AbstractVillager) {
            return COLOR_VILLAGER;
        }
        return COLOR_ANIMAL;
    }
}
