package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PerSpellScalingHandler {

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (RuntimeConfig.spellDamageMultipliers.isEmpty()) {
            return;
        }
        AbstractSpell spell = event.getSpellDamageSource().spell();
        if (spell == null) {
            return;
        }
        Double multiplier = RuntimeConfig.spellDamageMultipliers.get(spell.getSpellId());
        if (multiplier == null) {
            return;
        }
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (RuntimeConfig.spellCooldownMultipliers.isEmpty()) {
            return;
        }
        AbstractSpell spell = event.getSpell();
        if (spell == null) {
            return;
        }
        Double multiplier = RuntimeConfig.spellCooldownMultipliers.get(spell.getSpellId());
        if (multiplier == null) {
            return;
        }
        int scaled = (int) Math.round(event.getEffectiveCooldown() * multiplier);
        event.setEffectiveCooldown(Math.max(0, scaled));
    }

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (RuntimeConfig.spellManaCostMultipliers.isEmpty()) {
            return;
        }
        Double multiplier = RuntimeConfig.spellManaCostMultipliers.get(event.getSpellId());
        if (multiplier == null) {
            return;
        }
        Player player = event.getEntity();
        // creative and mana-free cast sources never pay, so don't gate them
        if (player.isCreative() || !event.getCastSource().consumesMana()) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(event.getSpellId());
        if (spell == null) {
            return;
        }
        // Iron's affordability check ran against the base cost and never sees the per-spell multiplier, so re-check against the scaled cost; otherwise a multiplier above 1.0 would let an unaffordable cast through and drain mana to zero.
        int scaledCost = Math.max(0, (int) Math.round(spell.getManaCost(event.getSpellLevel()) * multiplier));
        if (MagicData.getPlayerMagicData(player).getMana() < scaledCost) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (RuntimeConfig.spellManaCostMultipliers.isEmpty()) {
            return;
        }
        Double multiplier = RuntimeConfig.spellManaCostMultipliers.get(event.getSpellId());
        if (multiplier == null) {
            return;
        }
        int scaled = (int) Math.round(event.getManaCost() * multiplier);
        event.setManaCost(Math.max(0, scaled));
    }
}
