// Counters Iron's black hole pull on configured mobs. Full immunity pins the victim in place; partial scaling reduces deltaMovement. Follow black holes via entity join and leave events to avoid scanning every level each tick.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class BlackHoleResistanceHandler {

    // Accessed only on the server thread during entity events and server tick, so a plain HashSet is enough
    private static final Set<BlackHole> activeBlackHoles = new HashSet<>();

    // Fully-immune victims get anchored on first sight and teleported back each tick. Motion-scaling alone fails because Iron's pushes during entity tick and vanilla integrates before this handler's ServerTickEvent.Post runs.
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
    public static void onServerTick(ServerTickEvent.Post event) {
        if (activeBlackHoles.isEmpty()) {
            return;
        }
        Map<ResourceLocation, Double> immunityMap = RuntimeConfig.blackholeImmunityMap;
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

    // Clear retained state on shutdown so black holes from a closed world don't linger into the next world loaded
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        activeBlackHoles.clear();
        anchoredVictims.clear();
    }

    private static void scaleVictimsForBlackHole(BlackHole blackHole, Map<ResourceLocation, Double> immunityMap, Set<UUID> currentTickAnchored) {
        Level level = blackHole.level();
        float radius = blackHole.getRadius();
        AABB scanArea = AABB.ofSize(blackHole.position(), radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> potentialVictims = level.getEntitiesOfClass(LivingEntity.class, scanArea);
        for (LivingEntity victim : potentialVictims) {
            ResourceLocation victimType = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
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
            // lock to current pos. Iron's already shoved them a tick, so pin them here from now on.
            anchor = victim.position();
            anchoredVictims.put(victimId, anchor);
        }
        victim.teleportTo(anchor.x, anchor.y, anchor.z);
        victim.setDeltaMovement(Vec3.ZERO);
        victim.hurtMarked = true;
        currentTickAnchored.add(victimId);
    }
}
