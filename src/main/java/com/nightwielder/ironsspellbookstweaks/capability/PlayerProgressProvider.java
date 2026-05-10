// Capability provider attached to every Player. Holds one PlayerProgress and serializes it to player data NBT.
package com.nightwielder.ironsspellbookstweaks.capability;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerProgressProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerProgress> PLAYER_PROGRESS = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation(IronsSpellbooksTweaks.MOD_ID, "player_progress");

    private final PlayerProgress progress = new PlayerProgress();
    private final LazyOptional<PlayerProgress> optional = LazyOptional.of(() -> progress);

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_PROGRESS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return progress.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        progress.deserializeNBT(tag);
    }
}
