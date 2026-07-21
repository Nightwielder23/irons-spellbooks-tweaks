// Re-evaluates unlocks on login. Catches advancement, kill-count, and composite unlocks that were already met before this session.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RetroactiveUnlockHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UnlockEvaluator.reevaluate(serverPlayer);
    }
}
