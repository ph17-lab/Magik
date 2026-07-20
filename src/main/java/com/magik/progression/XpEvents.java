package com.magik.progression;

import com.magik.MagikMod;
import com.magik.config.MagikServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * All RPG XP sources: combat kills, crafting, smelting, ore mining and
 * mod/vanilla advancements. Values are configurable in
 * {@link MagikServerConfig}. Vanilla XP is untouched.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class XpEvents {

    private XpEvents() {
    }

    /** Combat: XP scales with the victim's max health, so bosses pay big. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() != player) {
            double xp = (3.0D + event.getEntity().getMaxHealth() * 0.6D)
                    * MagikServerConfig.KILL_XP_MULTIPLIER.get();
            RpgXp.grant(player, (int) Math.round(xp));
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getCrafting().isEmpty()) {
            RpgXp.grant(player, MagikServerConfig.CRAFT_XP.get());
        }
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getSmelting().isEmpty()) {
            RpgXp.grant(player, MagikServerConfig.SMELT_XP.get());
        }
    }

    /** Mining: only ores grant XP, with a premium for the rarest ones. */
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        if (isRareOre(state)) {
            RpgXp.grant(player, MagikServerConfig.RARE_ORE_XP.get());
        } else if (state.is(Tags.Blocks.ORES)) {
            RpgXp.grant(player, MagikServerConfig.ORE_XP.get());
        }
    }

    private static boolean isRareOre(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    /** In-game achievements: every advancement with a visible display grants XP. */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getAdvancement().getDisplay() != null) {
            RpgXp.grant(player, MagikServerConfig.ADVANCEMENT_XP.get());
        }
    }
}
