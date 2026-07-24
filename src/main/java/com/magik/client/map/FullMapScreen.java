package com.magik.client.map;

import com.magik.client.ClientRpgData;
import com.magik.item.SpecialCompassItem;
import com.magik.network.LocateBiomePacket;
import com.magik.network.MagikNetwork;
import com.magik.network.WaypointPacket;
import com.magik.skills.SkillTrees;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Explorer's world map: a framed terrain render with a world grid, live
 * mob blips, labelled waypoints and a side panel that either lists the biomes
 * (picking one aims every biome compass at it) or manages waypoints.
 */
public class FullMapScreen extends Screen {

    private static final int TEX = 192;
    private static final int RADIUS = 220;
    private static final int GRID_BLOCKS = 64;

    private static final int ACCENT = 0xFF5BC0B0;
    private static final int PANEL_BG = 0xE8101018;
    private static final int FRAME_DARK = 0xFF0B0B12;

    private enum Panel {BIOMES, WAYPOINTS}

    private DynamicTexture texture;
    private ResourceLocation textureId;
    private int mapSize;
    private int mapX;
    private int mapY;
    private int panelX;
    private int panelWidth;
    private Panel panel = Panel.BIOMES;
    private int scroll;
    private int selectedBiome = -1;

    public FullMapScreen() {
        super(Component.translatable("screen.magik.map"));
    }

    @Override
    protected void init() {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (texture == null) {
            NativeImage image = new NativeImage(TEX, TEX, true);
            MapSampler.sample(image, player.level(), player.getBlockX(), player.getBlockZ(), RADIUS);
            texture = new DynamicTexture(image);
            textureId = minecraft.getTextureManager().register("magik_fullmap", texture);
        }

        panelWidth = 132;
        panelX = width - panelWidth - 10;
        mapSize = Math.min(panelX - 30, height - 64);
        mapX = Math.max(12, (panelX - mapSize) / 2);
        mapY = (height - mapSize) / 2 + 6;

        rebuildPanel();
    }

