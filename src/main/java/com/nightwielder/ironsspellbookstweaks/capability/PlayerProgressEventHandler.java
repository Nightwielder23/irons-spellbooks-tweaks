// Hooks the PlayerProgress capability into player lifecycle: attach on entity creation and copy across respawn or end-portal return.
package com.nightwielder.ironsspellbookstweaks.capability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerProgressEventHandler {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerProgressProvider provider = new PlayerProgressProvider();
            event.addCapability(PlayerProgressProvider.IDENTIFIER, provider);
        }
    }

    // Fires on both death respawn (wasDeath=true) and return-from-end (wasDeath=false). Both paths build a fresh Player with empty capabilities, so the capability is copied in either case.
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        // 1.20.1 invalidates the original entity's caps before this fires, so revive long enough to read them
        original.reviveCaps();
        original.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(originalProgress -> {
            clone.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(cloneProgress -> {
                cloneProgress.copyFrom(originalProgress);
            });
        });
        original.invalidateCaps();
    }
}
