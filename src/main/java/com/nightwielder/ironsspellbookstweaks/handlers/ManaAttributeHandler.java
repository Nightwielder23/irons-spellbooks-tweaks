// Runs on the game bus since the server config isn't loaded during mod construction.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoublePredicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ManaAttributeHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/ManaAttributeHandler");

    // Reuse the same identity per modifier so relogin replaces the entry instead of stacking a duplicate.
    // Give config-sourced and progress-sourced bonuses distinct identities so they stack with each other.
    private static final ModifierId MANA_REGEN_OVERRIDE_ID = new ModifierId("3d2a8b1e-5e4f-4f1a-9c2d-1a2b3c4d5e6f", "ist_mana_regen_override");
    private static final ModifierId MAX_MANA_OVERRIDE_ID = new ModifierId("4e3b9c2f-6f50-5021-0d3e-2b3c4d5e6f70", "ist_max_mana_override");
    private static final ModifierId COOLDOWN_REDUCTION_BONUS_ID = new ModifierId("5f4cad30-7061-6132-1e4f-3c4d5e6f7081", "ist_cooldown_reduction_bonus");
    private static final ModifierId CAST_TIME_REDUCTION_BONUS_ID = new ModifierId("60c5be41-8172-7243-2f50-4d5e6f708192", "ist_cast_time_reduction_bonus");
    private static final ModifierId COOLDOWN_REDUCTION_PROGRESS_ID = new ModifierId("71d6cf52-9283-8354-3061-5e6f70819203", "ist_cooldown_reduction_progress");
    private static final ModifierId CAST_TIME_REDUCTION_PROGRESS_ID = new ModifierId("82e7d063-a394-9465-4172-6f7081920314", "ist_cast_time_reduction_progress");
    private static final ModifierId MAX_MANA_PROGRESS_ID = new ModifierId("93f8e174-b4a5-a576-5283-708192a30425", "ist_max_mana_progress");
    private static final ModifierId MANA_REGEN_PROGRESS_ID = new ModifierId("a409f285-c5b6-b687-6394-8192a304a536", "ist_mana_regen_progress");

    // The two override bonuses use -1 as a disable sentinel; everything else is off only at exactly zero.
    private static final DoublePredicate APPLIED_WHEN_NON_NEGATIVE = value -> value >= 0;
    private static final DoublePredicate APPLIED_WHEN_NONZERO = value -> value != 0.0;

    private static final List<ManaModifierRow> CONFIG_ROWS = List.of(
            new ManaModifierRow("baseManaRegenPercent override",
                    player -> RuntimeConfig.baseManaRegenPercent, APPLIED_WHEN_NON_NEGATIVE,
                    IronsSpellbooksCompat::getManaRegenAttribute, MANA_REGEN_OVERRIDE_ID,
                    "MANA_REGEN attribute not registered, skipping baseManaRegenPercent override"),
            new ManaModifierRow("startingMaxMana override",
                    player -> RuntimeConfig.startingMaxMana, APPLIED_WHEN_NON_NEGATIVE,
                    IronsSpellbooksCompat::getMaxManaAttribute, MAX_MANA_OVERRIDE_ID,
                    "MAX_MANA attribute not registered, skipping startingMaxMana override"),
            new ManaModifierRow("cooldownReductionBonus",
                    player -> RuntimeConfig.cooldownReductionBonus, APPLIED_WHEN_NONZERO,
                    IronsSpellbooksCompat::getCooldownReductionAttribute, COOLDOWN_REDUCTION_BONUS_ID,
                    "COOLDOWN_REDUCTION attribute not registered, skipping cooldownReductionBonus"),
            new ManaModifierRow("castTimeReductionBonus",
                    player -> RuntimeConfig.castTimeReductionBonus, APPLIED_WHEN_NONZERO,
                    IronsSpellbooksCompat::getCastTimeReductionAttribute, CAST_TIME_REDUCTION_BONUS_ID,
                    "CAST_TIME_REDUCTION attribute not registered, skipping castTimeReductionBonus"));

    private static final List<ManaModifierRow> PROGRESS_ROWS = List.of(
            new ManaModifierRow("progress cooldown bonus",
                    player -> progress(player, PlayerProgress::getCooldownReductionBonus), APPLIED_WHEN_NONZERO,
                    IronsSpellbooksCompat::getCooldownReductionAttribute, COOLDOWN_REDUCTION_PROGRESS_ID, null),
            new ManaModifierRow("progress cast time bonus",
                    player -> progress(player, PlayerProgress::getCastTimeReductionBonus), APPLIED_WHEN_NONZERO,
                    IronsSpellbooksCompat::getCastTimeReductionAttribute, CAST_TIME_REDUCTION_PROGRESS_ID, null),
            new ManaModifierRow("progress max mana bonus",
                    player -> progress(player, p -> (double) p.getMaxManaBonus()), APPLIED_WHEN_NONZERO,
                    IronsSpellbooksCompat::getMaxManaAttribute, MAX_MANA_PROGRESS_ID, null),
            new ManaModifierRow("progress mana regen bonus",
                    player -> progress(player, PlayerProgress::getManaRegenBonus), APPLIED_WHEN_NONZERO,
                    IronsSpellbooksCompat::getManaRegenAttribute, MANA_REGEN_PROGRESS_ID, null));

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        applyAll(event.getEntity());
    }

    // Respawn builds a fresh ServerPlayer without the permanent modifiers, so reapply on each respawn.
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        applyAll(event.getEntity());
    }

    private static void applyAll(Player player) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        for (ManaModifierRow row : CONFIG_ROWS) {
            apply(player, row);
        }
        for (ManaModifierRow row : PROGRESS_ROWS) {
            apply(player, row);
        }
    }

    // Called by UnlockApplicator when an unlock fires mid-session so the new bonus shows up before next login.
    public static void refreshProgressModifiers(Player player) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        for (ManaModifierRow row : PROGRESS_ROWS) {
            apply(player, row);
        }
    }

    // Pull every progress-sourced modifier off the player. Revoke and reset call this before rebuilding, since apply() leaves a stale modifier in place when its bonus drops to zero.
    public static void stripProgressModifiers(Player player) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        for (ManaModifierRow row : PROGRESS_ROWS) {
            Optional<Attribute> attribute = row.attribute().get();
            if (attribute.isEmpty()) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute.get());
            if (instance != null) {
                instance.removeModifier(row.modifier().uuid());
            }
        }
    }

    private static void apply(Player player, ManaModifierRow row) {
        double value = row.value().applyAsDouble(player);
        if (!row.appliedWhen().test(value)) {
            return;
        }
        Optional<Attribute> attribute = row.attribute().get();
        if (attribute.isEmpty()) {
            if (row.missingAttributeWarning() != null) {
                logger.warn(row.missingAttributeWarning());
            }
            return;
        }
        applyAdditiveOverride(player, attribute.get(), row.modifier(), value);
        logger.info("applied {} to {}: {}", row.label(), player.getName().getString(), value);
    }

    private static double progress(Player player, ToDoubleFunction<PlayerProgress> getter) {
        return player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(getter::applyAsDouble)
                .orElse(0.0);
    }

    private static void applyAdditiveOverride(Player player, Attribute attribute, ModifierId modifier, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        // drop any prior copy of the modifier so config edits take on relogin; removeModifier is null-safe
        instance.removeModifier(modifier.uuid());
        AttributeModifier attributeModifier = new AttributeModifier(modifier.uuid(), modifier.name(), value, AttributeModifier.Operation.ADDITION);
        instance.addPermanentModifier(attributeModifier);
    }

    private record ManaModifierRow(String label, ToDoubleFunction<Player> value, DoublePredicate appliedWhen,
            Supplier<Optional<Attribute>> attribute, ModifierId modifier, String missingAttributeWarning) {
    }

    private record ModifierId(UUID uuid, String name) {
        private ModifierId(String uuid, String name) {
            this(UUID.fromString(uuid), name);
        }
    }
}
