package com.magik.client;

import com.magik.MagikMod;
import com.magik.config.MagikClientConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side Forge-bus HUD tweaks. The RPG HUD already draws a health bar,
 * so the vanilla heart row is hidden whenever the vitals panel is enabled.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HudEvents {

    private HudEvents() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                && MagikClientConfig.SHOW_VITALS.get()) {
            event.setCanceled(true);
        }
    }
}
