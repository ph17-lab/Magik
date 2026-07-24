package com.magik.progression;

import com.magik.MagikMod;
import com.magik.combat.CombatEvents;
import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.skills.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Passive gameplay effects of the gathering trees: Miner (fortune, double
 * drops, mining XP, pickaxe durability), Lumberjack (whole-tree felling, extra
 * wood, axe durability), Farmer (area harvest, auto-replant, extra yield) and
 * Fisher (extra loot, rod durability). All server-side.
 */
@Mod.EventBusSubscriber(modid = MagikMod.MOD_ID)
public final class ProgressionEvents {

    private static final int FELLER_LIMIT = 96;

    private ProgressionEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        PlayerRpg rpg = PlayerRpgProvider.get(player).orElse(null);
        if (rpg == null || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack tool = player.getMainHandItem();
        String path = pathOf(state.getBlock());
        boolean ore = path.contains("_ore") || path.equals("ancient_debris");

        // --- Farmer: mature crops (handle drops + replant ourselves) ---
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            handleFarming(event, player, level, rpg, pos, state, crop);
            return;
        }

        // --- Miner ---
        if (ore) {
            if (rpg.hasSkill(SkillTrees.MI_XP)) {
                player.giveExperiencePoints(2);
            }
            if (rpg.hasSkill(SkillTrees.MI_DOUBLE) && player.getRandom().nextFloat() < 0.20F) {
                dropCopy(level, pos, state, player, tool);
            } else if (rpg.hasSkill(SkillTrees.MI_FORTUNE) && player.getRandom().nextFloat() < 0.35F) {
                dropCopy(level, pos, state, player, tool);
            }
            if (rpg.hasSkill(SkillTrees.MI_DURABILITY) && player.getRandom().nextFloat() < 0.30F) {
                CombatEvents.restoreDurability(tool);
            }
        }

        // --- Lumberjack ---
        if (state.is(BlockTags.LOGS)) {
            if (rpg.hasSkill(SkillTrees.LU_YIELD) && player.getRandom().nextFloat() < 0.25F) {
                dropCopy(level, pos, state, player, tool);
            }
            if (rpg.hasSkill(SkillTrees.LU_DURABILITY) && player.getRandom().nextFloat() < 0.30F) {
                CombatEvents.restoreDurability(tool);
            }
            if (rpg.hasSkill(SkillTrees.LU_FELLER) && tool.getItem() instanceof net.minecraft.world.item.AxeItem) {
                fellTree(level, pos, state, player);
            }
        }
    }

    private static void handleFarming(BlockEvent.BreakEvent event, ServerPlayer player, ServerLevel level,
                                      PlayerRpg rpg, BlockPos center, BlockState state, CropBlock crop) {
        boolean area = rpg.hasSkill(SkillTrees.FA_AREA);
        boolean replant = rpg.hasSkill(SkillTrees.FA_REPLANT);
        boolean yield = rpg.hasSkill(SkillTrees.FA_YIELD);
        event.setCanceled(true);

        int radius = area ? 1 : 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos p = center.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (!(s.getBlock() instanceof CropBlock c) || !c.isMaxAge(s)) {
                    continue;
                }
                List<ItemStack> drops = Block.getDrops(s, level, p, null, player, player.getMainHandItem());
                for (ItemStack drop : drops) {
                    Block.popResource(level, p, drop.copy());
                    if (yield && player.getRandom().nextFloat() < 0.30F
                            && !drop.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(
                                    new net.minecraft.resources.ResourceLocation("forge", "seeds")))) {
                        Block.popResource(level, p, drop.copy());
                    }
                }
                if (replant) {
                    level.setBlock(p, c.defaultBlockState(), 3);
                } else {
                    level.destroyBlock(p, false);
                }
            }
        }
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.CROP_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    /** Breaks every connected log above/around the felled one. */
    private static void fellTree(ServerLevel level, BlockPos origin, BlockState logState, ServerPlayer player) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        int broken = 0;
        while (!queue.isEmpty() && broken < FELLER_LIMIT) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos n = p.offset(dx, dy, dz);
                        if (visited.contains(n) || n.equals(origin)) {
                            continue;
                        }
                        if (level.getBlockState(n).is(BlockTags.LOGS)) {
                            visited.add(n);
                            queue.add(n);
                            level.destroyBlock(n, true, player);
                            broken++;
                            if (broken >= FELLER_LIMIT) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void dropCopy(ServerLevel level, BlockPos pos, BlockState state,
                                 ServerPlayer player, ItemStack tool) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop.copy());
        }
    }

    // --- Fisher ---

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerRpg rpg = PlayerRpgProvider.get(player).orElse(null);
        if (rpg == null) {
            return;
        }
        if (rpg.hasSkill(SkillTrees.FI_RARE) && player.getRandom().nextFloat() < 0.30F) {
            for (ItemStack drop : event.getDrops()) {
                Block.popResource((ServerLevel) player.level(), player.blockPosition(), drop.copy());
            }
        }
        if (rpg.hasSkill(SkillTrees.FI_DURABILITY) && player.getRandom().nextFloat() < 0.40F) {
            CombatEvents.restoreDurability(player.getMainHandItem());
            CombatEvents.restoreDurability(player.getOffhandItem());
        }
    }

    // --- Farmer growth: accelerate nearby crops ---

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 40 != 0) {
            return;
        }
        PlayerRpgProvider.get(player).ifPresent(rpg -> {
            if (!rpg.hasSkill(SkillTrees.FA_GROWTH)) {
                return;
            }
            BlockPos base = player.blockPosition();
            for (int i = 0; i < 3; i++) {
                BlockPos p = base.offset(player.getRandom().nextInt(9) - 4, 0,
                        player.getRandom().nextInt(9) - 4);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) {
                    crop.randomTick(s, level, p, level.random);
                }
            }
        });
    }

    private static String pathOf(Block block) {
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
        return id != null ? id.getPath() : "";
    }
}

