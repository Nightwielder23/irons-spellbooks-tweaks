// Composite trigger that fires when every child trigger is satisfied.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.List;

public record AllOfTrigger(List<UnlockTrigger> children) implements UnlockTrigger {

    @Override
    public String type() {
        return "all_of";
    }
}
