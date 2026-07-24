package com.magik.client.map;

import com.magik.client.ClientRpgData;
import com.magik.network.MagikNetwork;
import com.magik.network.WaypointPacket;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The Explorer fullscreen map: a larger render of the surrounding terrain with
 * the player marker, every waypoint labelled, and a side list to remove them.
 */
public class FullMapScreen extends Screen {

    private static final int TEX = 160;
    private static final int RADIUS = 220;

    private DynamicTexture texture;
    private ResourceLocation textureId;
    private int mapSize;
    private int mapX;
    private int mapY;

    public FullMapScreen() {
        super(Component.translatable("screen.magik.map"));
    }

    @Override
    protected void init() {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        NativeImage image = new NativeImage(TEX, TEX, true);
        MapSampler.sample(image, player.level(), player.getBlockX(), player.getBlockZ(), RADIUS);
        texture = new DynamicTexture(image);
        textureId = minecraft.getTextureManager().register("magik_fullmap", texture);

        mapSize = Math.min(width - 160, height - 60);
        mapX = 20;
        mapY = (height - mapSize) / 2;

        // Waypoint remove buttons on the right.
        int listX = mapX + mapSize + 16;
        int y = 50;
        for (var wp : ClientRpgData.get().getWaypoints()) {
            String name = wp.name();
            addRenderableWidget(Button.builder(Component.literal("✖ " + name), b ->
                            MagikNetwork.CHANNEL.sendToServer(new WaypointPacket(false, name)))
                    .bounds(listX, y, Math.min(140, width - listX - 10), 16)
                    .build());
            y += 18;
            if (y > height - 40) {
                break;
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.magik.add_waypoint_here"), b -> {
                    MagikNetwork.CHANNEL.sendToServer(new WaypointPacket(true, ""));
                    onClose();
                })
                .bounds(listX, height - 30, Math.min(140, width - listX - 10), 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        LocalPlayer player = minecraft.player;
        if (player == null || textureId == null) {
            return;
        }
        graphics.drawCenteredString(font, title, mapX + mapSize / 2, mapY - 14, 0xFFB8F0E4);

        graphics.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, 0xFF5BC0B0);
        graphics.blit(textureId, mapX, mapY, mapSize, mapSize, 0.0F, 0.0F, TEX, TEX, TEX, TEX);

        // Player at center.
        int cx = mapX + mapSize / 2;
        int cy = mapY + mapSize / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(player.getYRot()));
        graphics.fill(-2, -6, 2, 4, 0xFFFFFFFF);
        graphics.fill(-4, 2, 4, 5, 0xFFFFFFFF);
        graphics.pose().popPose();

        // Waypoints.
        for (var wp : ClientRpgData.get().getWaypoints()) {
            double dx = (wp.x() - player.getX()) / (double) (RADIUS * 2) + 0.5;
            double dz = (wp.z() - player.getZ()) / (double) (RADIUS * 2) + 0.5;
            if (dx < 0 || dx > 1 || dz < 0 || dz > 1) {
                continue;
            }
            int px = mapX + (int) (dx * mapSize);
            int py = mapY + (int) (dz * mapSize);
            graphics.fill(px - 3, py - 3, px + 3, py + 3, 0xFF000000);
            graphics.fill(px - 2, py - 2, px + 2, py + 2, 0xFFF2D24A);
            graphics.drawString(font, wp.name(), px + 5, py - 4, 0xFFFFFFFF);
        }

        graphics.drawString(font, player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ(),
                mapX, mapY + mapSize + 6, 0xFFB8F0E4);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (texture != null) {
            minecraft.getTextureManager().release(textureId);
            texture.close();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
