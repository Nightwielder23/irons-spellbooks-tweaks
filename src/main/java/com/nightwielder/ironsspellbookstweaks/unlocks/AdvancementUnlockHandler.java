// Re-evaluates unlocks for a player whenever they earn an advancement.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AdvancementUnlockHandler {

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        UnlockEvaluator.reevaluate(player, event.getAdvancement().getId());
    }
}
