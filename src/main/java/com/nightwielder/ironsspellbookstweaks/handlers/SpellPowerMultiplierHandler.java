// Scales every player's SPELL_POWER attribute by the configured multiplier on login and respawn.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpellPowerMultiplierHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/SpellPowerMultiplierHandler");

    // Use a fixed id so relogin replaces the modifier instead of stacking another.
    private static final UUID SPELL_POWER_MODIFIER_ID = UUID.fromString("f3a7c1d9-2e84-4b16-9c5a-7d0e2f6b8a14");

    private static boolean warnedMissingAttribute;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }

    private static void apply(Player player) {
        Optional<Attribute> spellPowerAttribute = IronsSpellbooksCompat.getSpellPowerAttribute();
        if (spellPowerAttribute.isEmpty()) {
            if (!warnedMissingAttribute) {
                logger.warn("SPELL_POWER attribute not registered, so spellPowerMultiplier is skipped");
                warnedMissingAttribute = true;
            }
            return;
        }
        AttributeInstance instance = player.getAttribute(spellPowerAttribute.get());
        if (instance == null) {
            return;
        }
        // strip prior copy so config edits take on relogin; removeModifier is null-safe
        instance.removeModifier(SPELL_POWER_MODIFIER_ID);
        double multiplier = RuntimeConfig.spellPowerMultiplier;
        if (multiplier == 1.0) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(SPELL_POWER_MODIFIER_ID, "isstweaks_spell_power_multiplier", multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE);
        instance.addPermanentModifier(modifier);
    }
}
