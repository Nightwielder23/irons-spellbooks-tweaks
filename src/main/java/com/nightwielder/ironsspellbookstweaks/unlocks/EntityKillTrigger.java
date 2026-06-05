// Fires an unlock when the player kills an entity matching the given type id. Useful for boss mods that don't include advancements.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import net.minecraft.resources.ResourceLocation;

public record EntityKillTrigger(ResourceLocation entityTypeId) implements UnlockTrigger {

    @Override
    public String type() {
        return "entity_kill";
    }
}
