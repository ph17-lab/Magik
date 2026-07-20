package com.magik.client;

import com.magik.MagikMod;
import com.magik.client.hud.RpgHudOverlay;
import com.magik.client.render.MagicBoltRenderer;
import com.magik.registry.ModEntities;
import com.magik.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

/** Mod-bus client registrations: renderers, overlays, key mappings, item model predicates. */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties(ModItems.SHORT_BOW);
            registerBowProperties(ModItems.LONG_BOW);
            registerBowProperties(ModItems.COMPOSITE_BOW);
            registerBowProperties(ModItems.ELVISH_BOW);
            registerBowProperties(ModItems.ARCANE_BOW);
            registerBowProperties(ModItems.LEGENDARY_BOW);

            registerShieldProperties(ModItems.WOODEN_SHIELD);
            registerShieldProperties(ModItems.IRON_SHIELD);
            registerShieldProperties(ModItems.GOLDEN_SHIELD);
            registerShieldProperties(ModItems.DIAMOND_SHIELD);
            registerShieldProperties(ModItems.OBSIDIAN_SHIELD);
            registerShieldProperties(ModItems.LEGENDARY_SHIELD);
        });
    }

    /** Vanilla only registers pull/pulling for Items.BOW; mirror it for our bows. */
    private static void registerBowProperties(RegistryObject<Item> bow) {
        ItemProperties.register(bow.get(), new ResourceLocation("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
                });
        ItemProperties.register(bow.get(), new ResourceLocation("pulling"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        if (!(bow.get() instanceof BowItem)) {
            throw new IllegalStateException("Bow properties registered for a non-bow item");
        }
    }

    /** Blocking predicate so shields swap to their raised model. */
    private static void registerShieldProperties(RegistryObject<Item> shield) {
        ItemProperties.register(shield.get(), new ResourceLocation("blocking"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_MENU);
        event.register(KeyBindings.DASH);
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
    }
}
