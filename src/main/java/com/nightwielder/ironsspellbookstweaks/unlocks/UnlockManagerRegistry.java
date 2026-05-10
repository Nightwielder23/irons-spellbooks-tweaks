// Attaches the singleton UnlockManager to the server's resource reload pipeline.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
public class UnlockManagerRegistry {

    private static final UnlockManager INSTANCE = new UnlockManager();

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}
