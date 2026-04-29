package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Drainback for disableManaRegenEntirely. Iron's regen does not fire an event we can cancel, so we sample mana per tick and undo any increase.
public class ManaRegenCancelHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player.level().isClientSide) {
            return;
        }
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        if (!Config.DISABLE_MANA_REGEN_ENTIRELY.get()) {
            return;
        }
        // TODO: sample current mana via Iron's MagicData reflection, store last known value per player UUID, drain back any positive delta
    }
}
