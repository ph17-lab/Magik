package com.magik.network;

import com.magik.combat.RpgAttributeApplier;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgAttribute;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: request to spend one attribute point. Server validates. */
public class SpendAttributePointPacket {

    private final RpgAttribute attribute;

    public SpendAttributePointPacket(RpgAttribute attribute) {
        this.attribute = attribute;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(attribute);
    }

    public static SpendAttributePointPacket decode(FriendlyByteBuf buf) {
        return new SpendAttributePointPacket(buf.readEnum(RpgAttribute.class));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            PlayerRpgProvider.get(player).ifPresent(rpg -> {
                if (rpg.spendAttributePoint(attribute)) {
                    RpgAttributeApplier.apply(player, rpg);
                    MagikNetwork.syncFull(player, rpg);
                }
            });
        });
        context.get().setPacketHandled(true);
    }
}
