// Fires entity_kill unlocks when a player kills a matching entity. Hooks LivingDeathEvent and resolves the killer through DamageSource.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
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
        ResourceLocation victimTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
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
