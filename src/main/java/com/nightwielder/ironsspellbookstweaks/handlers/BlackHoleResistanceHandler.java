// Counters Iron's BlackHole pull on configured mobs. Fully-immune entries anchor the victim to a locked position; partial entries scale deltaMovement. We track black holes via entity-join/leave events so we don't scan every level every tick.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BlackHoleResistanceHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/BlackHoleResistanceHandler");

    // server-thread-only access during entity events and server tick, so a plain HashSet is enough
    private static final Set<BlackHole> activeBlackHoles = new HashSet<>();

    // identity-compared cache so we rebuild only when Forge swaps the underlying list on config reload
    private static List<? extends String> cachedRawList;
    private static Map<ResourceLocation, Double> cachedImmunityMap = Map.of();

    // Fully-immune victims get anchored on first sight and teleported back each tick. Motion-scaling alone fails because Iron's pushes during entity tick and vanilla integrates before our ServerTickEvent.END runs.
    private static final Map<UUID, Vec3> anchoredVictims = new HashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof BlackHole blackHole) {
            activeBlackHoles.add(blackHole);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof BlackHole blackHole) {
            activeBlackHoles.remove(blackHole);
            if (activeBlackHoles.isEmpty()) {
                anchoredVictims.clear();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (activeBlackHoles.isEmpty()) {
            return;
        }
        Map<ResourceLocation, Double> immunityMap = getImmunityMap();
        if (immunityMap.isEmpty()) {
            return;
        }
        Set<UUID> currentTickAnchored = new HashSet<>();
        Iterator<BlackHole> iterator = activeBlackHoles.iterator();
        while (iterator.hasNext()) {
            BlackHole blackHole = iterator.next();
            // EntityLeaveLevelEvent should clear these, but defend against the rare edge where it's missed
            if (blackHole.isRemoved()) {
                iterator.remove();
                continue;
            }
            scaleVictimsForBlackHole(blackHole, immunityMap, currentTickAnchored);
        }
        // drop anchors for victims that left every black hole's range this tick
        anchoredVictims.keySet().retainAll(currentTickAnchored);
    }

    private static void scaleVictimsForBlackHole(BlackHole blackHole, Map<ResourceLocation, Double> immunityMap, Set<UUID> currentTickAnchored) {
        Level level = blackHole.level();
        float radius = blackHole.getRadius();
        AABB scanArea = AABB.ofSize(blackHole.position(), radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> potentialVictims = level.getEntitiesOfClass(LivingEntity.class, scanArea);
        for (LivingEntity victim : potentialVictims) {
            ResourceLocation victimType = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
            if (victimType == null) {
                continue;
            }
            Double strength = immunityMap.get(victimType);
            if (strength == null) {
                continue;
            }
            if (strength >= 1.0) {
                anchorVictim(victim, currentTickAnchored);
            } else {
                double scale = Math.max(0.0, 1.0 - strength);
                Vec3 motion = victim.getDeltaMovement();
                victim.setDeltaMovement(motion.scale(scale));
                // hurtMarked forces the velocity change to flush to clients on the next packet
                victim.hurtMarked = true;
            }
        }
    }

    private static void anchorVictim(LivingEntity victim, Set<UUID> currentTickAnchored) {
        UUID victimId = victim.getUUID();
        Vec3 anchor = anchoredVictims.get(victimId);
        if (anchor == null) {
            // lock to current position; the victim has already been displaced one tick by Iron's push, but subsequent ticks pin them here
            anchor = victim.position();
            anchoredVictims.put(victimId, anchor);
        }
        victim.teleportTo(anchor.x, anchor.y, anchor.z);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.hurtMarked = true;
        currentTickAnchored.add(victimId);
    }

    private static Map<ResourceLocation, Double> getImmunityMap() {
        List<? extends String> currentRaw = Config.BLACKHOLE_IMMUNITY.get();
        if (currentRaw == cachedRawList) {
            return cachedImmunityMap;
        }
        Map<ResourceLocation, Double> rebuilt = parseImmunityList(currentRaw);
        cachedRawList = currentRaw;
        cachedImmunityMap = rebuilt;
        return rebuilt;
    }

    private static Map<ResourceLocation, Double> parseImmunityList(List<? extends String> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Double> result = new HashMap<>();
        for (String entry : raw) {
            // entity ids contain one colon, so the strength is everything after the LAST colon
            int separator = entry.lastIndexOf(':');
            if (separator < 0) {
                logger.warn("blackhole immunity entry '{}' missing strength suffix, skipping", entry);
                continue;
            }
            String idPart = entry.substring(0, separator);
            String strengthPart = entry.substring(separator + 1);
            ResourceLocation entityId = ResourceLocation.tryParse(idPart);
            if (entityId == null) {
                logger.warn("blackhole immunity entry '{}' has invalid entity id '{}', skipping", entry, idPart);
                continue;
            }
            double strength;
            try {
                strength = Double.parseDouble(strengthPart);
            } catch (NumberFormatException numberFailed) {
                logger.warn("blackhole immunity entry '{}' has non-numeric strength '{}', skipping", entry, strengthPart);
                continue;
            }
            double clamped = Math.max(0.0, Math.min(1.0, strength));
            result.put(entityId, clamped);
        }
        return Map.copyOf(result);
    }
}
