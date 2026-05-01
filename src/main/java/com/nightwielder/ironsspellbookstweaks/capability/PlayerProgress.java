// Per-player progression state. Persisted via PlayerProgressProvider and cloned on respawn.
package com.nightwielder.ironsspellbookstweaks.capability;

import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class PlayerProgress {

    private static final String KEY_COOLDOWN_BONUS = "cooldown_reduction_bonus";
    private static final String KEY_CAST_TIME_BONUS = "cast_time_reduction_bonus";
    private static final String KEY_DIMENSIONS_REMOVED = "dimensions_removed";
    private static final String KEY_INSCRIPTIONS_REMOVED = "inscriptions_removed";
    private static final String KEY_GRANTED_UNLOCKS = "granted_unlocks";
    private static final String KEY_RARITY_CAP = "rarity_cap";

    private double cooldownReductionBonus = 0.0;
    private double castTimeReductionBonus = 0.0;
    private final Set<ResourceLocation> dimensionsRemoved = new HashSet<>();
    private final Set<ResourceLocation> inscriptionsRemoved = new HashSet<>();
    private final Set<ResourceLocation> grantedUnlocks = new HashSet<>();
    // null means no per-player rarity gate, callers fall back to config
    private SpellRarity rarityCap = null;

    public double getCooldownReductionBonus() {
        return cooldownReductionBonus;
    }

    public double getCastTimeReductionBonus() {
        return castTimeReductionBonus;
    }

    public Set<ResourceLocation> getDimensionsRemoved() {
        return Collections.unmodifiableSet(dimensionsRemoved);
    }

    public Set<ResourceLocation> getInscriptionsRemoved() {
        return Collections.unmodifiableSet(inscriptionsRemoved);
    }

    public Set<ResourceLocation> getGrantedUnlocks() {
        return Collections.unmodifiableSet(grantedUnlocks);
    }

    public SpellRarity getRarityCap() {
        return rarityCap;
    }

    public void addCooldownBonus(double amount) {
        cooldownReductionBonus += amount;
    }

    public void addCastTimeBonus(double amount) {
        castTimeReductionBonus += amount;
    }

    public void addDimensionRemoved(ResourceLocation dimension) {
        dimensionsRemoved.add(dimension);
    }

    public void addInscriptionRemoved(ResourceLocation spellId) {
        inscriptionsRemoved.add(spellId);
    }

    // raise-only so a later unlock with a stricter cap can't tighten an earlier loosening
    public void raiseRarityCap(SpellRarity newCap) {
        if (newCap == null) {
            return;
        }
        if (rarityCap == null || newCap.compareRarity(rarityCap) > 0) {
            rarityCap = newCap;
        }
    }

    public boolean markUnlockGranted(ResourceLocation unlockId) {
        return grantedUnlocks.add(unlockId);
    }

    // only clears the granted-set membership, cumulative bonuses stay applied since we can't trace which value came from which unlock
    public boolean removeUnlockGranted(ResourceLocation unlockId) {
        return grantedUnlocks.remove(unlockId);
    }

    public boolean hasUnlockGranted(ResourceLocation unlockId) {
        return grantedUnlocks.contains(unlockId);
    }

    public void copyFrom(PlayerProgress other) {
        this.cooldownReductionBonus = other.cooldownReductionBonus;
        this.castTimeReductionBonus = other.castTimeReductionBonus;
        this.dimensionsRemoved.clear();
        this.dimensionsRemoved.addAll(other.dimensionsRemoved);
        this.inscriptionsRemoved.clear();
        this.inscriptionsRemoved.addAll(other.inscriptionsRemoved);
        this.grantedUnlocks.clear();
        this.grantedUnlocks.addAll(other.grantedUnlocks);
        this.rarityCap = other.rarityCap;
    }

    public void reset() {
        cooldownReductionBonus = 0.0;
        castTimeReductionBonus = 0.0;
        dimensionsRemoved.clear();
        inscriptionsRemoved.clear();
        grantedUnlocks.clear();
        rarityCap = null;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(KEY_COOLDOWN_BONUS, cooldownReductionBonus);
        tag.putDouble(KEY_CAST_TIME_BONUS, castTimeReductionBonus);
        tag.put(KEY_DIMENSIONS_REMOVED, writeIdSet(dimensionsRemoved));
        tag.put(KEY_INSCRIPTIONS_REMOVED, writeIdSet(inscriptionsRemoved));
        tag.put(KEY_GRANTED_UNLOCKS, writeIdSet(grantedUnlocks));
        if (rarityCap != null) {
            tag.putString(KEY_RARITY_CAP, rarityCap.name());
        }
        return tag;
    }

    // missing keys fall through to defaults so saves from earlier versions still load
    public void deserializeNBT(CompoundTag tag) {
        cooldownReductionBonus = tag.contains(KEY_COOLDOWN_BONUS, Tag.TAG_DOUBLE) ? tag.getDouble(KEY_COOLDOWN_BONUS) : 0.0;
        castTimeReductionBonus = tag.contains(KEY_CAST_TIME_BONUS, Tag.TAG_DOUBLE) ? tag.getDouble(KEY_CAST_TIME_BONUS) : 0.0;
        readIdSet(tag, KEY_DIMENSIONS_REMOVED, dimensionsRemoved);
        readIdSet(tag, KEY_INSCRIPTIONS_REMOVED, inscriptionsRemoved);
        readIdSet(tag, KEY_GRANTED_UNLOCKS, grantedUnlocks);
        rarityCap = readRarityCap(tag);
    }

    // Iron's-loaded check keeps SpellRarity.valueOf off the path when the mod is missing, so a save written with Iron's then reopened without it doesn't crash.
    private static SpellRarity readRarityCap(CompoundTag tag) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return null;
        }
        if (!tag.contains(KEY_RARITY_CAP, Tag.TAG_STRING)) {
            return null;
        }
        try {
            return SpellRarity.valueOf(tag.getString(KEY_RARITY_CAP));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static ListTag writeIdSet(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static void readIdSet(CompoundTag tag, String key, Set<ResourceLocation> target) {
        target.clear();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation parsed = ResourceLocation.tryParse(list.getString(i));
            if (parsed != null) {
                target.add(parsed);
            }
        }
    }
}
