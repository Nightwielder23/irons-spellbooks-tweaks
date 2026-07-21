package com.nightwielder.ironsspellbookstweaks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import com.nightwielder.ironsspellbookstweaks.handlers.BlackHoleResistanceHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.BuffAmplifiedMeleeDamageHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.InscriptionBlacklistHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.PerSpellScalingHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellCastDimensionHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellPowerMultiplierHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellRarityGateHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SummonScalingHandler;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IronsSpellbooksTweaks.MOD_ID)
public class IronsSpellbooksTweaks {

    public static final String MOD_ID = "irons_spellbooks_tweaks";

    private static final Logger logger = LogManager.getLogger(MOD_ID);

    public IronsSpellbooksTweaks(IEventBus modEventBus, ModContainer modContainer) {
        PlayerProgressAttachments.register(modEventBus);
        // Register as COMMON so the toml writes to the global config folder instead of per-world serverconfig.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SERVER_SPEC, Config.SERVER_CONFIG_FILE);

        // Register these handlers only when Iron's is loaded. Some hard-reference Iron's classes or event types in their signatures, which the bus resolves at registration before any runtime isLoaded check, and the rest read Iron's attributes or scale Iron's summons, so they do nothing useful without Iron's.
        if (IronsSpellbooksCompat.isLoaded()) {
            NeoForge.EVENT_BUS.register(InscriptionBlacklistHandler.class);
            NeoForge.EVENT_BUS.register(SpellCastDimensionHandler.class);
            NeoForge.EVENT_BUS.register(SpellRarityGateHandler.class);
            NeoForge.EVENT_BUS.register(BlackHoleResistanceHandler.class);
            NeoForge.EVENT_BUS.register(SummonScalingHandler.class);
            NeoForge.EVENT_BUS.register(SpellPowerMultiplierHandler.class);
            NeoForge.EVENT_BUS.register(PerSpellScalingHandler.class);
            NeoForge.EVENT_BUS.register(BuffAmplifiedMeleeDamageHandler.class);
            logger.info("Iron's Spellbooks Tweaks loaded with Iron's integration");
        } else {
            logger.info("Iron's Spellbooks not detected, gated handlers (inscription, dimension, rarity, black hole, summons) will not be registered");
        }
    }
}
