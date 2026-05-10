// Cancels spell casts in configured dimensions. Affects players only.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

public class SpellCastDimensionHandler {

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        List<? extends String> blockedDimensions = Config.SPELL_CASTING_DISABLED_DIMENSIONS.get();
        if (blockedDimensions.isEmpty()) {
            return;
        }
        ResourceLocation currentDimension = event.getEntity().level().dimension().location();
        String currentDimensionId = currentDimension.toString();
        if (!blockedDimensions.contains(currentDimensionId)) {
            return;
        }
        PlayerProgress progress = event.getEntity().getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        if (progress.getDimensionsRemoved().contains(currentDimension)) {
            return;
        }
        event.setCanceled(true);
    }
}
