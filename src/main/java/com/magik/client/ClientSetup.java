package com.magik.client;

import com.magik.MagikMod;
import com.magik.client.hud.RpgHudOverlay;
import com.magik.client.render.MagicBoltRenderer;
import com.magik.item.SpecialCompassItem;
import com.magik.registry.ModEntities;
import com.magik.registry.ModItems;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Mod-bus client registrations: renderers, overlays, key mappings, item model predicates. */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerCompassAngle(ModItems.WAYPOINT_COMPASS.get());
            registerCompassAngle(ModItems.BIOME_COMPASS.get());
            registerCompassAngle(ModItems.STRUCTURE_COMPASS.get());
        });
    }

    /** Points the compass needle at the located target stored in the item's NBT. */
    private static void registerCompassAngle(Item compass) {
        ItemProperties.register(compass, new ResourceLocation("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    BlockPos target = SpecialCompassItem.targetOf(stack);
                    if (target == null || level == null) {
                        return null;
                    }
                    return GlobalPos.of(level.dimension(), target);
                }));
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_MENU);
        event.register(KeyBindings.DASH);
        event.register(KeyBindings.OPEN_MAP);
        event.register(KeyBindings.ADD_WAYPOINT);
        event.register(KeyBindings.MINIMAP_ZOOM);
        for (var mapping : KeyBindings.SKILL_SLOTS) {
            event.register(mapping);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAGIC_BOLT.get(), MagicBoltRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("rpg_hud", new RpgHudOverlay());
        event.registerAboveAll("magik_minimap", com.magik.client.map.MinimapRenderer.INSTANCE);
    }
}
