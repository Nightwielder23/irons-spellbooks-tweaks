// blocks player casts whose min rarity is over the ceiling (looser of the config gate and the player's own cap). mobs aren't affected since SpellPreCastEvent is player-only.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpellRarityGateHandler {

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        SpellRarity configThreshold = parseRarity(RuntimeConfig.maxSpellRarity);
        Player player = event.getEntity();
        // resolve() converts LazyOptional to plain Optional so a null mapped value does not blow up Optional.of inside LazyOptional.map
        String playerCapName = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .resolve()
                .map(PlayerProgress::getRarityCap)
                .orElse(null);
        SpellRarity playerCap = parseRarity(playerCapName);
        SpellRarity effective = looserOf(configThreshold, playerCap);
        if (effective == null) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) {
            return;
        }
        // AbstractSpell.getMinRarity is deprecated for removal, so go through SpellConfigManager directly
        SpellRarity spellRarity = SpellConfigManager.getSpellConfigValue(spell, SpellConfigParameter.MIN_RARITY);
        if (spellRarity == null) {
            return;
        }
        if (spellRarity.compareRarity(effective) > 0) {
            event.setCanceled(true);
        }
    }

    // higher tier = looser gate. player cap can only raise the ceiling, never lower it.
    private static SpellRarity looserOf(SpellRarity a, SpellRarity b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareRarity(b) >= 0 ? a : b;
    }

    // cap is stored as a string to keep Iron's out of the capability layer. parse to enum here.
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
