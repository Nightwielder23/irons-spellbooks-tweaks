// Some Iron's spells (Spider Aspect) amplify the caster's melee hit on LivingDamageEvent instead of dealing spell damage, so that damage never reaches SpellDamageEvent and PerSpellScalingHandler cannot see it. This applies the same damageMultipliers entry at LOWEST priority, after the effect has amplified the hit. The multiplier scales the whole hit amount at that point, base weapon damage included, which matches how the direct-cast damageMultipliers behaves. Real spell hits are skipped because PerSpellScalingHandler already covers them.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class BuffAmplifiedMeleeDamageHandler {

    // Effect id the attacker holds -> the spell id pack devs put in damageMultipliers.
    private static final Map<ResourceLocation, ResourceLocation> AMPLIFY_EFFECTS = Map.of(
            new ResourceLocation("irons_spellbooks", "spider_aspect"),
            new ResourceLocation("irons_spellbooks", "spider_aspect"));

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
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
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(entry.getKey());
            if (effect == null || !attacker.hasEffect(effect)) {
                continue;
            }
            Double multiplier = RuntimeConfig.spellDamageMultipliers.get(entry.getValue().toString());
            if (multiplier != null) {
                factor *= multiplier;
            }
        }
        if (factor != 1.0) {
            event.setAmount((float) (event.getAmount() * factor));
        }
    }
}
