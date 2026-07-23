package com.magik.network;

import com.magik.combat.RpgAttributeApplier;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: reset every unlocked skill, refunding one skill point per
 * skill so the player can rebuild their character freely. Server validated.
 */
public class RespecPacket {

    public RespecPacket() {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static RespecPacket decode(FriendlyByteBuf buf) {
        return new RespecPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            PlayerRpgProvider.get(player).ifPresent(rpg -> {
                int refund = rpg.getUnlockedSkills().size();
                if (refund <= 0) {
                    return;
                }
                rpg.setSkillPoints(rpg.getSkillPoints() + refund);
                rpg.getUnlockedSkills().clear();
                for (int slot = 0; slot < PlayerRpg.SKILL_SLOTS; slot++) {
                    rpg.setSkillSlot(slot, null);
                }
                rpg.getCooldowns().clear();
                RpgAttributeApplier.apply(player, rpg); // Remove passive modifiers.
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.9F, 1.1F);
                MagikNetwork.syncFull(player, rpg);
            });
        });
        context.get().setPacketHandled(true);
    }
}
