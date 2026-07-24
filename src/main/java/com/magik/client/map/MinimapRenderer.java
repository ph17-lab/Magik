package com.magik.client.map;

import com.magik.client.ClientRpgData;
import com.magik.skills.SkillTrees;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The mod's own minimap: it samples the surrounding terrain's map colors into
 * a dynamic texture (with height shading) and draws it in the top-right corner.
 * Features unlock progressively through the Explorer tree — the map itself,
 * coordinates, a compass rose, zoom and waypoint markers.
 */
public final class MinimapRenderer implements IGuiOverlay {

    public static final MinimapRenderer INSTANCE = new MinimapRenderer();

    private static final int SIZE = 96;          // texture resolution
    private static final int SCREEN = 100;       // on-screen size (px)
    private static final int[] ZOOMS = {32, 48, 72};

    private final DynamicTexture texture = new DynamicTexture(new NativeImage(SIZE, SIZE, true));
    private final ResourceLocation textureId;
    private int zoomIndex = 1;
    private long lastUpdate = -1000;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    private MinimapRenderer() {
        this.textureId = Minecraft.getInstance().getTextureManager()
                .register("magik_minimap", texture);
    }

    public void cycleZoom() {
        zoomIndex = (zoomIndex + 1) % ZOOMS.length;
        lastUpdate = -1000; // force refresh
    }

    public int radius() {
        return ZOOMS[zoomIndex];
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.options.renderDebug
                || !ClientRpgData.get().hasSkill(SkillTrees.EX_MINIMAP)) {
            return;
        }

        long time = System.currentTimeMillis();
        int cx = player.getBlockX();
        int cz = player.getBlockZ();
        if (time - lastUpdate > 500 || cx != lastCenterX || cz != lastCenterZ) {
            sampleTerrain(player.level(), cx, cz);
            lastUpdate = time;
            lastCenterX = cx;
            lastCenterZ = cz;
        }

        int x = screenWidth - SCREEN - 8;
        int y = 8;
        // Frame.
        graphics.fill(x - 2, y - 2, x + SCREEN + 2, y + SCREEN + 2, 0xFF10101A);
        graphics.fill(x - 1, y - 1, x + SCREEN + 1, y + SCREEN + 1, 0xFF5BC0B0);
        graphics.enableScissor(x, y, x + SCREEN, y + SCREEN);
        graphics.blit(textureId, x, y, SCREEN, SCREEN, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);

        renderWaypoints(graphics, player, x, y);
        renderPlayerArrow(graphics, player, x + SCREEN / 2, y + SCREEN / 2);
        graphics.disableScissor();

        renderCardinals(graphics, mc, player, x, y);
        renderCoords(graphics, mc, player, x, y);
    }

    private void sampleTerrain(Level level, int centerX, int centerZ) {
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }
        MapSampler.sample(image, level, centerX, centerZ, radius());
        texture.upload();
    }

    private void renderPlayerArrow(GuiGraphics graphics, LocalPlayer player, int cx, int cy) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(player.getYRot()));
        graphics.fill(-1, -4, 1, 3, 0xFFFFFFFF);
        graphics.fill(-3, 1, 3, 3, 0xFFFFFFFF);
        graphics.fill(-2, -5, 2, -3, 0xFFE33B3B);
        graphics.pose().popPose();
    }

    private void renderWaypoints(GuiGraphics graphics, LocalPlayer player, int mapX, int mapY) {
        if (!ClientRpgData.get().hasSkill(SkillTrees.EX_WAYPOINTS)) {
            return;
        }
        int radius = radius();
        for (var wp : ClientRpgData.get().getWaypoints()) {
            double dx = (wp.x() - player.getX()) / (double) (radius * 2) + 0.5;
            double dz = (wp.z() - player.getZ()) / (double) (radius * 2) + 0.5;
            int px = mapX + (int) (dx * SCREEN);
            int py = mapY + (int) (dz * SCREEN);
            px = Math.max(mapX + 2, Math.min(mapX + SCREEN - 3, px));
            py = Math.max(mapY + 2, Math.min(mapY + SCREEN - 3, py));
            graphics.fill(px - 2, py - 2, px + 2, py + 2, 0xFF000000);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, 0xFFF2D24A);
        }
    }

    private void renderCardinals(GuiGraphics graphics, Minecraft mc, LocalPlayer player, int x, int y) {
        if (!ClientRpgData.get().hasSkill(SkillTrees.EX_DIRECTION)) {
            return;
        }
        graphics.drawCenteredString(mc.font, "N", x + SCREEN / 2, y + 1, 0xFFFF6060);
        graphics.drawCenteredString(mc.font, "S", x + SCREEN / 2, y + SCREEN - 9, 0xFFFFFFFF);
        graphics.drawString(mc.font, "W", x + 2, y + SCREEN / 2 - 4, 0xFFFFFFFF);
        graphics.drawString(mc.font, "E", x + SCREEN - 8, y + SCREEN / 2 - 4, 0xFFFFFFFF);
    }

    private void renderCoords(GuiGraphics graphics, Minecraft mc, LocalPlayer player, int x, int y) {
        if (!ClientRpgData.get().hasSkill(SkillTrees.EX_COORDS)) {
            return;
        }
        String coords = player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ();
        graphics.drawCenteredString(mc.font, coords, x + SCREEN / 2, y + SCREEN + 4, 0xFFB8F0E4);
    }
}
