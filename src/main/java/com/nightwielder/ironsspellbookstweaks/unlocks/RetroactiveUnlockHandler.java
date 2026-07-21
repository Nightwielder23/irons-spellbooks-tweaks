// Re-evaluates unlocks on login. Catches advancement, kill-count, and composite unlocks that were already met before this session.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
public class RetroactiveUnlockHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UnlockEvaluator.reevaluate(serverPlayer);
    }
}
