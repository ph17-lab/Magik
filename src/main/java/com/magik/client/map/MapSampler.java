package com.magik.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Renders the surrounding terrain's map colors (with height shading) into a NativeImage. */
public final class MapSampler {

    private MapSampler() {
    }

    public static void sample(NativeImage image, Level level, int centerX, int centerZ, int radius) {
        int size = image.getWidth();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                int wx = centerX + (int) ((px / (double) size - 0.5) * radius * 2);
                int wz = centerZ + (int) ((py / (double) size - 0.5) * radius * 2);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
                pos.set(wx, top - 1, wz);
                BlockState state = level.getBlockState(pos);
                MapColor mapColor = state.getMapColor(level, pos);
                int rgb = mapColor == MapColor.NONE ? 0x2A3B4A : mapColor.col;
                int north = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz - 1);
                double shade = top > north ? 1.12 : top < north ? 0.82 : 1.0;
                image.setPixelRGBA(px, py, packAbgr(rgb, shade));
            }
        }
    }

    private static int packAbgr(int rgb, double shade) {
        int r = clamp((int) (((rgb >> 16) & 0xFF) * shade));
        int g = clamp((int) (((rgb >> 8) & 0xFF) * shade));
        int b = clamp((int) ((rgb & 0xFF) * shade));
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
