package com.magik.network;

import com.magik.client.ClientRpgData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: lightweight mana/stamina correction. */
public class SyncVitalsPacket {

    private final float mana;
    private final float stamina;

    public SyncVitalsPacket(float mana, float stamina) {
        this.mana = mana;
        this.stamina = stamina;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(mana);
        buf.writeFloat(stamina);
    }

    public static SyncVitalsPacket decode(FriendlyByteBuf buf) {
        return new SyncVitalsPacket(buf.readFloat(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> ClientRpgData.acceptVitalsSync(mana, stamina)));
        context.get().setPacketHandled(true);
    }
}
