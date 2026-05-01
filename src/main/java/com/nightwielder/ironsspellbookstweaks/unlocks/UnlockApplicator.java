// Single entry point for applying an unlock to a player. Used by trigger handlers and the admin command.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaAttributeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UnlockApplicator {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/UnlockApplicator");

    private UnlockApplicator() {
    }

    public static void apply(Player player, UnlockDefinition unlock) {
        player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(progress -> {
            if (progress.hasUnlockGranted(unlock.getId())) {
                return;
            }
            UnlockGrants grants = unlock.getGrants();
            if (grants.getSpellLevelCap() > 0) {
                progress.raiseSpellLevelCap(grants.getSpellLevelCap());
            }
            if (grants.getCooldownReductionBonus() != 0.0) {
                progress.addCooldownBonus(grants.getCooldownReductionBonus());
            }
            if (grants.getCastTimeReductionBonus() != 0.0) {
                progress.addCastTimeBonus(grants.getCastTimeReductionBonus());
            }
            for (ResourceLocation dimension : grants.getDimensionsRemoved()) {
                progress.addDimensionRemoved(dimension);
            }
            for (ResourceLocation inscription : grants.getInscriptionsRemoved()) {
                progress.addInscriptionRemoved(inscription);
            }
            progress.markUnlockGranted(unlock.getId());
            ManaAttributeHandler.refreshProgressModifiers(player);
            unlock.getMessage().ifPresent(messageText -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal(messageText));
                }
            });
            logger.info("applied unlock {} to player {}", unlock.getId(), player.getName().getString());
        });
    }
}
