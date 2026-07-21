// Resolve Iron's classes by name so the handler stays loadable when Iron's is absent.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig.ScalingConfig;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SummonScalingHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/SummonScalingHandler");

    private static final String SUMMON_SWORDS_SPELL_ID = "irons_spellbooks:summon_swords";

    // Use a fixed id so a re-fired EntityJoinLevelEvent replaces the HP modifier instead of stacking another
    private static final UUID HP_SCALING_MODIFIER_ID = UUID.fromString("b51a7e2c-3f6d-4a89-9c10-2d4e6f8a0b13");

    // Resolve Iron's summon mob classes once by name. A null entry means the class was not found and scaling for it is skipped.
    private static boolean summonClassesResolved;
    private static Class<?> summonedVexClass;
    private static Class<?> summonedZombieClass;
    private static Class<?> summonedSkeletonClass;
    private static Class<?> summonedPolarBearClass;
    private static Class<?> summonedHorseClass;

    // Resolve Iron's damage-source reflection handles once by name. Any null here disables sword damage scaling.
    private static boolean damageClassesResolved;
    private static Class<?> spellDamageSourceClass;
    private static Method spellAccessor;
    private static Method getSpellIdAccessor;

    // Run at LOWEST priority so any other mod's MAX_HEALTH modifications on the same event land first. MULTIPLY_BASE composes with theirs.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity summon)) {
            return;
        }
        resolveSummonClasses();
        ScalingConfig config = RuntimeConfig.scaling;
        if (isInstanceOf(summonedVexClass, summon)) {
            applyHpScaling(summon, config.vexHpMultiplier);
        } else if (isInstanceOf(summonedZombieClass, summon) || isInstanceOf(summonedSkeletonClass, summon)) {
            applyHpScaling(summon, config.raiseDeadHpMultiplier);
        } else if (isInstanceOf(summonedPolarBearClass, summon)) {
            applyHpScaling(summon, config.polarBearHpMultiplier);
        } else if (isInstanceOf(summonedHorseClass, summon)) {
            applyHpScaling(summon, config.horseHpMultiplier);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        ScalingConfig config = RuntimeConfig.scaling;
        // Summon Swords carries its spell id on the damage source, so it is matched by spell rather than by attacker.
        // Handle it first and return so the summon-melee path below can never double-scale the same sword hit.
        if (isSummonSwordsDamage(event.getSource())) {
            if (config.swordsDamageMultiplier != 1.0) {
                event.setAmount((float) (event.getAmount() * config.swordsDamageMultiplier));
            }
            return;
        }
        // Iron's summon melee uses getDamageSource(summon, summoner). summon goes in the direct-entity slot, player in the causing-entity slot.
        // getEntity() gives the player, so getDirectEntity() returns the summon that actually swung.
        Entity attacker = event.getSource().getDirectEntity();
        if (attacker == null) {
            return;
        }
        resolveSummonClasses();
        double multiplier = mobDamageMultiplier(attacker, config);
        if (multiplier == 1.0) {
            return;
        }
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    private static double mobDamageMultiplier(Entity attacker, ScalingConfig config) {
        if (isInstanceOf(summonedVexClass, attacker)) {
            return config.vexDamageMultiplier;
        }
        if (isInstanceOf(summonedZombieClass, attacker) || isInstanceOf(summonedSkeletonClass, attacker)) {
            return config.raiseDeadDamageMultiplier;
        }
        if (isInstanceOf(summonedPolarBearClass, attacker)) {
            return config.polarBearDamageMultiplier;
        }
        return 1.0;
    }

    private static void applyHpScaling(LivingEntity summon, double multiplier) {
        AttributeInstance maxHealth = summon.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // strip prior scaling so a chunk-reload re-fire of EntityJoinLevelEvent does not stack a second modifier
        boolean alreadyScaled = maxHealth.getModifier(HP_SCALING_MODIFIER_ID) != null;
        if (alreadyScaled) {
            maxHealth.removeModifier(HP_SCALING_MODIFIER_ID);
        }
        if (multiplier == 1.0) {
            return;
        }
        AttributeModifier scaling;
        if (multiplier <= 0.0) {
            // MULTIPLY_BASE of -1 zeroes the base, but other ADDITION modifiers can still drift HP above 1,
            // so ADDITION of (1 - base) pins the total to 1, the closest stable alive floor.
            double pinDelta = 1.0 - maxHealth.getBaseValue();
            scaling = new AttributeModifier(HP_SCALING_MODIFIER_ID, "isstweaks_summon_hp_scaling", pinDelta, AttributeModifier.Operation.ADDITION);
        } else {
            // MULTIPLY_BASE composes with any other pack-balance scalers (Apotheosis, ScalingHealth, etc): a 3.0
            // multiplier here always means "3x the base Iron's intended", on top of whatever the pack already does.
            scaling = new AttributeModifier(HP_SCALING_MODIFIER_ID, "isstweaks_summon_hp_scaling", multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        maxHealth.addPermanentModifier(scaling);
        // a fresh summon should appear at full HP; on a re-fire only pull an over-max health bar back down
        float newMaxHealth = summon.getMaxHealth();
        if (!alreadyScaled || summon.getHealth() > newMaxHealth) {
            summon.setHealth(newMaxHealth);
        }
    }

    private static boolean isInstanceOf(Class<?> type, Entity entity) {
        return type != null && type.isInstance(entity);
    }

    private static boolean isSummonSwordsDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        resolveDamageClasses();
        if (spellDamageSourceClass == null || spellAccessor == null || getSpellIdAccessor == null) {
            return false;
        }
        if (!spellDamageSourceClass.isInstance(source)) {
            return false;
        }
        try {
            Object spell = spellAccessor.invoke(source);
            if (spell == null) {
                return false;
            }
            Object spellId = getSpellIdAccessor.invoke(spell);
            return SUMMON_SWORDS_SPELL_ID.equals(spellId);
        } catch (ReflectiveOperationException reflectionFailed) {
            logger.warn("could not read spell id from a SpellDamageSource, skipping sword damage scaling", reflectionFailed);
            return false;
        }
    }

    private static void resolveSummonClasses() {
        if (summonClassesResolved) {
            return;
        }
        summonClassesResolved = true;
        summonedVexClass = lookupClass("io.redspace.ironsspellbooks.entity.mobs.SummonedVex");
        summonedZombieClass = lookupClass("io.redspace.ironsspellbooks.entity.mobs.SummonedZombie");
        summonedSkeletonClass = lookupClass("io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton");
        summonedPolarBearClass = lookupClass("io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear");
        summonedHorseClass = lookupClass("io.redspace.ironsspellbooks.entity.mobs.SummonedHorse");
    }

    private static void resolveDamageClasses() {
        if (damageClassesResolved) {
            return;
        }
        damageClassesResolved = true;
        try {
            spellDamageSourceClass = Class.forName("io.redspace.ironsspellbooks.damage.SpellDamageSource");
            spellAccessor = spellDamageSourceClass.getMethod("spell");
            Class<?> abstractSpellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            getSpellIdAccessor = abstractSpellClass.getMethod("getSpellId");
        } catch (ReflectiveOperationException lookupFailed) {
            logger.warn("could not resolve Iron's SpellDamageSource API, so sword damage scaling is disabled", lookupFailed);
        }
    }

    private static Class<?> lookupClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException lookupFailed) {
            logger.warn("Iron's class {} not found, so HP scaling for it is disabled", className);
            return null;
        }
    }
}
