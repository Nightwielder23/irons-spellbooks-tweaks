// Fires once the player has killed the given entity type at least requiredCount times.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import net.minecraft.resources.ResourceLocation;

public record EntityKillCountTrigger(ResourceLocation entityTypeId, int requiredCount) implements UnlockTrigger {

    @Override
    public String type() {
        return "entity_kill_count";
    }
}
