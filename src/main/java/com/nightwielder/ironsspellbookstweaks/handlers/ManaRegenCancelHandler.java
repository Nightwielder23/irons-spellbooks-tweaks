// Drainback for disableManaRegen. Iron's regen does not fire a cancellable event so we sample mana per tick and undo any positive delta. Reflection-bound to MagicData on first use.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
public class ManaRegenCancelHandler {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/ManaRegenCancelHandler");
    private static final String MAGIC_DATA_CLASS = "io.redspace.ironsspellbooks.api.magic.MagicData";

    private static final Map<UUID, Float> lastKnownMana = new HashMap<>();
    private static boolean reflectionAvailable = true;
    private static boolean reflectionInitialized = false;

    private static Method getPlayerMagicDataMethod;
    private static Method getManaMethod;
    private static Method setManaMethod;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        if (!Config.DISABLE_MANA_REGEN.get()) {
            // clear cache when feature is off so a re-enable starts fresh
            if (!lastKnownMana.isEmpty()) {
                lastKnownMana.clear();
            }
            return;
        }
        if (!reflectionAvailable) {
            return;
        }
        if (!reflectionInitialized) {
            initReflection();
        }
        if (!reflectionAvailable) {
            return;
        }
        applyDrainback(player);
    }

    private static void initReflection() {
        reflectionInitialized = true;
        try {
            Class<?> magicDataClass = Class.forName(MAGIC_DATA_CLASS);
            // getPlayerMagicData is static and takes a LivingEntity
            Class<?> livingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");
            getPlayerMagicDataMethod = magicDataClass.getMethod("getPlayerMagicData", livingEntityClass);
            getManaMethod = magicDataClass.getMethod("getMana");
            setManaMethod = magicDataClass.getMethod("setMana", float.class);
        } catch (Exception lookupFailed) {
            logger.warn("could not bind to Iron's MagicData reflection, disableManaRegen will no-op", lookupFailed);
            reflectionAvailable = false;
        }
    }

    private static void applyDrainback(Player player) {
        try {
            Object magicData = getPlayerMagicDataMethod.invoke(null, player);
            if (magicData == null) {
                return;
            }
            float currentMana = (float) getManaMethod.invoke(magicData);
            UUID playerId = player.getUUID();
            Float previousMana = lastKnownMana.get(playerId);
            if (previousMana == null) {
                lastKnownMana.put(playerId, currentMana);
                return;
            }
            if (currentMana > previousMana) {
                // regen (or some other source) added mana. snap back to previous and leave the cache pinned there.
                setManaMethod.invoke(magicData, previousMana);
                return;
            }
            // mana went down or held steady. that is a legitimate spend (cast, drain effect, etc.) so adopt the new value as the floor.
            lastKnownMana.put(playerId, currentMana);
        } catch (Exception drainbackFailed) {
            logger.warn("drainback failed for player {}, disabling for rest of runtime", player.getUUID(), drainbackFailed);
            reflectionAvailable = false;
        }
    }
}
