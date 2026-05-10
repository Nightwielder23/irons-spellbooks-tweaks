// Cancels player spell casts whose minimum rarity is above the effective ceiling. Ceiling is the looser of the config gate and the player's per-player cap. Mob casters are unaffected because SpellPreCastEvent is a PlayerEvent.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpellRarityGateHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/SpellRarityGateHandler");

    // identity-compared cache so we re-parse the config string only when it changes; player cap is read fresh each cast
    private static String cachedRawValue;
    private static SpellRarity cachedThreshold;

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        SpellRarity configThreshold = getConfigThreshold();
        Player player = event.getEntity();
        PlayerProgress progress = player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        SpellRarity playerCap = parseRarity(progress.getRarityCap());
        SpellRarity effective = looserOf(configThreshold, playerCap);
        if (effective == null) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) {
            return;
        }
        // AbstractSpell.getMinRarity returns an int rank; go through SpellConfigManager to get the SpellRarity enum directly
        SpellRarity spellRarity = SpellConfigManager.getSpellConfigValue(spell, SpellConfigParameter.MIN_RARITY);
        if (spellRarity == null) {
            return;
        }
        if (spellRarity.compareRarity(effective) > 0) {
            event.setCanceled(true);
        }
    }

    // Higher-tier value means the gate is looser (allows more rarities through). Player cap raises the ceiling above whatever config says.
    private static SpellRarity looserOf(SpellRarity a, SpellRarity b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareRarity(b) >= 0 ? a : b;
    }

    private static SpellRarity getConfigThreshold() {
        String currentRaw = Config.MAX_SPELL_RARITY.get();
        if (currentRaw == cachedRawValue) {
            return cachedThreshold;
        }
        SpellRarity parsed = parseThreshold(currentRaw);
        cachedRawValue = currentRaw;
        cachedThreshold = parsed;
        return parsed;
    }

    private static SpellRarity parseThreshold(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SpellRarity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException invalid) {
            logger.warn("maxSpellRarity '{}' is not a valid rarity, gate disabled until corrected", raw);
            return null;
        }
    }

    // PlayerProgress stores the rarity name as a string to avoid pulling Iron's into the attachment layer, so we parse it here at the comparison site
    private static SpellRarity parseRarity(String name) {
        if (name == null) {
            return null;
        }
        try {
            return SpellRarity.valueOf(name);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
