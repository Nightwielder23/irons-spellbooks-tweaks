// Routes advancement-earned events to any unlocks indexed against that advancement id.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
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
        ResourceLocation advancementId = event.getAdvancement().getId();
        List<UnlockDefinition> matchingUnlocks = UnlockManager.getByAdvancement(advancementId);
        if (matchingUnlocks.isEmpty()) {
            return;
        }
        for (UnlockDefinition unlock : matchingUnlocks) {
            UnlockApplicator.apply(player, unlock);
        }
    }
}
