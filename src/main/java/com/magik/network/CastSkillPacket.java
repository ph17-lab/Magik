package com.magik.network;

import com.magik.skills.SkillCasting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: request to cast an active skill. Server validates everything. */
public class CastSkillPacket {

    private final String skillId;

    public CastSkillPacket(String skillId) {
        this.skillId = skillId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId);
    }

    public static CastSkillPacket decode(FriendlyByteBuf buf) {
        return new CastSkillPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                SkillCasting.cast(player, skillId);
            }
        });
        context.get().setPacketHandled(true);
    }
}
