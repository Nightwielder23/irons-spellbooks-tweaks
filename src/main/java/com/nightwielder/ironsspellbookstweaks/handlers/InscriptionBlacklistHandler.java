// Cancels inscription of blacklisted spells at the inscription table. Spell IDs are matched as full namespaced strings (e.g. "irons_spellbooks:fireball").
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import java.util.List;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InscriptionBlacklistHandler {

    @SubscribeEvent
    public static void onInscribeSpell(InscribeSpellEvent event) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        List<? extends String> blacklist = Config.INSCRIPTION_BLACKLIST.get();
        if (blacklist.isEmpty()) {
            return;
        }
        String spellId = event.getSpellData().getSpell().getSpellId();
        if (blacklist.contains(spellId)) {
            event.setCanceled(true);
        }
    }
}
