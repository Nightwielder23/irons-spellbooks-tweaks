// Fires entity_kill unlocks when a player kills a matching entity. Hooks LivingDeathEvent and resolves the killer through DamageSource.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityKillUnlockHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        // ignore PvP for now; only mob kills count toward unlocks
        if (victim instanceof Player) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }
        ResourceLocation victimTypeId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (victimTypeId == null) {
            return;
        }
        List<UnlockDefinition> matchingUnlocks = UnlockManager.getByEntityKill(victimTypeId);
        if (matchingUnlocks.isEmpty()) {
            return;
        }
        for (UnlockDefinition unlock : matchingUnlocks) {
            UnlockApplicator.apply(player, unlock);
        }
    }
}
