// Routes advancement-earned events to any unlocks indexed against that advancement id.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
public class AdvancementUnlockHandler {

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ResourceLocation advancementId = event.getAdvancement().id();
        List<UnlockDefinition> matchingUnlocks = UnlockManager.getByAdvancement(advancementId);
        if (matchingUnlocks.isEmpty()) {
            return;
        }
        for (UnlockDefinition unlock : matchingUnlocks) {
            UnlockApplicator.apply(player, unlock);
        }
    }
}
