package com.magik.network;

import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.skills.Skill;
import com.magik.skills.SkillTrees;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: assign an unlocked active skill to a HUD hotkey slot. */
public class AssignSkillSlotPacket {

    private final int slot;
    /** Empty string clears the slot. */
    private final String skillId;

    public AssignSkillSlotPacket(int slot, String skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeUtf(skillId);
    }

    public static AssignSkillSlotPacket decode(FriendlyByteBuf buf) {
        return new AssignSkillSlotPacket(buf.readVarInt(), buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || slot < 0 || slot >= PlayerRpg.SKILL_SLOTS) {
                return;
            }
            PlayerRpgProvider.get(player).ifPresent(rpg -> {
                if (skillId.isEmpty()) {
                    rpg.setSkillSlot(slot, null);
                } else {
                    Skill skill = SkillTrees.get(skillId);
                    if (skill == null || skill.getType() != Skill.Type.ACTIVE || !rpg.hasSkill(skillId)) {
                        return;
                    }
                    // A skill can only occupy one slot at a time.
                    for (int i = 0; i < PlayerRpg.SKILL_SLOTS; i++) {
                        if (skillId.equals(rpg.getSkillSlots()[i])) {
                            rpg.setSkillSlot(i, null);
                        }
                    }
                    rpg.setSkillSlot(slot, skillId);
                }
                MagikNetwork.syncFull(player, rpg);
            });
        });
        context.get().setPacketHandled(true);
    }
}
