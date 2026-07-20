package com.magik.client.gui;

import com.magik.client.ClientRpgData;
import com.magik.network.MagikNetwork;
import com.magik.network.SpendAttributePointPacket;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerTitles;
import com.magik.player.RpgAttribute;
import com.magik.player.RpgStats;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * The character menu: level, title, XP, mana, stamina, equipment preview and
 * the attribute panel with point spending. A tab button opens the skill trees.
 * Drawn with flat pixel-style fills to blend with vanilla's look.
 */
public class CharacterScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 200;

    private static final int COLOR_PANEL = 0xE8101018;
    private static final int COLOR_BORDER = 0xFF3A3A4A;
    private static final int COLOR_ACCENT = 0xFFE3C55A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_MUTED = 0xFF9A9AA5;

    private int left;
    private int top;

    public CharacterScreen() {
        super(Component.translatable("screen.magik.character"));
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;

        // One [+] button per attribute.
        RpgAttribute[] attributes = RpgAttribute.values();
        for (int i = 0; i < attributes.length; i++) {
            RpgAttribute attribute = attributes[i];
            int y = top + 40 + i * 20;
            addRenderableWidget(Button.builder(Component.literal("+"), button ->
                            MagikNetwork.CHANNEL.sendToServer(new SpendAttributePointPacket(attribute)))
                    .bounds(left + PANEL_WIDTH - 26, y - 3, 16, 16)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.magik.skills_tab"),
                        button -> minecraft.setScreen(new SkillTreeScreen()))
                .bounds(left + PANEL_WIDTH - 90, top + PANEL_HEIGHT - 26, 82, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        PlayerRpg rpg = ClientRpgData.get();

        // Panel
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, COLOR_BORDER);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);

        // Header: title + level.
        graphics.drawString(font, title, left + 8, top + 8, COLOR_ACCENT);
        String levelLine = "Lv " + rpg.getLevel() + " • " + PlayerTitles.getTitle(rpg).getString();
        graphics.drawString(font, levelLine, left + 8, top + 20, COLOR_TEXT);

        // Player preview (left column).
        if (minecraft != null && minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    left + 46, top + 130, 40, left + 46 - mouseX, top + 80 - mouseY, minecraft.player);
        }

        // Vitals under the preview.
        int vy = top + 142;
        if (minecraft != null && minecraft.player != null) {
            drawStat(graphics, left + 10, vy, Component.translatable("screen.magik.health").getString(),
                    (int) minecraft.player.getHealth() + "/" + (int) minecraft.player.getMaxHealth(), 0xFFD83B3B);
        }
        drawStat(graphics, left + 10, vy + 12,
                Component.translatable("screen.magik.mana").getString(),
                (int) rpg.getMana() + "/" + (int) RpgStats.maxMana(rpg, minecraft.player), 0xFF6A96F0);
        drawStat(graphics, left + 10, vy + 24,
                Component.translatable("screen.magik.stamina").getString(),
                (int) rpg.getStamina() + "/" + (int) RpgStats.maxStamina(rpg), 0xFF74E083);
        drawStat(graphics, left + 10, vy + 36, "XP",
                rpg.getXp() + "/" + RpgStats.xpForNextLevel(rpg.getLevel()), 0xFFD26AF0);

        // Attribute panel (right columns).
        int ax = left + 110;
        graphics.drawString(font, Component.translatable("screen.magik.attributes"), ax, top + 28, COLOR_MUTED);
        RpgAttribute[] attributes = RpgAttribute.values();
        for (int i = 0; i < attributes.length; i++) {
            RpgAttribute attribute = attributes[i];
            int y = top + 40 + i * 20;
            graphics.drawString(font, attribute.getDisplayName(), ax, y, COLOR_TEXT);
            String value = String.valueOf(rpg.getAttribute(attribute));
            graphics.drawString(font, value, left + PANEL_WIDTH - 44, y, COLOR_ACCENT);
            if (mouseX >= ax && mouseX < left + PANEL_WIDTH - 48 && mouseY >= y - 3 && mouseY < y + 12) {
                graphics.renderTooltip(font, attribute.getDescription(), mouseX, mouseY);
            }
        }

        // Available points.
        graphics.drawString(font,
                Component.translatable("screen.magik.attribute_points", rpg.getAttributePoints()),
                ax, top + PANEL_HEIGHT - 34, COLOR_TEXT);
        graphics.drawString(font,
                Component.translatable("screen.magik.skill_points", rpg.getSkillPoints()),
                ax, top + PANEL_HEIGHT - 22, COLOR_TEXT);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawStat(GuiGraphics graphics, int x, int y, String label, String value, int color) {
        graphics.drawString(font, label, x, y, COLOR_MUTED);
        graphics.drawString(font, value, x + 46, y, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
