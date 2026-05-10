package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Applies configured attribute overrides to players on login. Runs on the Forge bus because per-world server configs are not loaded until after mod-bus events fire.
public class ManaAttributeHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/ManaAttributeHandler");

    // Stable UUIDs let us replace our own modifiers on relogin instead of stacking duplicates.
    private static final UUID MANA_REGEN_MODIFIER_ID = UUID.fromString("3d2a8b1e-5e4f-4f1a-9c2d-1a2b3c4d5e6f");
    private static final UUID MAX_MANA_MODIFIER_ID = UUID.fromString("4e3b9c2f-6f50-5021-0d3e-2b3c4d5e6f70");
    private static final UUID COOLDOWN_REDUCTION_MODIFIER_ID = UUID.fromString("5f4cad30-7061-6132-1e4f-3c4d5e6f7081");
    private static final UUID CAST_TIME_REDUCTION_MODIFIER_ID = UUID.fromString("60c5be41-8172-7243-2f50-4d5e6f708192");
    // distinct UUIDs for capability-sourced bonuses so they stack with the config-sourced modifiers above
    private static final UUID COOLDOWN_REDUCTION_PROGRESS_MODIFIER_ID = UUID.fromString("71d6cf52-9283-8354-3061-5e6f70819203");
    private static final UUID CAST_TIME_REDUCTION_PROGRESS_MODIFIER_ID = UUID.fromString("82e7d063-a394-9465-4172-6f7081920314");
    private static final UUID MAX_MANA_PROGRESS_MODIFIER_ID = UUID.fromString("93f8e174-b4a5-a576-5283-708192a30425");
    private static final UUID MANA_REGEN_PROGRESS_MODIFIER_ID = UUID.fromString("a409f285-c5b6-b687-6394-8192a304a536");

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        Player player = event.getEntity();
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
        Optional<Attribute> manaRegenAttribute = IronsSpellbooksCompat.getManaRegenAttribute();
        if (manaRegenAttribute.isEmpty()) {
            logger.warn("MANA_REGEN attribute not registered, skipping baseManaRegenPercent override");
            return;
        }
        applyAdditiveOverride(player, manaRegenAttribute.get(), MANA_REGEN_MODIFIER_ID, "ist_mana_regen_override", configuredValue);
        logger.info("applied baseManaRegenPercent override to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyMaxManaOverride(Player player) {
        int configuredValue = Config.STARTING_MAX_MANA.get();
        if (configuredValue < 0) {
            return;
        }
        Optional<Attribute> maxManaAttribute = IronsSpellbooksCompat.getMaxManaAttribute();
        if (maxManaAttribute.isEmpty()) {
            logger.warn("MAX_MANA attribute not registered, skipping startingMaxMana override");
            return;
        }
        applyAdditiveOverride(player, maxManaAttribute.get(), MAX_MANA_MODIFIER_ID, "ist_max_mana_override", configuredValue);
        logger.info("applied startingMaxMana override to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyCooldownReductionBonus(Player player) {
        double configuredValue = Config.COOLDOWN_REDUCTION_BONUS.get();
        if (configuredValue == 0.0) {
            return;
        }
        Optional<Attribute> cooldownReductionAttribute = IronsSpellbooksCompat.getCooldownReductionAttribute();
        if (cooldownReductionAttribute.isEmpty()) {
            logger.warn("COOLDOWN_REDUCTION attribute not registered, skipping cooldownReductionBonus");
            return;
        }
        applyAdditiveOverride(player, cooldownReductionAttribute.get(), COOLDOWN_REDUCTION_MODIFIER_ID, "ist_cooldown_reduction_bonus", configuredValue);
        logger.info("applied cooldownReductionBonus to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyCastTimeReductionBonus(Player player) {
        double configuredValue = Config.CAST_TIME_REDUCTION_BONUS.get();
        if (configuredValue == 0.0) {
            return;
        }
        Optional<Attribute> castTimeReductionAttribute = IronsSpellbooksCompat.getCastTimeReductionAttribute();
        if (castTimeReductionAttribute.isEmpty()) {
            logger.warn("CAST_TIME_REDUCTION attribute not registered, skipping castTimeReductionBonus");
            return;
        }
        applyAdditiveOverride(player, castTimeReductionAttribute.get(), CAST_TIME_REDUCTION_MODIFIER_ID, "ist_cast_time_reduction_bonus", configuredValue);
        logger.info("applied castTimeReductionBonus to {}: {}", player.getName().getString(), configuredValue);
    }

    private static void applyProgressCooldownBonus(Player player) {
        double progressValue = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(PlayerProgress::getCooldownReductionBonus)
                .orElse(0.0);
        if (progressValue == 0.0) {
            return;
        }
        Optional<Attribute> cooldownReductionAttribute = IronsSpellbooksCompat.getCooldownReductionAttribute();
        if (cooldownReductionAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, cooldownReductionAttribute.get(), COOLDOWN_REDUCTION_PROGRESS_MODIFIER_ID, "ist_cooldown_reduction_progress", progressValue);
        logger.info("applied progress cooldown bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyProgressCastTimeBonus(Player player) {
        double progressValue = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(PlayerProgress::getCastTimeReductionBonus)
                .orElse(0.0);
        if (progressValue == 0.0) {
            return;
        }
        Optional<Attribute> castTimeReductionAttribute = IronsSpellbooksCompat.getCastTimeReductionAttribute();
        if (castTimeReductionAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, castTimeReductionAttribute.get(), CAST_TIME_REDUCTION_PROGRESS_MODIFIER_ID, "ist_cast_time_reduction_progress", progressValue);
        logger.info("applied progress cast time bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyProgressMaxManaBonus(Player player) {
        int progressValue = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(PlayerProgress::getMaxManaBonus)
                .orElse(0);
        if (progressValue == 0) {
            return;
        }
        Optional<Attribute> maxManaAttribute = IronsSpellbooksCompat.getMaxManaAttribute();
        if (maxManaAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, maxManaAttribute.get(), MAX_MANA_PROGRESS_MODIFIER_ID, "ist_max_mana_progress", progressValue);
        logger.info("applied progress max mana bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyProgressManaRegenBonus(Player player) {
        double progressValue = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(PlayerProgress::getManaRegenBonus)
                .orElse(0.0);
        if (progressValue == 0.0) {
            return;
        }
        Optional<Attribute> manaRegenAttribute = IronsSpellbooksCompat.getManaRegenAttribute();
        if (manaRegenAttribute.isEmpty()) {
            return;
        }
        applyAdditiveOverride(player, manaRegenAttribute.get(), MANA_REGEN_PROGRESS_MODIFIER_ID, "ist_mana_regen_progress", progressValue);
        logger.info("applied progress mana regen bonus to {}: {}", player.getName().getString(), progressValue);
    }

    private static void applyAdditiveOverride(Player player, Attribute attribute, UUID modifierId, String modifierName, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        // remove any prior version of our modifier so changing the config + re-logging in actually updates the value
        AttributeModifier existing = instance.getModifier(modifierId);
        if (existing != null) {
            instance.removeModifier(modifierId);
        }
        AttributeModifier modifier = new AttributeModifier(modifierId, modifierName, value, AttributeModifier.Operation.ADDITION);
        instance.addPermanentModifier(modifier);
    }
}
