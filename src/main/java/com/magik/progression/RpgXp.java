package com.magik.progression;

import com.magik.combat.RpgAttributeApplier;
import com.magik.network.MagikNetwork;
import com.magik.player.PlayerRpgProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Single entry point for granting RPG XP. Handles level ups, celebratory
 * feedback and client synchronization, so XP sources only need to call
 * {@link #grant(ServerPlayer, int)}.
 */
public final class RpgXp {

    private RpgXp() {
    }

    public static void grant(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            int levelsGained = rpg.addXp(amount);
            if (levelsGained > 0) {
                RpgAttributeApplier.apply(player, rpg);
                celebrate(player, rpg.getLevel());
            }
            MagikNetwork.syncFull(player, rpg);
        });
    }

    private static void celebrate(ServerPlayer player, int newLevel) {
        Level level = player.level();
        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.0F);
        player.displayClientMessage(
                Component.translatable("message.magik.level_up", newLevel), true);
    }
}
