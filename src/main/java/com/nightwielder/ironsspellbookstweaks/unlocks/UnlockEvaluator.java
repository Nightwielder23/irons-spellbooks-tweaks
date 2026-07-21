// Re-evaluates ungranted unlocks against a player's advancements and kill counts, applying any whose trigger is now satisfied.
// Keeps an index from advancement id and entity type id to the unlocks that reference them so a single earned advancement or kill only rescans the unlocks that could care.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class UnlockEvaluator {

    private static volatile Map<ResourceLocation, List<UnlockDefinition>> unlocksByAdvancement = Map.of();
    private static volatile Map<ResourceLocation, List<UnlockDefinition>> unlocksByEntityType = Map.of();

    private UnlockEvaluator() {
    }

    // Rebuilt by UnlockManager on every datapack reload so the index tracks the live unlock set.
    public static void rebuildIndex(Collection<UnlockDefinition> unlocks) {
        Map<ResourceLocation, List<UnlockDefinition>> byAdvancement = new HashMap<>();
        Map<ResourceLocation, List<UnlockDefinition>> byEntityType = new HashMap<>();
        for (UnlockDefinition unlock : unlocks) {
            Set<ResourceLocation> advancementIds = new HashSet<>();
            Set<ResourceLocation> entityTypeIds = new HashSet<>();
            collectReferences(unlock.getTrigger(), advancementIds, entityTypeIds);
            for (ResourceLocation advancementId : advancementIds) {
                byAdvancement.computeIfAbsent(advancementId, key -> new ArrayList<>()).add(unlock);
            }
            for (ResourceLocation entityTypeId : entityTypeIds) {
                byEntityType.computeIfAbsent(entityTypeId, key -> new ArrayList<>()).add(unlock);
            }
        }
        unlocksByAdvancement = Map.copyOf(byAdvancement);
        unlocksByEntityType = Map.copyOf(byEntityType);
    }

    public static boolean referencesEntityType(ResourceLocation entityTypeId) {
        return unlocksByEntityType.containsKey(entityTypeId);
    }

    public static void reevaluate(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(progress ->
                evaluate(serverPlayer, progress, UnlockManager.getAll().values()));
    }

    public static void reevaluate(Player player, ResourceLocation advancementId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        List<UnlockDefinition> candidates = unlocksByAdvancement.get(advancementId);
        if (candidates == null) {
            return;
        }
        serverPlayer.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(progress ->
                evaluate(serverPlayer, progress, candidates));
    }

    public static void reevaluate(ServerPlayer player, PlayerProgress progress, ResourceLocation entityTypeId) {
        List<UnlockDefinition> candidates = unlocksByEntityType.get(entityTypeId);
        if (candidates == null) {
            return;
        }
        evaluate(player, progress, candidates);
    }

    private static void evaluate(ServerPlayer player, PlayerProgress progress, Collection<UnlockDefinition> candidates) {
        for (UnlockDefinition unlock : candidates) {
            if (progress.hasUnlockGranted(unlock.getId())) {
                continue;
            }
            if (isSatisfied(unlock.getTrigger(), player, progress)) {
                UnlockApplicator.apply(player, unlock);
            }
        }
    }

    private static void collectReferences(UnlockTrigger trigger, Set<ResourceLocation> advancementIds, Set<ResourceLocation> entityTypeIds) {
        if (trigger instanceof AdvancementTrigger advancement) {
            advancementIds.add(advancement.advancementId());
        } else if (trigger instanceof EntityKillTrigger entityKill) {
            entityTypeIds.add(entityKill.entityTypeId());
        } else if (trigger instanceof EntityKillCountTrigger killCount) {
            entityTypeIds.add(killCount.entityTypeId());
        } else if (trigger instanceof AllOfTrigger allOf) {
            for (UnlockTrigger child : allOf.children()) {
                collectReferences(child, advancementIds, entityTypeIds);
            }
        } else if (trigger instanceof AnyOfTrigger anyOf) {
            for (UnlockTrigger child : anyOf.children()) {
                collectReferences(child, advancementIds, entityTypeIds);
            }
        }
    }

    // Entity types named by a trigger's leaves, walking composites. Advancement leaves contribute nothing.
    public static Set<ResourceLocation> referencedEntityTypes(UnlockTrigger trigger) {
        Set<ResourceLocation> entityTypeIds = new HashSet<>();
        collectReferences(trigger, new HashSet<>(), entityTypeIds);
        return entityTypeIds;
    }

    // True when some loaded unlock other than the excluded one still names this entity type.
    public static boolean entityReferencedByOtherUnlock(ResourceLocation entityTypeId, ResourceLocation excludeUnlockId) {
        for (UnlockDefinition unlock : UnlockManager.getAll().values()) {
            if (unlock.getId().equals(excludeUnlockId)) {
                continue;
            }
            if (referencedEntityTypes(unlock.getTrigger()).contains(entityTypeId)) {
                return true;
            }
        }
        return false;
    }

    // Entity type to the highest required-kill count across the unlocks this player has not been granted. entity_kill counts as one.
    public static Map<ResourceLocation, Integer> uncompletedEntityRequirements(PlayerProgress progress) {
        Map<ResourceLocation, Integer> requirements = new HashMap<>();
        for (UnlockDefinition unlock : UnlockManager.getAll().values()) {
            if (progress.hasUnlockGranted(unlock.getId())) {
                continue;
            }
            collectEntityRequirements(unlock.getTrigger(), requirements);
        }
        return requirements;
    }

    private static void collectEntityRequirements(UnlockTrigger trigger, Map<ResourceLocation, Integer> requirements) {
        if (trigger instanceof EntityKillTrigger entityKill) {
            requirements.merge(entityKill.entityTypeId(), 1, Math::max);
        } else if (trigger instanceof EntityKillCountTrigger killCount) {
            requirements.merge(killCount.entityTypeId(), killCount.requiredCount(), Math::max);
        } else if (trigger instanceof AllOfTrigger allOf) {
            for (UnlockTrigger child : allOf.children()) {
                collectEntityRequirements(child, requirements);
            }
        } else if (trigger instanceof AnyOfTrigger anyOf) {
            for (UnlockTrigger child : anyOf.children()) {
                collectEntityRequirements(child, requirements);
            }
        }
    }

    public static boolean isSatisfied(UnlockTrigger trigger, ServerPlayer player, PlayerProgress progress) {
        if (trigger instanceof AdvancementTrigger advancement) {
            return hasAdvancement(player, advancement.advancementId());
        }
        if (trigger instanceof EntityKillTrigger entityKill) {
            return progress.getKillCount(entityKill.entityTypeId()) >= 1;
        }
        if (trigger instanceof EntityKillCountTrigger killCount) {
            return progress.getKillCount(killCount.entityTypeId()) >= killCount.requiredCount();
        }
        if (trigger instanceof AllOfTrigger allOf) {
            for (UnlockTrigger child : allOf.children()) {
                if (!isSatisfied(child, player, progress)) {
                    return false;
                }
            }
            return true;
        }
        if (trigger instanceof AnyOfTrigger anyOf) {
            for (UnlockTrigger child : anyOf.children()) {
                if (isSatisfied(child, player, progress)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return false;
        }
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
