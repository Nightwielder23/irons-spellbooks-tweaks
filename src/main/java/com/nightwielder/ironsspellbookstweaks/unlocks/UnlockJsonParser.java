// Hand-rolled Gson parser for unlock JSON files. Codec felt heavyweight for a schema this small.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UnlockJsonParser {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/UnlockJsonParser");

    private UnlockJsonParser() {
    }

    public static UnlockDefinition parse(ResourceLocation id, JsonObject json) {
        UnlockTrigger trigger = parseTrigger(id, json);
        UnlockGrants grants = parseGrants(id, json);
        Optional<String> message = parseMessage(id, json);
        String requirementText = parseRequirementText(id, json);
        return new UnlockDefinition(id, trigger, grants, message, requirementText);
    }

    private static UnlockTrigger parseTrigger(ResourceLocation unlockId, JsonObject json) {
        if (!json.has("trigger")) {
            throw new JsonParseException("unlock " + unlockId + " is missing required 'trigger' field");
        }
        JsonElement triggerElement = json.get("trigger");
        if (!triggerElement.isJsonObject()) {
            throw new JsonParseException("unlock " + unlockId + " 'trigger' must be a JSON object");
        }
        JsonObject triggerJson = triggerElement.getAsJsonObject();
        if (!triggerJson.has("type") || !triggerJson.get("type").isJsonPrimitive()) {
            throw new JsonParseException("unlock " + unlockId + " trigger missing required string 'type'");
        }
        String triggerType = triggerJson.get("type").getAsString();
        if ("advancement".equals(triggerType)) {
            return parseAdvancementTrigger(unlockId, triggerJson);
        }
        if ("entity_kill".equals(triggerType)) {
            return parseEntityKillTrigger(unlockId, triggerJson);
        }
        throw new JsonParseException("unlock " + unlockId + " has unknown trigger type '" + triggerType + "'");
    }

    private static AdvancementTrigger parseAdvancementTrigger(ResourceLocation unlockId, JsonObject triggerJson) {
        if (!triggerJson.has("id") || !triggerJson.get("id").isJsonPrimitive()) {
            throw new JsonParseException("unlock " + unlockId + " advancement trigger missing required string 'id'");
        }
        String rawId = triggerJson.get("id").getAsString();
        ResourceLocation advancementId = ResourceLocation.tryParse(rawId);
        if (advancementId == null) {
            throw new JsonParseException("unlock " + unlockId + " advancement trigger has invalid id '" + rawId + "'");
        }
        return new AdvancementTrigger(advancementId);
    }

    private static EntityKillTrigger parseEntityKillTrigger(ResourceLocation unlockId, JsonObject triggerJson) {
        if (!triggerJson.has("id") || !triggerJson.get("id").isJsonPrimitive()) {
            throw new JsonParseException("unlock " + unlockId + " entity_kill trigger missing required string 'id'");
        }
        String rawId = triggerJson.get("id").getAsString();
        ResourceLocation entityTypeId = ResourceLocation.tryParse(rawId);
        if (entityTypeId == null) {
            throw new JsonParseException("unlock " + unlockId + " entity_kill trigger has invalid entity type id '" + rawId + "'");
        }
        return new EntityKillTrigger(entityTypeId);
    }

    private static UnlockGrants parseGrants(ResourceLocation unlockId, JsonObject json) {
        if (!json.has("grants")) {
            logger.debug("unlock {} has no grants block, treating as empty", unlockId);
            return UnlockGrants.EMPTY;
        }
        JsonElement grantsElement = json.get("grants");
        if (!grantsElement.isJsonObject()) {
            throw new JsonParseException("unlock " + unlockId + " 'grants' must be a JSON object");
        }
        JsonObject grantsJson = grantsElement.getAsJsonObject();

        if (grantsJson.has("spell_level_cap")) {
            logger.warn("unlock {} uses 'spell_level_cap' grant, which is no longer supported (replaced by 'rarity_cap'). Ignoring.", unlockId);
        }
        double cooldownReductionBonus = grantsJson.has("cooldown_reduction_bonus")
                ? grantsJson.get("cooldown_reduction_bonus").getAsDouble()
                : 0.0;
        double castTimeReductionBonus = grantsJson.has("cast_time_reduction_bonus")
                ? grantsJson.get("cast_time_reduction_bonus").getAsDouble()
                : 0.0;
        int maxManaBonus = grantsJson.has("max_mana_bonus")
                ? grantsJson.get("max_mana_bonus").getAsInt()
                : 0;
        double manaRegenBonus = grantsJson.has("mana_regen_bonus")
                ? grantsJson.get("mana_regen_bonus").getAsDouble()
                : 0.0;
        Set<ResourceLocation> dimensionsRemoved = parseIdList(unlockId, grantsJson, "remove_dimensions");
        Set<ResourceLocation> inscriptionsRemoved = parseIdList(unlockId, grantsJson, "remove_inscriptions");
        String rarityCap = parseRarityCap(unlockId, grantsJson);

        return new UnlockGrants(cooldownReductionBonus, castTimeReductionBonus, maxManaBonus, manaRegenBonus, dimensionsRemoved, inscriptionsRemoved, rarityCap);
    }

    private static String parseRarityCap(ResourceLocation unlockId, JsonObject grantsJson) {
        if (!grantsJson.has("rarity_cap")) {
            return null;
        }
        JsonElement element = grantsJson.get("rarity_cap");
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            logger.warn("unlock {} 'rarity_cap' is not a string, ignoring", unlockId);
            return null;
        }
        String raw = element.getAsString();
        String upper = raw.trim().toUpperCase();
        if (!PlayerProgress.RARITY_RANKS.containsKey(upper)) {
            logger.warn("unlock {} 'rarity_cap' value '{}' is not a valid rarity, skipping", unlockId, raw);
            return null;
        }
        return upper;
    }

    private static Set<ResourceLocation> parseIdList(ResourceLocation unlockId, JsonObject grantsJson, String key) {
        if (!grantsJson.has(key)) {
            return Set.of();
        }
        JsonElement element = grantsJson.get(key);
        if (!element.isJsonArray()) {
            logger.warn("unlock {} '{}' is not an array, ignoring", unlockId, key);
            return Set.of();
        }
        JsonArray array = element.getAsJsonArray();
        Set<ResourceLocation> collected = new HashSet<>();
        for (JsonElement entry : array) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                logger.warn("unlock {} '{}' contains non-string entry, skipping", unlockId, key);
                continue;
            }
            String rawId = entry.getAsString();
            ResourceLocation parsed = ResourceLocation.tryParse(rawId);
            if (parsed == null) {
                logger.warn("unlock {} '{}' contains invalid resource location '{}', skipping", unlockId, key, rawId);
                continue;
            }
            collected.add(parsed);
        }
        return Set.copyOf(collected);
    }

    private static Optional<String> parseMessage(ResourceLocation unlockId, JsonObject json) {
        if (!json.has("message")) {
            return Optional.empty();
        }
        JsonElement messageElement = json.get("message");
        if (!messageElement.isJsonPrimitive() || !messageElement.getAsJsonPrimitive().isString()) {
            logger.warn("unlock {} 'message' is not a string, ignoring", unlockId);
            return Optional.empty();
        }
        return Optional.of(messageElement.getAsString());
    }

    private static String parseRequirementText(ResourceLocation unlockId, JsonObject json) {
        if (!json.has("requirement_text")) {
            return null;
        }
        JsonElement element = json.get("requirement_text");
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            logger.warn("unlock {} 'requirement_text' is not a string, ignoring", unlockId);
            return null;
        }
        return element.getAsString();
    }
}
