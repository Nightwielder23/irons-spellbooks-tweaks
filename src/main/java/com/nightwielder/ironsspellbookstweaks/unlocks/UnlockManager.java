// Datapack reload listener for isstweaks/unlocks. Holds the parsed unlocks and a per-trigger lookup index.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnlockManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "isstweaks/unlocks";
    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/UnlockManager");

    // volatile + Map.copyOf gives lock-free atomic swap on reload, reads stay unsynchronized
    private static volatile Map<ResourceLocation, UnlockDefinition> unlocks = Map.of();
    private static volatile Map<ResourceLocation, List<UnlockDefinition>> byAdvancement = Map.of();

    public UnlockManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, UnlockDefinition> parsed = new HashMap<>();
        Map<ResourceLocation, List<UnlockDefinition>> advancementIndex = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();
            if (!element.isJsonObject()) {
                logger.warn("skipping unlock {}: top-level JSON is not an object", id);
                continue;
            }
            try {
                UnlockDefinition definition = UnlockJsonParser.parse(id, element.getAsJsonObject());
                parsed.put(id, definition);
                if (definition.getTrigger() instanceof AdvancementTrigger advancementTrigger) {
                    advancementIndex.computeIfAbsent(advancementTrigger.advancementId(), k -> new ArrayList<>()).add(definition);
                }
            } catch (JsonParseException parseFailed) {
                logger.warn("failed to parse unlock {}: {}", id, parseFailed.getMessage());
            }
        }
        Map<ResourceLocation, List<UnlockDefinition>> frozenIndex = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<UnlockDefinition>> indexed : advancementIndex.entrySet()) {
            frozenIndex.put(indexed.getKey(), List.copyOf(indexed.getValue()));
        }
        unlocks = Map.copyOf(parsed);
        byAdvancement = Map.copyOf(frozenIndex);
        logger.info("loaded {} unlocks ({} advancement-triggered)", unlocks.size(), byAdvancement.size());
    }

    public static Map<ResourceLocation, UnlockDefinition> getAll() {
        return unlocks;
    }

    public static List<UnlockDefinition> getByAdvancement(ResourceLocation advancementId) {
        return byAdvancement.getOrDefault(advancementId, List.of());
    }

    public static Optional<UnlockDefinition> getById(ResourceLocation unlockId) {
        return Optional.ofNullable(unlocks.get(unlockId));
    }
}
