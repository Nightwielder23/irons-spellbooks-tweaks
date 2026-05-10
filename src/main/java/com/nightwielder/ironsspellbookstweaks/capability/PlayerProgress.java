// Per-player progression state. Persisted via PlayerProgressProvider and cloned on respawn.
package com.nightwielder.ironsspellbookstweaks.capability;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class PlayerProgress {

    private static final String KEY_COOLDOWN_BONUS = "cooldown_reduction_bonus";
    private static final String KEY_CAST_TIME_BONUS = "cast_time_reduction_bonus";
    private static final String KEY_MAX_MANA_BONUS = "max_mana_bonus";
    private static final String KEY_MANA_REGEN_BONUS = "mana_regen_bonus";
    private static final String KEY_DIMENSIONS_REMOVED = "dimensions_removed";
    private static final String KEY_INSCRIPTIONS_REMOVED = "inscriptions_removed";
    private static final String KEY_GRANTED_UNLOCKS = "granted_unlocks";
    private static final String KEY_RARITY_CAP = "rarity_cap";

    // Hardcoded rank order matches Iron's SpellRarity enum so this class can do raise-only comparisons without importing Iron's. Keeping PlayerProgress free of Iron's class refs is what lets the capability layer load when the soft-dep is missing.
    public static final Map<String, Integer> RARITY_RANKS = Map.of(
            "COMMON", 0,
            "UNCOMMON", 1,
            "RARE", 2,
            "EPIC", 3,
            "LEGENDARY", 4
    );

    private double cooldownReductionBonus = 0.0;
    private double castTimeReductionBonus = 0.0;
    private int maxManaBonus = 0;
    private double manaRegenBonus = 0.0;
    private final Set<ResourceLocation> dimensionsRemoved = new HashSet<>();
    private final Set<ResourceLocation> inscriptionsRemoved = new HashSet<>();
    private final Set<ResourceLocation> grantedUnlocks = new HashSet<>();
    // null means no per-player rarity gate, callers fall back to config. Stored as the rarity name (uppercase) so this class never references SpellRarity directly.
    private String rarityCap = null;

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
        return Collections.unmodifiableSet(dimensionsRemoved);
    }

    public Set<ResourceLocation> getInscriptionsRemoved() {
        return Collections.unmodifiableSet(inscriptionsRemoved);
    }

    public Set<ResourceLocation> getGrantedUnlocks() {
        return Collections.unmodifiableSet(grantedUnlocks);
    }

    public String getRarityCap() {
        return rarityCap;
    }

    public void addCooldownBonus(double amount) {
        cooldownReductionBonus += amount;
    }

    public void addCastTimeBonus(double amount) {
        castTimeReductionBonus += amount;
    }

    public void addMaxManaBonus(int amount) {
        maxManaBonus += amount;
    }

    public void addManaRegenBonus(double amount) {
        manaRegenBonus += amount;
    }

    public void addDimensionRemoved(ResourceLocation dimension) {
        dimensionsRemoved.add(dimension);
    }

    public void addInscriptionRemoved(ResourceLocation spellId) {
        inscriptionsRemoved.add(spellId);
    }

    // raise-only so a later unlock with a stricter cap can't tighten an earlier loosening
    public void raiseRarityCap(String newCap) {
        if (newCap == null) {
            return;
        }
        Integer newRank = RARITY_RANKS.get(newCap);
        if (newRank == null) {
            return;
        }
        Integer currentRank = rarityCap == null ? null : RARITY_RANKS.get(rarityCap);
        if (currentRank == null || newRank > currentRank) {
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
        this.maxManaBonus = other.maxManaBonus;
        this.manaRegenBonus = other.manaRegenBonus;
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
        maxManaBonus = 0;
        manaRegenBonus = 0.0;
        dimensionsRemoved.clear();
        inscriptionsRemoved.clear();
        grantedUnlocks.clear();
        rarityCap = null;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(KEY_COOLDOWN_BONUS, cooldownReductionBonus);
        tag.putDouble(KEY_CAST_TIME_BONUS, castTimeReductionBonus);
        tag.putInt(KEY_MAX_MANA_BONUS, maxManaBonus);
        tag.putDouble(KEY_MANA_REGEN_BONUS, manaRegenBonus);
        tag.put(KEY_DIMENSIONS_REMOVED, writeIdSet(dimensionsRemoved));
        tag.put(KEY_INSCRIPTIONS_REMOVED, writeIdSet(inscriptionsRemoved));
        tag.put(KEY_GRANTED_UNLOCKS, writeIdSet(grantedUnlocks));
        if (rarityCap != null) {
            tag.putString(KEY_RARITY_CAP, rarityCap);
        }
        return tag;
    }

    // missing keys fall through to defaults so saves from earlier versions still load
    public void deserializeNBT(CompoundTag tag) {
        cooldownReductionBonus = tag.contains(KEY_COOLDOWN_BONUS, Tag.TAG_DOUBLE) ? tag.getDouble(KEY_COOLDOWN_BONUS) : 0.0;
        castTimeReductionBonus = tag.contains(KEY_CAST_TIME_BONUS, Tag.TAG_DOUBLE) ? tag.getDouble(KEY_CAST_TIME_BONUS) : 0.0;
        maxManaBonus = tag.contains(KEY_MAX_MANA_BONUS, Tag.TAG_INT) ? tag.getInt(KEY_MAX_MANA_BONUS) : 0;
        manaRegenBonus = tag.contains(KEY_MANA_REGEN_BONUS, Tag.TAG_DOUBLE) ? tag.getDouble(KEY_MANA_REGEN_BONUS) : 0.0;
        readIdSet(tag, KEY_DIMENSIONS_REMOVED, dimensionsRemoved);
        readIdSet(tag, KEY_INSCRIPTIONS_REMOVED, inscriptionsRemoved);
        readIdSet(tag, KEY_GRANTED_UNLOCKS, grantedUnlocks);
        rarityCap = readRarityCap(tag);
    }

    // unknown rarity names get dropped so a save from a future Iron's version with extra rarities doesn't poison our state
    private static String readRarityCap(CompoundTag tag) {
        if (!tag.contains(KEY_RARITY_CAP, Tag.TAG_STRING)) {
            return null;
        }
        String stored = tag.getString(KEY_RARITY_CAP);
        return RARITY_RANKS.containsKey(stored) ? stored : null;
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
