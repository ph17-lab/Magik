package com.magik.network;

import com.magik.skills.SkillCasting;
import com.magik.skills.SkillTrees;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: request a stamina dash (Espadachim "Dash" skill). */
public class DashPacket {

    public DashPacket() {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static DashPacket decode(FriendlyByteBuf buf) {
        return new DashPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                SkillCasting.cast(player, SkillTrees.DASH);
            }
        });
        context.get().setPacketHandled(true);
    }
}
