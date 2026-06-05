// Cancels inscription of blacklisted spells at the inscription table. Spell IDs are matched as full namespaced strings (e.g. "irons_spellbooks:fireball").
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InscriptionBlacklistHandler {

    @SubscribeEvent
    public static void onInscribeSpell(InscribeSpellEvent event) {
        List<? extends String> blacklist = RuntimeConfig.inscriptionBlacklist;
        if (blacklist.isEmpty()) {
            return;
        }
        String spellIdString = event.getSpellData().getSpell().getSpellId();
        if (!blacklist.contains(spellIdString)) {
            return;
        }
        ResourceLocation spellId = ResourceLocation.tryParse(spellIdString);
        if (spellId != null) {
            boolean playerExempted = event.getEntity().getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                    .map(progress -> progress.getInscriptionsRemoved().contains(spellId))
                    .orElse(false);
            if (playerExempted) {
                return;
            }
        }
        event.setCanceled(true);
    }
}
