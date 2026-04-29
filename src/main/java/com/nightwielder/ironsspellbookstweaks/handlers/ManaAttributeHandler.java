package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Adjusts base values of Iron's mana and spell timing attributes on the player entity type. All four hook the same event so they live in one handler.
public class ManaAttributeHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/ManaAttributeHandler");

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        applyManaRegenOverride(event);
        applyMaxManaOverride(event);
        applyCooldownReductionBonus(event);
        applyCastTimeReductionBonus(event);
    }

    private static void applyManaRegenOverride(EntityAttributeModificationEvent event) {
        double configuredValue = Config.BASE_MANA_REGEN_PERCENT.get();
        if (configuredValue < 0) {
            return;
        }
        Optional<Attribute> manaRegenAttribute = IronsSpellbooksCompat.getManaRegenAttribute();
        if (manaRegenAttribute.isEmpty()) {
            logger.warn("MANA_REGEN attribute not registered, skipping baseManaRegenPercent override");
            return;
        }
        event.add(EntityType.PLAYER, manaRegenAttribute.get(), configuredValue);
        logger.info("applied baseManaRegenPercent override: {}", configuredValue);
    }

    private static void applyMaxManaOverride(EntityAttributeModificationEvent event) {
        int configuredValue = Config.STARTING_MAX_MANA.get();
        if (configuredValue < 0) {
            return;
        }
        Optional<Attribute> maxManaAttribute = IronsSpellbooksCompat.getMaxManaAttribute();
        if (maxManaAttribute.isEmpty()) {
            logger.warn("MAX_MANA attribute not registered, skipping startingMaxMana override");
            return;
        }
        event.add(EntityType.PLAYER, maxManaAttribute.get(), configuredValue);
        logger.info("applied startingMaxMana override: {}", configuredValue);
    }

    private static void applyCooldownReductionBonus(EntityAttributeModificationEvent event) {
        double configuredValue = Config.COOLDOWN_REDUCTION_BONUS.get();
        if (configuredValue == 0.0) {
            return;
        }
        Optional<Attribute> cooldownReductionAttribute = IronsSpellbooksCompat.getCooldownReductionAttribute();
        if (cooldownReductionAttribute.isEmpty()) {
            logger.warn("COOLDOWN_REDUCTION attribute not registered, skipping cooldownReductionBonus");
            return;
        }
        event.add(EntityType.PLAYER, cooldownReductionAttribute.get(), configuredValue);
        logger.info("applied cooldownReductionBonus: {}", configuredValue);
    }

    private static void applyCastTimeReductionBonus(EntityAttributeModificationEvent event) {
        double configuredValue = Config.CAST_TIME_REDUCTION_BONUS.get();
        if (configuredValue == 0.0) {
            return;
        }
        Optional<Attribute> castTimeReductionAttribute = IronsSpellbooksCompat.getCastTimeReductionAttribute();
        if (castTimeReductionAttribute.isEmpty()) {
            logger.warn("CAST_TIME_REDUCTION attribute not registered, skipping castTimeReductionBonus");
            return;
        }
        event.add(EntityType.PLAYER, castTimeReductionAttribute.get(), configuredValue);
        logger.info("applied castTimeReductionBonus: {}", configuredValue);
    }
}
