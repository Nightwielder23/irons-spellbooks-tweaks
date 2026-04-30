package com.nightwielder.ironsspellbookstweaks;

import com.nightwielder.ironsspellbookstweaks.handlers.InscriptionBlacklistHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaAttributeHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaRegenCancelHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellCastDimensionHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellLevelCapHandler;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("irons_spellbooks_tweaks")
public class IronsSpellbooksTweaks {

    public static final String MOD_ID = "irons_spellbooks_tweaks";

    private static final Logger logger = LogManager.getLogger(MOD_ID);

    public IronsSpellbooksTweaks() {
        MinecraftForge.EVENT_BUS.register(ManaAttributeHandler.class);
        MinecraftForge.EVENT_BUS.register(ManaRegenCancelHandler.class);
        MinecraftForge.EVENT_BUS.register(SpellCastDimensionHandler.class);
        MinecraftForge.EVENT_BUS.register(SpellLevelCapHandler.class);
        MinecraftForge.EVENT_BUS.register(InscriptionBlacklistHandler.class);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        logger.info("Iron's Spellbooks Tweaks loaded");
        if (!IronsSpellbooksCompat.isLoaded()) {
            logger.warn("Iron's Spellbooks not detected, all handlers will no-op");
        }
    }
}
