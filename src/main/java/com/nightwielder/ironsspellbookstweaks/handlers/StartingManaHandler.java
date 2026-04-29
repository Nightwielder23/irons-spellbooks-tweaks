package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// First-join max mana setter. Uses persistent NBT to track whether the player has been through this once before.
public class StartingManaHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        if (Config.STARTING_MAX_MANA.get() < 0) {
            return;
        }
        // TODO: read first-join flag from player persistent data, apply MAX_MANA attribute base value if unset, write flag
    }
}
