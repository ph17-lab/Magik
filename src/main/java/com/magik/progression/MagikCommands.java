package com.magik.progression;

import com.magik.MagikMod;
import com.magik.combat.RpgAttributeApplier;
import com.magik.config.MagikServerConfig;
import com.magik.network.MagikNetwork;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.player.RpgAttribute;
import com.magik.player.RpgStats;
import com.magik.skills.SkillTrees;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Admin commands for testing and server management:
 * /magikrpg xp <amount> | level <level> | reset
 * /fullxp - max level, all skills unlocked and attributes mastered.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class MagikCommands {

    private MagikCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("magikrpg")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> grantXp(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                .executes(ctx -> setLevel(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource()))));

        event.getDispatcher().register(Commands.literal("fullxp")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> fullXp(ctx.getSource())));
    }

    /** Testing shortcut: max level, every skill unlocked, attributes mastered, vitals refilled. */
    private static int fullXp(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            int maxLevel = MagikServerConfig.MAX_LEVEL.get();
            rpg.setLevel(maxLevel);
            rpg.setXp(0);

            // Master every attribute: push each one to its maximum.
            int cap = MagikServerConfig.MAX_ATTRIBUTE.get();
            for (RpgAttribute attribute : RpgAttribute.values()) {
                rpg.setAttribute(attribute, cap);
            }
            rpg.setAttributePoints(0);
            rpg.setSkillPoints(0);

            for (String skillId : SkillTrees.all().keySet()) {
                rpg.unlockSkill(skillId);
            }
            rpg.getCooldowns().clear();

            RpgAttributeApplier.apply(player, rpg);
            rpg.setMana(RpgStats.maxMana(rpg, player));
            rpg.setStamina(RpgStats.maxStamina(rpg));
            MagikNetwork.syncFull(player, rpg);
        });
        source.sendSuccess(() -> Component.literal(
                "Full XP: max level, all 40 skills unlocked, attributes mastered"), true);
        return 1;
    }

    private static int grantXp(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        RpgXp.grant(player, amount);
        source.sendSuccess(() -> Component.literal("Granted " + amount + " RPG XP"), true);
        return 1;
    }

    private static int setLevel(CommandSourceStack source, int level) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            rpg.setLevel(level);
            rpg.setXp(0);
            RpgAttributeApplier.apply(player, rpg);
            MagikNetwork.syncFull(player, rpg);
        });
        source.sendSuccess(() -> Component.literal("RPG level set to " + level), true);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            rpg.copyFrom(new PlayerRpg());
            RpgAttributeApplier.apply(player, rpg);
            MagikNetwork.syncFull(player, rpg);
        });
        source.sendSuccess(() -> Component.literal("RPG progress reset"), true);
        return 1;
    }
}
