// Immutable bundle of what a single unlock grants when it fires.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class UnlockGrants {

    public static final UnlockGrants EMPTY = new UnlockGrants(0.0, 0.0, 0, 0.0, Set.of(), Set.of(), null);

    private final double cooldownReductionBonus;
    private final double castTimeReductionBonus;
    private final int maxManaBonus;
    private final double manaRegenBonus;
    private final Set<ResourceLocation> dimensionsRemoved;
    private final Set<ResourceLocation> inscriptionsRemoved;
    // null means this unlock doesn't touch the player's rarity cap. Stored as the rarity name (uppercase) so this class never references SpellRarity directly.
    private final String rarityCap;

    public UnlockGrants(double cooldownReductionBonus,
                        double castTimeReductionBonus,
                        int maxManaBonus,
                        double manaRegenBonus,
                        Set<ResourceLocation> dimensionsRemoved,
                        Set<ResourceLocation> inscriptionsRemoved,
                        String rarityCap) {
        this.cooldownReductionBonus = cooldownReductionBonus;
        this.castTimeReductionBonus = castTimeReductionBonus;
        this.maxManaBonus = maxManaBonus;
        this.manaRegenBonus = manaRegenBonus;
        this.dimensionsRemoved = Set.copyOf(dimensionsRemoved);
        this.inscriptionsRemoved = Set.copyOf(inscriptionsRemoved);
        this.rarityCap = rarityCap;
    }

    public double getCooldownReductionBonus() {
        return cooldownReductionBonus;
    }

    public double getCastTimeReductionBonus() {
        return castTimeReductionBonus;
    }

    public int getMaxManaBonus() {
        return maxManaBonus;
    }

    public double getManaRegenBonus() {
        return manaRegenBonus;
    }

    public Set<ResourceLocation> getDimensionsRemoved() {
        return dimensionsRemoved;
    }

    public Set<ResourceLocation> getInscriptionsRemoved() {
        return inscriptionsRemoved;
    }

    public String getRarityCap() {
        return rarityCap;
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
        return Double.compare(cooldownReductionBonus, that.cooldownReductionBonus) == 0
                && Double.compare(castTimeReductionBonus, that.castTimeReductionBonus) == 0
                && Integer.compare(maxManaBonus, that.maxManaBonus) == 0
                && Double.compare(manaRegenBonus, that.manaRegenBonus) == 0
                && dimensionsRemoved.equals(that.dimensionsRemoved)
                && inscriptionsRemoved.equals(that.inscriptionsRemoved)
                && Objects.equals(rarityCap, that.rarityCap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cooldownReductionBonus, castTimeReductionBonus, maxManaBonus, manaRegenBonus, dimensionsRemoved, inscriptionsRemoved, rarityCap);
    }
}
