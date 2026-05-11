// Applies configured attribute overrides to players on login.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
public class ManaAttributeHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/ManaAttributeHandler");

    // Stable ResourceLocation keys let us replace our own modifiers on relogin instead of stacking duplicates.
    private static final ResourceLocation MANA_REGEN_OVERRIDE_ID = id("ist_mana_regen_override");
    private static final ResourceLocation MAX_MANA_OVERRIDE_ID = id("ist_max_mana_override");
    private static final ResourceLocation COOLDOWN_REDUCTION_BONUS_ID = id("ist_cooldown_reduction_bonus");
    private static final ResourceLocation CAST_TIME_REDUCTION_BONUS_ID = id("ist_cast_time_reduction_bonus");
    // distinct ids for attachment-sourced bonuses so they stack with the config-sourced modifiers above
    private static final ResourceLocation COOLDOWN_REDUCTION_PROGRESS_ID = id("ist_cooldown_reduction_progress");
    private static final ResourceLocation CAST_TIME_REDUCTION_PROGRESS_ID = id("ist_cast_time_reduction_progress");
    private static final ResourceLocation MAX_MANA_PROGRESS_ID = id("ist_max_mana_progress");
    private static final ResourceLocation MANA_REGEN_PROGRESS_ID = id("ist_mana_regen_progress");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IronsSpellbooksTweaks.MOD_ID, path);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        applyAll(event.getEntity());
    }

    // ServerPlayer.restoreFrom only copies attribute base values, not modifiers, so respawned players lose ours.
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyAll(event.getEntity());
    }

    private static void applyAll(Player player) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        applyManaRegenOverride(player);
        applyMaxManaOverride(player);
        applyCooldownReductionBonus(player);
        applyCastTimeReductionBonus(player);
        applyProgressCooldownBonus(player);
        applyProgressCastTimeBonus(player);
        applyProgressMaxManaBonus(player);
        applyProgressManaRegenBonus(player);
    }

    // Called by UnlockApplicator when an unlock fires mid-session so the new bonus shows up before next login.
    public static void refreshProgressModifiers(Player player) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        applyProgressCooldownBonus(player);
        applyProgressCastTimeBonus(player);
        applyProgressMaxManaBonus(player);
        applyProgressManaRegenBonus(player);
    }

    private static void applyManaRegenOverride(Player player) {
        double configuredValue = Config.BASE_MANA_REGEN_PERCENT.get();
        if (configuredValue < 0) {
            return;
        }
        Optional<Holder<Attribute>> manaRegenAttribute = IronsSpellbooksCompat.getManaRegenAttribute();
        if (manaRegenAttribute.isEmpty()) {
            logger.warn("MANA_REGEN attribute not registered, skipping baseManaRegenPercent override");
            return;
        }
        applyAdditiveOverride(player, manaRegenAttribute.get(), MANA_REGEN_OVERRIDE_ID, configuredValue);
        logger.info("applied baseManaRegenPercent override to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyMaxManaOverride(Player player) {
        int configuredValue = Config.STARTING_MAX_MANA.get();
        if (configuredValue < 0) {
            return;
        }
        Optional<Holder<Attribute>> maxManaAttribute = IronsSpellbooksCompat.getMaxManaAttribute();
        if (maxManaAttribute.isEmpty()) {
            logger.warn("MAX_MANA attribute not registered, skipping startingMaxMana override");
            return;
        }
        applyAdditiveOverride(player, maxManaAttribute.get(), MAX_MANA_OVERRIDE_ID, configuredValue);
        logger.info("applied startingMaxMana override to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyCooldownReductionBonus(Player player) {
        double configuredValue = Config.COOLDOWN_REDUCTION_BONUS.get();
        if (configuredValue == 0.0) {
            return;
        }
        Optional<Holder<Attribute>> cooldownReductionAttribute = IronsSpellbooksCompat.getCooldownReductionAttribute();
        if (cooldownReductionAttribute.isEmpty()) {
            logger.warn("COOLDOWN_REDUCTION attribute not registered, skipping cooldownReductionBonus");
            return;
        }
        applyAdditiveOverride(player, cooldownReductionAttribute.get(), COOLDOWN_REDUCTION_BONUS_ID, configuredValue);
        logger.info("applied cooldownReductionBonus to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyCastTimeReductionBonus(Player player) {
        double configuredValue = Config.CAST_TIME_REDUCTION_BONUS.get();
        if (configuredValue == 0.0) {
            return;
        }
        Optional<Holder<Attribute>> castTimeReductionAttribute = IronsSpellbooksCompat.getCastTimeReductionAttribute();
        if (castTimeReductionAttribute.isEmpty()) {
            logger.warn("CAST_TIME_REDUCTION attribute not registered, skipping castTimeReductionBonus");
            return;
        }
        applyAdditiveOverride(player, castTimeReductionAttribute.get(), CAST_TIME_REDUCTION_BONUS_ID, configuredValue);
        logger.info("applied castTimeReductionBonus to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyProgressCooldownBonus(Player player) {
        PlayerProgress progress = player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        double progressValue = progress.getCooldownReductionBonus();
        if (progressValue == 0.0) {
            return;
        }
        Optional<Holder<Attribute>> cooldownReductionAttribute = IronsSpellbooksCompat.getCooldownReductionAttribute();
        if (cooldownReductionAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, cooldownReductionAttribute.get(), COOLDOWN_REDUCTION_PROGRESS_ID, progressValue);
        logger.info("applied progress cooldown bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyProgressCastTimeBonus(Player player) {
        PlayerProgress progress = player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        double progressValue = progress.getCastTimeReductionBonus();
        if (progressValue == 0.0) {
            return;
        }
        Optional<Holder<Attribute>> castTimeReductionAttribute = IronsSpellbooksCompat.getCastTimeReductionAttribute();
        if (castTimeReductionAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, castTimeReductionAttribute.get(), CAST_TIME_REDUCTION_PROGRESS_ID, progressValue);
        logger.info("applied progress cast time bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyProgressMaxManaBonus(Player player) {
        PlayerProgress progress = player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        int progressValue = progress.getMaxManaBonus();
        if (progressValue == 0) {
            return;
        }
        Optional<Holder<Attribute>> maxManaAttribute = IronsSpellbooksCompat.getMaxManaAttribute();
        if (maxManaAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, maxManaAttribute.get(), MAX_MANA_PROGRESS_ID, progressValue);
        logger.info("applied progress max mana bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyProgressManaRegenBonus(Player player) {
        PlayerProgress progress = player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
        double progressValue = progress.getManaRegenBonus();
        if (progressValue == 0.0) {
            return;
        }
        Optional<Holder<Attribute>> manaRegenAttribute = IronsSpellbooksCompat.getManaRegenAttribute();
        if (manaRegenAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, manaRegenAttribute.get(), MANA_REGEN_PROGRESS_ID, progressValue);
        logger.info("applied progress mana regen bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyAdditiveOverride(Player player, Holder<Attribute> attribute, ResourceLocation modifierId, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        // drop any prior copy of our modifier so config edits take on relogin; removeModifier is null-safe
        instance.removeModifier(modifierId);
        AttributeModifier modifier = new AttributeModifier(modifierId, value, AttributeModifier.Operation.ADD_VALUE);
        instance.addPermanentModifier(modifier);
    }
}
