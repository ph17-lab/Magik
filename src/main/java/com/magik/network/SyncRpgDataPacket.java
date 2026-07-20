package com.magik.network;

import com.magik.client.ClientRpgData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: full RPG state snapshot. */
public class SyncRpgDataPacket {

    private final CompoundTag data;

    public SyncRpgDataPacket(CompoundTag data) {
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static SyncRpgDataPacket decode(FriendlyByteBuf buf) {
        return new SyncRpgDataPacket(buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> ClientRpgData.acceptFullSync(data)));
        context.get().setPacketHandled(true);
    }
}
