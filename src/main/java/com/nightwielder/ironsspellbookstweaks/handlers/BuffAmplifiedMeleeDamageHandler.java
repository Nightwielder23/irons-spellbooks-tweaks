// Some Iron's spells (Spider Aspect) and Cataclysm Spellbooks spells amplify the caster's melee hit on the damage event instead of dealing spell damage, so that damage never reaches SpellDamageEvent and PerSpellScalingHandler cannot see it. This applies the same damageMultipliers entry at LOWEST priority on LivingDamageEvent.Pre, which fires after both the Iron's LivingIncomingDamageEvent amplifier and the Cataclysm LivingDamageEvent.Pre amplifiers, so the multiplier lands on the already-amplified amount. The multiplier scales the whole hit amount at that point, base weapon damage included, which matches how the direct-cast damageMultipliers behaves. Real spell hits are skipped because PerSpellScalingHandler already covers them.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class BuffAmplifiedMeleeDamageHandler {

    // Effect id the attacker holds -> the spell id pack devs put in damageMultipliers. Cataclysm effect ids differ from their spell ids, so the map cannot assume the two match.
    private static final Map<ResourceLocation, ResourceLocation> AMPLIFY_EFFECTS;

    static {
        Map<ResourceLocation, ResourceLocation> effects = new HashMap<>();
        effects.put(id("irons_spellbooks", "spider_aspect"), id("irons_spellbooks", "spider_aspect"));
        if (ModList.get().isLoaded("cataclysm_spellbooks")) {
            effects.put(id("cataclysm_spellbooks", "abyssal_predator_effect"), id("cataclysm_spellbooks", "abyssal_predator"));
            effects.put(id("cataclysm_spellbooks", "wrathful_effect"), id("cataclysm_spellbooks", "forgone_rage"));
            effects.put(id("cataclysm_spellbooks", "kings_wrath"), id("cataclysm_spellbooks", "pharaohs_wrath"));
        }
        AMPLIFY_EFFECTS = Map.copyOf(effects);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (RuntimeConfig.spellDamageMultipliers.isEmpty()) {
            return;
        }
        // Spell hits go through PerSpellScalingHandler already, and scaling them here would tie an unrelated spell to the buff's config entry.
        if (event.getSource() instanceof SpellDamageSource) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        double factor = 1.0;
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : AMPLIFY_EFFECTS.entrySet()) {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(entry.getKey()).orElse(null);
            if (effect == null || !attacker.hasEffect(effect)) {
                continue;
            }
            Double multiplier = RuntimeConfig.spellDamageMultipliers.get(entry.getValue().toString());
            if (multiplier != null) {
                factor *= multiplier;
            }
        }
        if (factor != 1.0) {
            event.setNewDamage((float) (event.getNewDamage() * factor));
        }
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
