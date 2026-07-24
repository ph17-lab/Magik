package com.magik.client;

import com.magik.MagikMod;
import com.magik.client.gui.CharacterScreen;
import com.magik.network.CastSkillPacket;
import com.magik.network.DashPacket;
import com.magik.network.MagikNetwork;
import com.magik.player.PlayerRpg;
import com.magik.skills.SkillTrees;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-bus client events: key handling and the client prediction tick.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ClientRpgData.tick();

        while (KeyBindings.OPEN_MENU.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new CharacterScreen());
            }
        }

        while (KeyBindings.DASH.consumeClick()) {
            MagikNetwork.CHANNEL.sendToServer(new DashPacket());
        }

        PlayerRpg rpg = ClientRpgData.get();
        for (int slot = 0; slot < KeyBindings.SKILL_SLOTS.length; slot++) {
            while (KeyBindings.SKILL_SLOTS[slot].consumeClick()) {
                String skillId = rpg.getSkillSlots()[slot];
                if (skillId != null) {
                    MagikNetwork.CHANNEL.sendToServer(new CastSkillPacket(skillId));
                }
            }
        }
    }

}
