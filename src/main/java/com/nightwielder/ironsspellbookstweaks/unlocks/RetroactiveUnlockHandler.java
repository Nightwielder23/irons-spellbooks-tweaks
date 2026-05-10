// Scans loaded unlocks against a player's earned advancements on login. Catches unlocks added mid-playthrough or after a world transfer.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
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
        PlayerProgress progress = serverPlayer.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        int matchedCount = 0;
        for (UnlockDefinition unlock : UnlockManager.getAll().values()) {
            // entity_kill triggers can't be replayed retroactively since we don't track past kills. Players need to re-kill the entity, or an admin can grant via /isstweaks grant.
            if (!(unlock.getTrigger() instanceof AdvancementTrigger advancementTrigger)) {
                continue;
            }
            if (progress.hasUnlockGranted(unlock.getId())) {
                continue;
            }
            ResourceLocation advancementId = advancementTrigger.advancementId();
            AdvancementHolder advancement = server.getAdvancements().get(advancementId);
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
