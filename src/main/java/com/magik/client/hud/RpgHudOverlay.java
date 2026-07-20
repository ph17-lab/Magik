package com.magik.client.hud;

import com.magik.client.ClientRpgData;
import com.magik.client.KeyBindings;
import com.magik.client.SkillIcons;
import com.magik.config.MagikClientConfig;
import com.magik.player.PlayerRpg;
import com.magik.player.RpgStats;
import com.magik.skills.Skill;
import com.magik.skills.SkillTrees;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The RPG HUD: vitals panel (health / mana / stamina), level badge with an
 * animated XP bar, and the four skill slots with cooldown sweeps.
 * Drawn with flat pixel-style fills so it blends with vanilla's look.
 * All positions are configurable in {@link MagikClientConfig}.
 */
public class RpgHudOverlay implements IGuiOverlay {

    private static final int BAR_WIDTH = 62;
    private static final int BAR_HEIGHT = 7;
    private static final int SLOT_SIZE = 20;

    // Pixel-art palette (ARGB).
    private static final int COLOR_BORDER = 0xFF1B1B1B;
    private static final int COLOR_BACK = 0xC8262626;
    private static final int COLOR_HEALTH = 0xFFD83B3B;
    private static final int COLOR_HEALTH_HI = 0xFFF06A6A;
    private static final int COLOR_MANA = 0xFF3B6BD8;
    private static final int COLOR_MANA_HI = 0xFF6A96F0;
    private static final int COLOR_STAMINA = 0xFF3BB54A;
    private static final int COLOR_STAMINA_HI = 0xFF74E083;
    private static final int COLOR_XP = 0xFFB13BD8;
    private static final int COLOR_XP_HI = 0xFFD26AF0;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        PlayerRpg rpg = ClientRpgData.get();

