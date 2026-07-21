// Composite trigger that fires when any child trigger is satisfied.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.List;

public record AnyOfTrigger(List<UnlockTrigger> children) implements UnlockTrigger {

    @Override
    public String type() {
        return "any_of";
    }
}
