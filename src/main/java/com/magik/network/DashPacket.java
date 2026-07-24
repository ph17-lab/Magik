package com.magik.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: a light forward dash (free vanilla+ movement). */
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
            if (player == null || !player.onGround()) {
                return;
            }
            Vec3 look = player.getLookAngle();
            Vec3 dash = new Vec3(look.x, 0.0D, look.z).normalize().scale(1.1D).add(0.0D, 0.2D, 0.0D);
            player.setDeltaMovement(dash);
            player.hurtMarked = true;
        });
        context.get().setPacketHandled(true);
    }
}
