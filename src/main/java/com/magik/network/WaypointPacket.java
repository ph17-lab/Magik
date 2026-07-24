package com.magik.network;

import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.skills.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: add (at current position) or remove a named waypoint. */
public class WaypointPacket {

    private final boolean add;
    private final String name;

    public WaypointPacket(boolean add, String name) {
        this.add = add;
        this.name = name;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(add);
        buf.writeUtf(name);
    }

    public static WaypointPacket decode(FriendlyByteBuf buf) {
        return new WaypointPacket(buf.readBoolean(), buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            PlayerRpgProvider.get(player).ifPresent(rpg -> {
                if (!rpg.hasSkill(SkillTrees.EX_WAYPOINTS)) {
                    return;
                }
                if (add) {
                    String wpName = name.isEmpty()
                            ? "WP " + (rpg.getWaypoints().size() + 1) : name;
                    rpg.addWaypoint(new PlayerRpg.Waypoint(wpName,
                            player.getBlockX(), player.getBlockY(), player.getBlockZ()));
                    player.displayClientMessage(Component.translatable("message.magik.waypoint_added", wpName)
                            .withStyle(ChatFormatting.AQUA), true);
                } else {
                    rpg.removeWaypoint(name);
                }
                MagikNetwork.syncFull(player, rpg);
            });
        });
        context.get().setPacketHandled(true);
    }
}
