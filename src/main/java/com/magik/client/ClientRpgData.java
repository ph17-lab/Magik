package com.magik.client;

import com.magik.player.PlayerRpg;
import com.magik.player.RpgStats;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * Client-side cache of the local player's RPG state, fed by sync packets.
 * Between server syncs the client also *predicts* mana/stamina regeneration
 * with the same formulas, so HUD bars fill smoothly instead of stepping.
 */
public final class ClientRpgData {

    private static final PlayerRpg DATA = new PlayerRpg();

    /** Smoothed XP bar fill [0..1] for the animated HUD bar. */
    private static float displayedXpProgress = 0.0F;

    private ClientRpgData() {
    }

    public static PlayerRpg get() {
        return DATA;
    }

    public static void acceptFullSync(CompoundTag tag) {
        if (tag != null) {
            DATA.deserializeNBT(tag);
        }
    }

    public static void acceptVitalsSync(float mana, float stamina) {
        DATA.setMana(mana);
        DATA.setStamina(stamina);
    }

    /** Called every client tick: local regen prediction + XP bar animation. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        float maxMana = RpgStats.maxMana(DATA, minecraft.player);
        if (DATA.getMana() < maxMana) {
            DATA.setMana(Math.min(maxMana, DATA.getMana() + RpgStats.manaRegenPerTick(DATA)));
        }

        long gameTime = minecraft.player.level().getGameTime();
        float maxStamina = RpgStats.maxStamina(DATA);
        if (DATA.getStamina() < maxStamina
                && gameTime - DATA.getLastStaminaUse() >= RpgStats.STAMINA_REGEN_DELAY_TICKS) {
            DATA.setStamina(Math.min(maxStamina, DATA.getStamina() + RpgStats.staminaRegenPerTick(DATA)));
        }

        float target = xpProgress();
        // When the bar wraps around on level up, snap back before animating again.
        if (target < displayedXpProgress - 0.5F) {
            displayedXpProgress = 0.0F;
        }
        displayedXpProgress = Mth.lerp(0.15F, displayedXpProgress, target);
    }

    public static float xpProgress() {
        int needed = RpgStats.xpForNextLevel(DATA.getLevel());
        return needed <= 0 ? 0.0F : Mth.clamp(DATA.getXp() / (float) needed, 0.0F, 1.0F);
    }

    public static float displayedXpProgress() {
        return displayedXpProgress;
    }
}
