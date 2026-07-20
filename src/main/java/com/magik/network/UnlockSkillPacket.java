package com.magik.network;

import com.magik.combat.RpgAttributeApplier;
import com.magik.player.PlayerRpgProvider;
import com.magik.skills.Skill;
import com.magik.skills.SkillTrees;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: request to unlock a skill tree node. Server validates. */
public class UnlockSkillPacket {

    private final String skillId;

    public UnlockSkillPacket(String skillId) {
        this.skillId = skillId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId);
    }

    public static UnlockSkillPacket decode(FriendlyByteBuf buf) {
        return new UnlockSkillPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            Skill skill = SkillTrees.get(skillId);
            if (player == null || skill == null) {
                return;
            }
            PlayerRpgProvider.get(player).ifPresent(rpg -> {
                if (SkillTrees.canUnlock(rpg, skill)) {
                    rpg.setSkillPoints(rpg.getSkillPoints() - 1);
                    rpg.unlockSkill(skill.getId());
                    RpgAttributeApplier.apply(player, rpg); // Passives may add modifiers.
                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    MagikNetwork.syncFull(player, rpg);
                }
            });
        });
        context.get().setPacketHandled(true);
    }
}
