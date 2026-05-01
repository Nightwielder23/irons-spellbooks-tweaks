// Scans loaded unlocks against a player's earned advancements on login. Catches unlocks added mid-playthrough or after a world transfer.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RetroactiveUnlockHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/RetroactiveUnlockHandler");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }
        PlayerAdvancements playerAdvancements = serverPlayer.getAdvancements();
        int matchedCount = 0;
        for (UnlockDefinition unlock : UnlockManager.getAll().values()) {
            // entity_kill triggers can't be replayed retroactively since we don't track past kills. Players will need to re-kill the entity, or an admin can grant via /isstweaks unlock.
            if (!(unlock.getTrigger() instanceof AdvancementTrigger advancementTrigger)) {
                continue;
            }
            // cheap dedup before the advancement lookup; missing capability also short-circuits to skip
            boolean alreadyGranted = serverPlayer.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                    .map(progress -> progress.hasUnlockGranted(unlock.getId()))
                    .orElse(true);
            if (alreadyGranted) {
                continue;
            }
            ResourceLocation advancementId = advancementTrigger.advancementId();
            Advancement advancement = server.getAdvancements().getAdvancement(advancementId);
            if (advancement == null) {
                // referenced advancement does not exist or is not loaded yet, skip silently
                continue;
            }
            if (!playerAdvancements.getOrStartProgress(advancement).isDone()) {
                continue;
            }
            UnlockApplicator.apply(serverPlayer, unlock);
            matchedCount++;
        }
        if (matchedCount > 0) {
            logger.info("retroactively applied {} unlocks to {}", matchedCount, serverPlayer.getName().getString());
        }
    }
}
