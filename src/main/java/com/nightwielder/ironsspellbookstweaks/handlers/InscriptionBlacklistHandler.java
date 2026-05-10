// Cancels inscription of blacklisted spells at the inscription table. Spell IDs are matched as full namespaced strings (e.g. "irons_spellbooks:fireball").
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;

public class InscriptionBlacklistHandler {

    @SubscribeEvent
    public static void onInscribeSpell(InscribeSpellEvent event) {
        List<? extends String> blacklist = Config.INSCRIPTION_BLACKLIST.get();
        if (blacklist.isEmpty()) {
            return;
        }
        String spellIdString = event.getSpellData().getSpell().getSpellId();
        if (!blacklist.contains(spellIdString)) {
            return;
        }
        ResourceLocation spellId = ResourceLocation.tryParse(spellIdString);
        if (spellId != null) {
            PlayerProgress progress = event.getEntity().getData(PlayerProgressAttachments.PLAYER_PROGRESS);
            if (progress.getInscriptionsRemoved().contains(spellId)) {
                return;
            }
        }
        event.setCanceled(true);
    }
}
