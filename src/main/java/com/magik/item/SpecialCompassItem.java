package com.magik.item;

import com.magik.player.PlayerRpg;
import com.magik.player.PlayerRpgProvider;
import com.magik.skills.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Explorer's special compasses. Right-click to search for the target
 * (nearest waypoint / biome / structure) and the needle then points to it;
 * sneak-right-click cycles which biome/structure is being searched. Requires
 * the matching Explorer skill to work.
 */
public class SpecialCompassItem extends CompassItem {

    public enum Mode {WAYPOINT, BIOME, STRUCTURE}

    /** Biomes the biome compass can search, in cycle order. */
    private static final ResourceKey<Biome>[] BIOMES = keys(Registries.BIOME,
            "cherry_grove", "desert", "jungle", "badlands", "mangrove_swamp", "mushroom_fields",
            "ice_spikes", "savanna", "meadow", "dark_forest");

    private static final ResourceKey<Structure>[] STRUCTURES = new ResourceKey[]{
            BuiltinStructures.VILLAGE_PLAINS, BuiltinStructures.STRONGHOLD, BuiltinStructures.ANCIENT_CITY,
            BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.OCEAN_MONUMENT, BuiltinStructures.MINESHAFT,
            BuiltinStructures.DESERT_PYRAMID, BuiltinStructures.SHIPWRECK_BEACHED,
            BuiltinStructures.RUINED_PORTAL_STANDARD, BuiltinStructures.PILLAGER_OUTPOST};

    private final Mode mode;

    public SpecialCompassItem(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<Biome>[] keys(ResourceKey<net.minecraft.core.Registry<Biome>> registry,
                                             String... names) {
        ResourceKey<Biome>[] out = new ResourceKey[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = ResourceKey.create(registry, new ResourceLocation(names[i]));
        }
        return out;
    }

    private String requiredSkill() {
        return switch (mode) {
            case WAYPOINT -> SkillTrees.EX_WAYPOINTS;
            case BIOME -> SkillTrees.EX_BIOME_COMPASS;
            case STRUCTURE -> SkillTrees.EX_STRUCT_COMPASS;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.consume(stack);
        }
        PlayerRpg rpg = PlayerRpgProvider.get(serverPlayer).orElse(null);
        if (rpg == null || !rpg.hasSkill(requiredSkill())) {
            serverPlayer.displayClientMessage(Component.translatable("message.magik.compass_locked")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (player.isShiftKeyDown() && mode != Mode.WAYPOINT) {
            cycleIndex(stack);
            serverPlayer.displayClientMessage(Component.translatable("message.magik.compass_target",
                    targetName(stack)).withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.success(stack);
        }

        BlockPos found = locate(serverLevel, serverPlayer, rpg, stack);
        if (found == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.magik.compass_not_found",
                    targetName(stack)).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("TX", found.getX());
        tag.putInt("TY", found.getY());
        tag.putInt("TZ", found.getZ());
        tag.putBoolean("TSet", true);
        int dist = (int) Math.sqrt(serverPlayer.blockPosition().distSqr(found));
        serverPlayer.displayClientMessage(Component.translatable("message.magik.compass_found",
                targetName(stack), dist).withStyle(ChatFormatting.GREEN), true);
        return InteractionResultHolder.success(stack);
    }

    @Nullable
    private BlockPos locate(ServerLevel level, ServerPlayer player, PlayerRpg rpg, ItemStack stack) {
        BlockPos origin = player.blockPosition();
        switch (mode) {
            case WAYPOINT -> {
                PlayerRpg.Waypoint nearest = null;
                double best = Double.MAX_VALUE;
                for (PlayerRpg.Waypoint wp : rpg.getWaypoints()) {
                    double d = origin.distSqr(new BlockPos(wp.x(), wp.y(), wp.z()));
                    if (d < best) {
                        best = d;
                        nearest = wp;
                    }
                }
                return nearest == null ? null : new BlockPos(nearest.x(), nearest.y(), nearest.z());
            }
            case BIOME -> {
                ResourceKey<Biome> key = BIOMES[index(stack) % BIOMES.length];
                var result = level.findClosestBiome3d(h -> h.is(key), origin, 6400, 32, 64);
                return result == null ? null : result.getFirst();
            }
            case STRUCTURE -> {
                ResourceKey<Structure> key = STRUCTURES[index(stack) % STRUCTURES.length];
                var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
                if (holder.isEmpty()) {
                    return null;
                }
                HolderSet<Structure> set = HolderSet.direct(holder.get());
                var result = level.getChunkSource().getGenerator()
                        .findNearestMapStructure(level, set, origin, 100, false);
                return result == null ? null : result.getFirst();
            }
        }
        return null;
    }

    private int index(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Idx");
    }

    private void cycleIndex(ItemStack stack) {
        int count = mode == Mode.BIOME ? BIOMES.length : STRUCTURES.length;
        stack.getOrCreateTag().putInt("Idx", (index(stack) + 1) % count);
    }

    private Component targetName(ItemStack stack) {
        return switch (mode) {
            case WAYPOINT -> Component.translatable("message.magik.nearest_waypoint");
            case BIOME -> Component.translatable("biome.minecraft." + BIOMES[index(stack) % BIOMES.length].location().getPath());
            case STRUCTURE -> Component.literal(STRUCTURES[index(stack) % STRUCTURES.length].location().getPath());
        };
    }

    /** Read the located target as a block position, or null if unset. */
    @Nullable
    public static BlockPos targetOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean("TSet")) {
            return null;
        }
        return new BlockPos(tag.getInt("TX"), tag.getInt("TY"), tag.getInt("TZ"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return targetOf(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.magik." + registryPath() + ".tooltip")
                .withStyle(ChatFormatting.GRAY));
        if (mode != Mode.WAYPOINT) {
            tooltip.add(Component.translatable("message.magik.compass_target", targetName(stack))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private String registryPath() {
        return switch (mode) {
            case WAYPOINT -> "waypoint_compass";
            case BIOME -> "biome_compass";
            case STRUCTURE -> "structure_compass";
        };
    }
}