    private void rebuildPanel() {
        clearWidgets();

        int half = (panelWidth - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.magik.panel_biomes"), b -> {
                    panel = Panel.BIOMES;
                    scroll = 0;
                    rebuildPanel();
                }).bounds(panelX, 26, half, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.magik.panel_waypoints"), b -> {
                    panel = Panel.WAYPOINTS;
                    scroll = 0;
                    rebuildPanel();
                }).bounds(panelX + half + 4, 26, half, 16).build());

        int y = 48;
        int bottom = height - 34;
        if (panel == Panel.BIOMES) {
            boolean unlocked = ClientRpgData.get().hasSkill(SkillTrees.EX_BIOME_COMPASS);
            for (int i = scroll; i < SpecialCompassItem.BIOMES.length && y < bottom; i++) {
                int index = i;
                Button button = Button.builder(SpecialCompassItem.biomeName(i), b -> {
                            selectedBiome = index;
                            MagikNetwork.CHANNEL.sendToServer(new LocateBiomePacket(index));
                        })
                        .bounds(panelX, y, panelWidth, 16)
                        .build();
                button.active = unlocked;
                addRenderableWidget(button);
                y += 18;
            }
        } else {
            var waypoints = ClientRpgData.get().getWaypoints();
            for (int i = scroll; i < waypoints.size() && y < bottom; i++) {
                String name = waypoints.get(i).name();
                addRenderableWidget(Button.builder(Component.literal("✖ " + name), b -> {
                            MagikNetwork.CHANNEL.sendToServer(new WaypointPacket(false, name));
                            rebuildPanel();
                        })
                        .bounds(panelX, y, panelWidth, 16)
                        .build());
                y += 18;
            }
            addRenderableWidget(Button.builder(Component.translatable("screen.magik.add_waypoint_here"), b -> {
                        MagikNetwork.CHANNEL.sendToServer(new WaypointPacket(true, ""));
                        onClose();
                    })
                    .bounds(panelX, height - 30, panelWidth, 18)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = minecraft.player;
        if (player == null || textureId == null) {
            return;
        }
        graphics.fillGradient(0, 0, width, height, 0xF00A0A12, 0xF605050A);

        renderMapFrame(graphics);
        graphics.enableScissor(mapX, mapY, mapX + mapSize, mapY + mapSize);
        graphics.blit(textureId, mapX, mapY, mapSize, mapSize, 0.0F, 0.0F, TEX, TEX, TEX, TEX);
        renderGrid(graphics, player);
        renderMobs(graphics, player);
        renderWaypoints(graphics, player);
        renderPlayer(graphics, player);
        graphics.disableScissor();

        renderCardinals(graphics);
        renderInfoBar(graphics, player);
        renderPanelBackdrop(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMapFrame(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, mapX + mapSize / 2, mapY - 16, ACCENT);
        graphics.fill(mapX - 3, mapY - 3, mapX + mapSize + 3, mapY + mapSize + 3, FRAME_DARK);
        graphics.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, ACCENT);
        graphics.fill(mapX - 1, mapY - 1, mapX + mapSize + 1, mapY + mapSize + 1, FRAME_DARK);

        // Corner accents.
        int c = 8;
        for (int[] corner : new int[][]{{mapX, mapY, 1, 1}, {mapX + mapSize, mapY, -1, 1},
                {mapX, mapY + mapSize, 1, -1}, {mapX + mapSize, mapY + mapSize, -1, -1}}) {
            int cx = corner[0];
            int cy = corner[1];
            int sx = corner[2];
            int sy = corner[3];
            graphics.fill(Math.min(cx, cx + sx * c), cy - (sy > 0 ? 3 : 0),
                    Math.max(cx, cx + sx * c), cy + (sy > 0 ? 0 : 3), ACCENT);
            graphics.fill(cx - (sx > 0 ? 3 : 0), Math.min(cy, cy + sy * c),
                    cx + (sx > 0 ? 0 : 3), Math.max(cy, cy + sy * c), ACCENT);
        }
    }

    /** Faint world-aligned grid every {@value #GRID_BLOCKS} blocks. */
    private void renderGrid(GuiGraphics graphics, LocalPlayer player) {
        double span = RADIUS * 2.0D;
        double pxPerBlock = mapSize / span;
        int startX = Mth.floor((player.getX() - RADIUS) / GRID_BLOCKS) * GRID_BLOCKS;
        for (int wx = startX; wx <= player.getX() + RADIUS; wx += GRID_BLOCKS) {
            int px = mapX + (int) ((wx - (player.getX() - RADIUS)) * pxPerBlock);
            graphics.fill(px, mapY, px + 1, mapY + mapSize, 0x18FFFFFF);
        }
        int startZ = Mth.floor((player.getZ() - RADIUS) / GRID_BLOCKS) * GRID_BLOCKS;
        for (int wz = startZ; wz <= player.getZ() + RADIUS; wz += GRID_BLOCKS) {
            int py = mapY + (int) ((wz - (player.getZ() - RADIUS)) * pxPerBlock);
            graphics.fill(mapX, py, mapX + mapSize, py + 1, 0x18FFFFFF);
        }
    }

    private void renderMobs(GuiGraphics graphics, LocalPlayer player) {
        for (MobRadar.Blip blip : MobRadar.scan(player.getX(), player.getZ(), RADIUS)) {
            int px = mapX + (int) (blip.u() * mapSize);
            int py = mapY + (int) (blip.v() * mapSize);
            graphics.fill(px - 3, py - 3, px + 3, py + 3, 0xC0000000);
            graphics.fill(px - 2, py - 2, px + 2, py + 2, blip.color());
        }
    }

    private void renderWaypoints(GuiGraphics graphics, LocalPlayer player) {
        for (var wp : ClientRpgData.get().getWaypoints()) {
            double u = (wp.x() - player.getX()) / (RADIUS * 2.0D) + 0.5D;
            double v = (wp.z() - player.getZ()) / (RADIUS * 2.0D) + 0.5D;
            if (u < 0 || u > 1 || v < 0 || v > 1) {
                continue;
            }
            int px = mapX + (int) (u * mapSize);
            int py = mapY + (int) (v * mapSize);
            // Diamond marker.
            for (int i = 0; i <= 3; i++) {
                graphics.fill(px - i, py - 3 + i, px + i + 1, py - 2 + i, 0xFF000000);
                graphics.fill(px - i, py + 3 - i, px + i + 1, py + 4 - i, 0xFF000000);
            }
            graphics.fill(px - 1, py - 1, px + 2, py + 2, 0xFFF2D24A);
            graphics.drawString(font, wp.name(), px + 6, py - 4, 0xFFFFFFFF);
        }
    }

    private void renderPlayer(GuiGraphics graphics, LocalPlayer player) {
        int cx = mapX + mapSize / 2;
        int cy = mapY + mapSize / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(player.getYRot()));
        graphics.fill(-3, -7, 3, 5, 0xFF000000);
        graphics.fill(-2, -6, 2, 4, 0xFFFFFFFF);
        graphics.fill(-4, 3, 4, 6, 0xFF000000);
        graphics.fill(-3, 3, 3, 5, 0xFFFFFFFF);
        graphics.pose().popPose();
    }

    private void renderCardinals(GuiGraphics graphics) {
        graphics.drawCenteredString(font, "N", mapX + mapSize / 2, mapY + 3, 0xFFFF6060);
        graphics.drawCenteredString(font, "S", mapX + mapSize / 2, mapY + mapSize - 11, 0xFFCFCFDA);
        graphics.drawString(font, "W", mapX + 4, mapY + mapSize / 2 - 4, 0xFFCFCFDA);
        graphics.drawString(font, "E", mapX + mapSize - 10, mapY + mapSize / 2 - 4, 0xFFCFCFDA);
    }

    /** Coordinates on the left, marker legend on the right. */
    private void renderInfoBar(GuiGraphics graphics, LocalPlayer player) {
        int y = mapY + mapSize + 7;
        graphics.drawString(font, player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ(),
                mapX, y, 0xFFB8F0E4);

        int x = mapX + mapSize;
        x = legend(graphics, x, y, MobRadar.COLOR_PLAYER, "screen.magik.legend_players", true);
        x = legend(graphics, x, y, MobRadar.COLOR_VILLAGER, "screen.magik.legend_villagers", true);
        x = legend(graphics, x, y, MobRadar.COLOR_ANIMAL, "screen.magik.legend_animals", true);
        legend(graphics, x, y, MobRadar.COLOR_HOSTILE, "screen.magik.legend_hostiles", true);
    }

    /** Draws one right-aligned legend entry; returns the next x to the left. */
    private int legend(GuiGraphics graphics, int right, int y, int color, String key, boolean rightAligned) {
        Component label = Component.translatable(key);
        int textWidth = font.width(label);
        int x = rightAligned ? right - textWidth : right;
        graphics.drawString(font, label, x, y, 0xFF9A9AA5);
        graphics.fill(x - 8, y + 1, x - 3, y + 6, 0xFF000000);
        graphics.fill(x - 7, y + 2, x - 4, y + 5, color);
        return x - 14;
    }

    private void renderPanelBackdrop(GuiGraphics graphics) {
        graphics.fill(panelX - 5, 20, panelX + panelWidth + 5, height - 10, PANEL_BG);
        graphics.fill(panelX - 5, 20, panelX + panelWidth + 5, 21, ACCENT);
        if (panel == Panel.BIOMES) {
            boolean unlocked = ClientRpgData.get().hasSkill(SkillTrees.EX_BIOME_COMPASS);
            Component hint = unlocked
                    ? (selectedBiome >= 0
                        ? Component.translatable("message.magik.compass_target",
                            SpecialCompassItem.biomeName(selectedBiome))
                        : Component.translatable("screen.magik.pick_biome"))
                    : Component.translatable("message.magik.compass_locked").withStyle(ChatFormatting.RED);
            graphics.drawCenteredString(font, hint, panelX + panelWidth / 2, height - 24, 0xFF9AE8DA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= panelX - 5) {
            int max = panel == Panel.BIOMES
                    ? SpecialCompassItem.BIOMES.length : ClientRpgData.get().getWaypoints().size();
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, Math.max(0, max - 1));
            rebuildPanel();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (texture != null) {
            minecraft.getTextureManager().release(textureId);
            texture.close();
            texture = null;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
