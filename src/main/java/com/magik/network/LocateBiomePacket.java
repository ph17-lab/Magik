package com.magik.network;

import com.magik.item.SpecialCompassItem;
import com.magik.player.PlayerRpgProvider;
import com.magik.registry.ModItems;
import com.magik.skills.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player picked a biome in the world map. The server
 * locates it and points every biome compass they carry at it.
 */
public class LocateBiomePacket {

    private final int biomeIndex;

    public LocateBiomePacket(int biomeIndex) {
        this.biomeIndex = biomeIndex;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(biomeIndex);
    }

    public static LocateBiomePacket decode(FriendlyByteBuf buf) {
        return new LocateBiomePacket(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            boolean allowed = PlayerRpgProvider.get(player)
                    .map(rpg -> rpg.hasSkill(SkillTrees.EX_BIOME_COMPASS)).orElse(false);
            if (!allowed) {
                player.displayClientMessage(Component.translatable("message.magik.compass_locked")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            Component name = SpecialCompassItem.biomeName(biomeIndex);
            BlockPos found = SpecialCompassItem.findBiome(level, player.blockPosition(), biomeIndex);
            if (found == null) {
                player.displayClientMessage(Component.translatable("message.magik.compass_not_found", name)
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }

            int pointed = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.BIOME_COMPASS.get())) {
                    SpecialCompassItem.setTarget(stack, found, biomeIndex);
                    pointed++;
                }
            }
            ItemStack offhand = player.getOffhandItem();
            if (offhand.is(ModItems.BIOME_COMPASS.get())) {
                SpecialCompassItem.setTarget(offhand, found, biomeIndex);
                pointed++;
            }

            if (pointed == 0) {
                player.displayClientMessage(Component.translatable("message.magik.need_biome_compass")
                        .withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(found));
            player.displayClientMessage(Component.translatable("message.magik.compass_found", name, distance)
                    .withStyle(ChatFormatting.GREEN), true);
        });
        context.get().setPacketHandled(true);
    }
}
