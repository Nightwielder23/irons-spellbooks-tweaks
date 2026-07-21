// Single entry point for applying an unlock to a player. Used by trigger handlers and the admin command.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaAttributeHandler;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        PlayerProgress progress = player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        if (progress.hasUnlockGranted(unlock.getId())) {
            return;
        }
        applyGrants(progress, unlock.getGrants());
        progress.markUnlockGranted(unlock.getId());
        ManaAttributeHandler.refreshProgressModifiers(player);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(unlockMessage(unlock));
        }
        logger.info("applied unlock {} to player {}", unlock.getId(), player.getName().getString());
    }

    // Rebuild the additive bonuses from the unlocks the player still holds, then refresh the attribute modifiers. No message and no change to the granted set, since these unlocks are already earned. Callers strip the old modifiers first so a bonus that dropped to zero is not left behind.
    public static void reapplyGrants(Player player, PlayerProgress progress) {
        for (ResourceLocation unlockId : progress.getGrantedUnlocks()) {
            UnlockManager.getById(unlockId).ifPresent(unlock -> applyGrants(progress, unlock.getGrants()));
        }
        ManaAttributeHandler.refreshProgressModifiers(player);
    }

    private static void applyGrants(PlayerProgress progress, UnlockGrants grants) {
        if (grants.getCooldownReductionBonus() != 0.0) {
            progress.addCooldownBonus(grants.getCooldownReductionBonus());
        }
        if (grants.getCastTimeReductionBonus() != 0.0) {
            progress.addCastTimeBonus(grants.getCastTimeReductionBonus());
        }
        if (grants.getMaxManaBonus() != 0) {
            progress.addMaxManaBonus(grants.getMaxManaBonus());
        }
        if (grants.getManaRegenBonus() != 0.0) {
            progress.addManaRegenBonus(grants.getManaRegenBonus());
        }
        for (ResourceLocation dimension : grants.getDimensionsRemoved()) {
            progress.addDimensionRemoved(dimension);
        }
        for (ResourceLocation inscription : grants.getInscriptionsRemoved()) {
            progress.addInscriptionRemoved(inscription);
        }
        if (grants.getRarityCap() != null) {
            progress.raiseRarityCap(grants.getRarityCap());
        }
    }

    // A pack-provided message wins; without one, announce the unlock and summarize its grants in parens.
    private static Component unlockMessage(UnlockDefinition unlock) {
        Optional<String> custom = unlock.getMessage();
        if (custom.isPresent()) {
            return Component.literal(custom.get());
        }
        MutableComponent message = Component.empty()
                .append(Component.literal("Unlock earned: ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(UnlockDescriber.displayName(unlock)).withStyle(ChatFormatting.YELLOW));
        String summary = UnlockDescriber.grantSummary(unlock.getGrants());
        if (!summary.isEmpty()) {
            message.append(Component.literal(" (" + summary + ")"));
        }
        return message;
    }
}
