package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Adjusts base values of Iron's MANA_REGEN, MAX_MANA, and COOLDOWN_REDUCTION attributes on the player entity type. All three share the same event so they live in one handler.
public class ManaAttributeHandler {

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        // TODO: apply baseManaRegenPercent (or convert flat to percent), cooldownReductionBonus, and starting MAX_MANA defaults to EntityType.PLAYER
    }
}