        if (MagikClientConfig.SHOW_VITALS.get()) {
            renderVitals(graphics, player, rpg);
        }
        if (MagikClientConfig.SHOW_XP_BAR.get()) {
            renderXpBar(graphics, minecraft, rpg);
        }
        if (MagikClientConfig.SHOW_SKILL_SLOTS.get()) {
            renderSkillSlots(graphics, minecraft, player, rpg, screenWidth, screenHeight);
        }
    }

    private void renderVitals(GuiGraphics graphics, LocalPlayer player, PlayerRpg rpg) {
        int x = MagikClientConfig.VITALS_X.get();
        int y = MagikClientConfig.VITALS_Y.get();

        float maxMana = RpgStats.maxMana(rpg, player);
        float maxStamina = RpgStats.maxStamina(rpg);
        boolean full = rpg.getMana() >= maxMana - 0.01F && rpg.getStamina() >= maxStamina - 0.01F;
        if (MagikClientConfig.HIDE_FULL_BARS.get() && full && rpg.getCooldowns().isEmpty()) {
            return;
        }

        drawBar(graphics, x, y, player.getHealth() / player.getMaxHealth(),
                COLOR_HEALTH, COLOR_HEALTH_HI,
                (int) player.getHealth() + "/" + (int) player.getMaxHealth());
        drawBar(graphics, x, y + BAR_HEIGHT + 3, rpg.getMana() / maxMana,
                COLOR_MANA, COLOR_MANA_HI,
                (int) rpg.getMana() + "/" + (int) maxMana);
        drawBar(graphics, x, y + (BAR_HEIGHT + 3) * 2, rpg.getStamina() / maxStamina,
                COLOR_STAMINA, COLOR_STAMINA_HI,
                (int) rpg.getStamina() + "/" + (int) maxStamina);
    }

    /** A bordered pixel bar with a lighter top row so it reads as vanilla pixel art. */
    private void drawBar(GuiGraphics graphics, int x, int y, float fill, int color, int highlight, String label) {
        fill = Mth.clamp(fill, 0.0F, 1.0F);
        graphics.fill(x, y, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 2, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, x + 1 + BAR_WIDTH, y + 1 + BAR_HEIGHT, COLOR_BACK);
        int filled = (int) (BAR_WIDTH * fill);
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + 1 + BAR_HEIGHT, color);
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + 2, highlight);
        }
        Minecraft minecraft = Minecraft.getInstance();
        graphics.pose().pushPose();
        graphics.pose().translate(x + BAR_WIDTH + 6, y + 1, 0);
        graphics.pose().scale(0.75F, 0.75F, 1.0F);
        graphics.drawString(minecraft.font, label, 0, 0, COLOR_TEXT, true);
        graphics.pose().popPose();
    }

    private void renderXpBar(GuiGraphics graphics, Minecraft minecraft, PlayerRpg rpg) {
        int x = MagikClientConfig.XP_BAR_X.get();
        int y = MagikClientConfig.XP_BAR_Y.get();

        String levelText = "Lv " + rpg.getLevel() + " - "
                + com.magik.player.PlayerTitles.getTitle(rpg).getString();
        graphics.drawString(minecraft.font, levelText, x + 1, y, 0xFFE3C55A, true);

        int barY = y + 10;
        int barWidth = BAR_WIDTH + 2;
        float fill = ClientRpgData.displayedXpProgress();
        graphics.fill(x, barY, x + barWidth + 2, barY + 5, COLOR_BORDER);
        graphics.fill(x + 1, barY + 1, x + 1 + barWidth, barY + 4, COLOR_BACK);
        int filled = (int) (barWidth * fill);
        if (filled > 0) {
            graphics.fill(x + 1, barY + 1, x + 1 + filled, barY + 4, COLOR_XP);
            graphics.fill(x + 1, barY + 1, x + 1 + filled, barY + 2, COLOR_XP_HI);
        }

        String xpText = rpg.getXp() + "/" + RpgStats.xpForNextLevel(rpg.getLevel());
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1, barY + 7, 0);
        graphics.pose().scale(0.6F, 0.6F, 1.0F);
        graphics.drawString(minecraft.font, xpText, 0, 0, 0xFFCCCCCC, true);
        graphics.pose().popPose();
    }

    private void renderSkillSlots(GuiGraphics graphics, Minecraft minecraft, LocalPlayer player,
                                  PlayerRpg rpg, int screenWidth, int screenHeight) {
        int totalWidth = PlayerRpg.SKILL_SLOTS * (SLOT_SIZE + 2) - 2;
        int x = screenWidth / 2 + 95;
        int y = screenHeight - SLOT_SIZE - 3;
        if (x + totalWidth > screenWidth) {
            x = screenWidth - totalWidth - 2;
        }

        long gameTime = player.level().getGameTime();
        for (int slot = 0; slot < PlayerRpg.SKILL_SLOTS; slot++) {
            int slotX = x + slot * (SLOT_SIZE + 2);
            graphics.fill(slotX, y, slotX + SLOT_SIZE, y + SLOT_SIZE, COLOR_BORDER);
            graphics.fill(slotX + 1, y + 1, slotX + SLOT_SIZE - 1, y + SLOT_SIZE - 1, COLOR_BACK);

            String skillId = rpg.getSkillSlots()[slot];
            if (skillId != null) {
                graphics.blit(SkillIcons.get(skillId), slotX + 2, y + 2, SLOT_SIZE - 4, SLOT_SIZE - 4,
                        0.0F, 0.0F, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE, SkillIcons.SIZE);
                float cooldown = cooldownFraction(rpg, skillId, gameTime);
                if (cooldown > 0.0F) {
                    int height = (int) ((SLOT_SIZE - 2) * cooldown);
                    graphics.fill(slotX + 1, y + SLOT_SIZE - 1 - height,
                            slotX + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xA0FFFFFF);
                }
            }

            String key = KeyBindings.SKILL_SLOTS[slot].getTranslatedKeyMessage().getString();
            graphics.pose().pushPose();
            graphics.pose().translate(slotX + 2, y + SLOT_SIZE - 6, 100);
            graphics.pose().scale(0.55F, 0.55F, 1.0F);
            graphics.drawString(minecraft.font, key, 0, 0, COLOR_TEXT, true);
            graphics.pose().popPose();
        }
    }

    /** Remaining cooldown [0..1] for the sweep overlay. */
    private float cooldownFraction(PlayerRpg rpg, String skillId, long gameTime) {
        Skill skill = SkillTrees.get(skillId);
        if (skill == null || skill.getCooldownTicks() <= 0) {
            return 0.0F;
        }
        Long readyAt = rpg.getCooldowns().get(skillId);
        if (readyAt == null || readyAt <= gameTime) {
            return 0.0F;
        }
        float total = skill.getCooldownTicks() * RpgStats.cooldownMultiplier(rpg);
        return Mth.clamp((readyAt - gameTime) / total, 0.0F, 1.0F);
    }
}
