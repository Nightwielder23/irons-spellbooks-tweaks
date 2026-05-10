// Fires an unlock when the named advancement is earned.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import net.minecraft.resources.ResourceLocation;

public record AdvancementTrigger(ResourceLocation advancementId) implements UnlockTrigger {

    @Override
    public String type() {
        return "advancement";
    }
}
