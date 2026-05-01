// Immutable bundle of what a single unlock grants when it fires.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class UnlockGrants {

    public static final UnlockGrants EMPTY = new UnlockGrants(-1, 0.0, 0.0, Set.of(), Set.of());

    // -1 means no spell-level grant from this unlock
    private final int spellLevelCap;
    private final double cooldownReductionBonus;
    private final double castTimeReductionBonus;
    private final Set<ResourceLocation> dimensionsRemoved;
    private final Set<ResourceLocation> inscriptionsRemoved;

    public UnlockGrants(int spellLevelCap,
                        double cooldownReductionBonus,
                        double castTimeReductionBonus,
                        Set<ResourceLocation> dimensionsRemoved,
                        Set<ResourceLocation> inscriptionsRemoved) {
        this.spellLevelCap = spellLevelCap;
        this.cooldownReductionBonus = cooldownReductionBonus;
        this.castTimeReductionBonus = castTimeReductionBonus;
        this.dimensionsRemoved = Set.copyOf(dimensionsRemoved);
        this.inscriptionsRemoved = Set.copyOf(inscriptionsRemoved);
    }

    public int getSpellLevelCap() {
        return spellLevelCap;
    }

    public double getCooldownReductionBonus() {
        return cooldownReductionBonus;
    }

    public double getCastTimeReductionBonus() {
        return castTimeReductionBonus;
    }

    public Set<ResourceLocation> getDimensionsRemoved() {
        return dimensionsRemoved;
    }

    public Set<ResourceLocation> getInscriptionsRemoved() {
        return inscriptionsRemoved;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UnlockGrants)) {
            return false;
        }
        UnlockGrants that = (UnlockGrants) other;
        return spellLevelCap == that.spellLevelCap
                && Double.compare(cooldownReductionBonus, that.cooldownReductionBonus) == 0
                && Double.compare(castTimeReductionBonus, that.castTimeReductionBonus) == 0
                && dimensionsRemoved.equals(that.dimensionsRemoved)
                && inscriptionsRemoved.equals(that.inscriptionsRemoved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spellLevelCap, cooldownReductionBonus, castTimeReductionBonus, dimensionsRemoved, inscriptionsRemoved);
    }
}
