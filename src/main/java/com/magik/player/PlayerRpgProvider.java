package com.magik.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Capability provider that attaches a {@link PlayerRpg} instance to every player
 * and persists it inside the player's NBT.
 */
public class PlayerRpgProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerRpg> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final PlayerRpg data = new PlayerRpg();
    private final LazyOptional<PlayerRpg> optional = LazyOptional.of(() -> data);

    /** Convenience accessor; empty if the capability is missing (e.g. fake players). */
    public static Optional<PlayerRpg> get(Player player) {
        return player.getCapability(CAPABILITY).resolve();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
