package com.magik;

import com.magik.config.MagikClientConfig;
import com.magik.config.MagikServerConfig;
import com.magik.config.SkillBalance;
import com.magik.network.MagikNetwork;
import com.magik.registry.ModCreativeTabs;
import com.magik.registry.ModEntities;
import com.magik.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik RPG - a complete RPG expansion for Minecraft.
 *
 * <p>Architecture overview (each concern lives in its own package):</p>
 * <ul>
 *   <li>{@code player}      - the {@link com.magik.player.PlayerRpg} capability (level, XP,
 *                             attributes, mana, stamina, skills) and its lifecycle events.</li>
 *   <li>{@code progression} - XP sources (combat, crafting, mining, advancements) and level curve.</li>
 *   <li>{@code combat}      - hooks that translate attributes into real gameplay effects.</li>
 *   <li>{@code skills}      - skill tree definitions and server-side skill casting.</li>
 *   <li>{@code network}     - client/server synchronization packets.</li>
 *   <li>{@code item}/{@code entity} - RPG weapons, gear, consumables and spell projectiles.</li>
 *   <li>{@code client}      - HUD overlay, character/skill screens and key bindings.</li>
 * </ul>
 */
@Mod(MagikMod.MOD_ID)
public final class MagikMod {

    public static final String MOD_ID = "magik";
    public static final Logger LOGGER = LoggerFactory.getLogger("MagikRPG");

    public MagikMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MagikServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MagikClientConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MagikNetwork.register();
            SkillBalance.load();
        });
    }

    /** Convenience factory for namespaced resource locations. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
