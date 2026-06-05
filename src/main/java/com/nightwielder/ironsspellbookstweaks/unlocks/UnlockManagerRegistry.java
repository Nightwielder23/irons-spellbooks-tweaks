// Attaches the singleton UnlockManager to the server's resource reload listeners.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UnlockManagerRegistry {

    private static final UnlockManager INSTANCE = new UnlockManager();

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
