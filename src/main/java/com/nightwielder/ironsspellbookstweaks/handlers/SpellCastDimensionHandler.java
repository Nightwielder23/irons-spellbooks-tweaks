// Cancels spell casts in configured dimensions. Affects players only.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpellCastDimensionHandler {

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        List<? extends String> blockedDimensions = RuntimeConfig.spellCastingDisabledDimensions;
        if (blockedDimensions.isEmpty()) {
            return;
        }
        ResourceLocation currentDimension = event.getEntity().level().dimension().location();
        String currentDimensionId = currentDimension.toString();
        if (!blockedDimensions.contains(currentDimensionId)) {
            return;
        }
        boolean playerExempted = event.getEntity().getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(progress -> progress.getDimensionsRemoved().contains(currentDimension))
                .orElse(false);
        if (playerExempted) {
            return;
        }
        event.setCanceled(true);
    }
}
