package com.nightwielder.ironsspellbookstweaks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import com.nightwielder.ironsspellbookstweaks.handlers.BlackHoleResistanceHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.InscriptionBlacklistHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellCastDimensionHandler;
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
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

        // gated on Iron's presence because these handlers reference Iron's event types in their @SubscribeEvent parameter signatures, which the bus resolves at registration time before any runtime isLoaded check could intercept
        if (IronsSpellbooksCompat.isLoaded()) {
            NeoForge.EVENT_BUS.register(InscriptionBlacklistHandler.class);
            NeoForge.EVENT_BUS.register(SpellCastDimensionHandler.class);
            NeoForge.EVENT_BUS.register(SpellRarityGateHandler.class);
            NeoForge.EVENT_BUS.register(BlackHoleResistanceHandler.class);
            NeoForge.EVENT_BUS.register(SummonScalingHandler.class);
            logger.info("Iron's Spellbooks Tweaks loaded with Iron's integration");
        } else {
            logger.info("Iron's Spellbooks not detected, gated handlers (inscription, dimension, rarity, black hole, summons) will not be registered");
        }
    }
}
