// Datapack reload listener for isstweaks/unlocks. Holds the parsed unlocks.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnlockManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "isstweaks/unlocks";
    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/UnlockManager");

    // volatile + Map.copyOf gives lock-free atomic swap on reload; reads stay unsynchronized
    private static volatile Map<ResourceLocation, UnlockDefinition> unlocks = Map.of();

    public UnlockManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, UnlockDefinition> parsed = new HashMap<>();
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
            } catch (JsonParseException parseFailed) {
                logger.warn("failed to parse unlock {}: {}", id, parseFailed.getMessage());
            } catch (RuntimeException unexpected) {
                // gson throws NumberFormatException or UnsupportedOperationException on wrong-type values, and neither is a JsonParseException. without this catch one bad file kills the whole reload.
                logger.warn("failed to parse unlock {}: {}", id, unexpected.toString(), unexpected);
            }
        }
        unlocks = Map.copyOf(parsed);
        UnlockEvaluator.rebuildIndex(unlocks.values());
        // a /reload can add or loosen an unlock a logged-in player already qualifies for, so re-check everyone online instead of waiting for their next login
        reevaluateOnlinePlayers();
        logger.info("loaded {} unlocks", unlocks.size());
    }

    private static void reevaluateOnlinePlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UnlockEvaluator.reevaluate(player);
        }
    }

    public static Map<ResourceLocation, UnlockDefinition> getAll() {
        return unlocks;
    }

    public static Optional<UnlockDefinition> getById(ResourceLocation unlockId) {
        return Optional.ofNullable(unlocks.get(unlockId));
    }

    public static Optional<UnlockDefinition> findUnlockForInscription(ResourceLocation spellId) {
        for (UnlockDefinition definition : unlocks.values()) {
            if (definition.getGrants().getInscriptionsRemoved().contains(spellId)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }
}
