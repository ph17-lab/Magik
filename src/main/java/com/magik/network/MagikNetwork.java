package com.magik.network;

import com.magik.MagikMod;
import com.magik.player.PlayerRpg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central networking channel. The server is always authoritative: clients only
 * send intents (spend point, unlock, cast, dash) and receive state syncs.
 */
public final class MagikNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            MagikMod.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private MagikNetwork() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(SyncRpgDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncRpgDataPacket::encode)
                .decoder(SyncRpgDataPacket::decode)
                .consumerMainThread(SyncRpgDataPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncVitalsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncVitalsPacket::encode)
                .decoder(SyncVitalsPacket::decode)
                .consumerMainThread(SyncVitalsPacket::handle)
                .add();

        CHANNEL.messageBuilder(SpendAttributePointPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpendAttributePointPacket::encode)
                .decoder(SpendAttributePointPacket::decode)
                .consumerMainThread(SpendAttributePointPacket::handle)
                .add();

        CHANNEL.messageBuilder(UnlockSkillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockSkillPacket::encode)
                .decoder(UnlockSkillPacket::decode)
                .consumerMainThread(UnlockSkillPacket::handle)
                .add();

        CHANNEL.messageBuilder(CastSkillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CastSkillPacket::encode)
                .decoder(CastSkillPacket::decode)
                .consumerMainThread(CastSkillPacket::handle)
                .add();

        CHANNEL.messageBuilder(AssignSkillSlotPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AssignSkillSlotPacket::encode)
                .decoder(AssignSkillSlotPacket::decode)
                .consumerMainThread(AssignSkillSlotPacket::handle)
                .add();

        CHANNEL.messageBuilder(DashPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DashPacket::encode)
                .decoder(DashPacket::decode)
                .consumerMainThread(DashPacket::handle)
                .add();

        CHANNEL.messageBuilder(RespecPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RespecPacket::encode)
                .decoder(RespecPacket::decode)
                .consumerMainThread(RespecPacket::handle)
                .add();

        CHANNEL.messageBuilder(WaypointPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WaypointPacket::encode)
                .decoder(WaypointPacket::decode)
                .consumerMainThread(WaypointPacket::handle)
                .add();

        CHANNEL.messageBuilder(LocateBiomePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(LocateBiomePacket::encode)
                .decoder(LocateBiomePacket::decode)
                .consumerMainThread(LocateBiomePacket::handle)
                .add();
    }

    /** Sends the complete RPG state to the owning client. */
    public static void syncFull(ServerPlayer player, PlayerRpg rpg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncRpgDataPacket(rpg.serializeNBT()));
    }

    /** Lightweight mana/stamina sync used by the regeneration loop. */
    public static void syncVitals(ServerPlayer player, PlayerRpg rpg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncVitalsPacket(rpg.getMana(), rpg.getStamina()));
    }
}
