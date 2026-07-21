// Sets the duration field by reflection because MobEffectInstance exposes no setter.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.config.RuntimeConfig;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BuffDurationHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/BuffDurationHandler");

    private static Field durationField;
    private static boolean durationFieldResolved;
    private static final Set<String> seenUnconfiguredNamespaces = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        double multiplier = RuntimeConfig.buffDurationMultiplier;
        if (multiplier == 1.0) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        ResourceLocation id = effectId(effect);
        if (id == null) {
            return;
        }
        String namespace = id.getNamespace();
        // Log an unconfigured non-vanilla namespace once so pack devs can add it to the config.
        if (!namespace.equals("minecraft") && !RuntimeConfig.buffDurationNamespaces.contains(namespace) && seenUnconfiguredNamespaces.add(namespace)) {
            logger.info("encountered effect namespace '{}' not in buffDurationNamespaces config. add it to scale effects from that mod.", namespace);
        }
        // Scale only effects whose namespace is in the configured list so vanilla potions and other mods stay untouched.
        if (!RuntimeConfig.buffDurationNamespaces.contains(namespace)) {
            return;
        }
        // Leave infinite effects alone so a multiplier never turns them finite.
        if (effect.isInfiniteDuration()) {
            return;
        }
        Field field = durationField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(effect, scaleDuration(effect.getDuration(), multiplier));
        } catch (IllegalAccessException accessFailed) {
            logger.warn("could not set effect duration, so buffDurationMultiplier is skipped", accessFailed);
        }
    }

    private static int scaleDuration(int duration, double multiplier) {
        if (multiplier <= 0.0) {
            return 0;
        }
        long scaled = Math.round(duration * multiplier);
        if (scaled < 1) {
            return 1;
        }
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) scaled;
    }

    private static ResourceLocation effectId(MobEffectInstance effect) {
        return ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
    }

    // Resolve the duration field once, trying the Mojmap name first and the Forge SRG name second.
    private static Field durationField() {
        if (durationFieldResolved) {
            return durationField;
        }
        durationFieldResolved = true;
        for (String name : new String[] {"duration", "f_19503_"}) {
            try {
                Field field = MobEffectInstance.class.getDeclaredField(name);
                field.setAccessible(true);
                durationField = field;
                logger.info("resolved MobEffectInstance duration field via name '{}'", name);
                return durationField;
            } catch (NoSuchFieldException missing) {
                // try the next mapping name
            } catch (RuntimeException inaccessible) {
                logger.warn("could not access the MobEffectInstance duration field, so buffDurationMultiplier is skipped", inaccessible);
                return null;
            }
        }
        logger.warn("MobEffectInstance duration field not found, so buffDurationMultiplier is skipped");
        return null;
    }
}
