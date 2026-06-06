package com.nightwielder.ironsspellbookstweaks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressEventHandler;
import com.nightwielder.ironsspellbookstweaks.command.ISSTweaksCommandRegistry;
import com.nightwielder.ironsspellbookstweaks.handlers.BlackHoleResistanceHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.BuffDurationHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.ConfigOverrideHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.InscriptionBlacklistHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaAttributeHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaRegenCancelHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellCastDimensionHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellPowerMultiplierHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SpellRarityGateHandler;
import com.nightwielder.ironsspellbookstweaks.handlers.SummonScalingHandler;
import com.nightwielder.ironsspellbookstweaks.unlocks.AdvancementUnlockHandler;
import com.nightwielder.ironsspellbookstweaks.unlocks.EntityKillUnlockHandler;
import com.nightwielder.ironsspellbookstweaks.unlocks.RetroactiveUnlockHandler;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockManagerRegistry;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("irons_spellbooks_tweaks")
public class IronsSpellbooksTweaks {

    public static final String MOD_ID = "irons_spellbooks_tweaks";

    private static final Logger logger = LogManager.getLogger(MOD_ID);

    public IronsSpellbooksTweaks() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::registerCapabilities);

        MinecraftForge.EVENT_BUS.register(ManaAttributeHandler.class);
        MinecraftForge.EVENT_BUS.register(ManaRegenCancelHandler.class);
        MinecraftForge.EVENT_BUS.register(ConfigOverrideHandler.class);
        MinecraftForge.EVENT_BUS.register(BuffDurationHandler.class);
        // Register these handlers only when Iron's is loaded. Some hard-reference Iron's classes or event types in their signatures, which the bus resolves at registration before any runtime isLoaded check, and the rest read Iron's attributes or scale Iron's summons, so they do nothing useful without Iron's.
        if (IronsSpellbooksCompat.isLoaded()) {
            MinecraftForge.EVENT_BUS.register(SpellCastDimensionHandler.class);
            MinecraftForge.EVENT_BUS.register(SpellRarityGateHandler.class);
            MinecraftForge.EVENT_BUS.register(InscriptionBlacklistHandler.class);
            MinecraftForge.EVENT_BUS.register(BlackHoleResistanceHandler.class);
            MinecraftForge.EVENT_BUS.register(SummonScalingHandler.class);
            MinecraftForge.EVENT_BUS.register(SpellPowerMultiplierHandler.class);
        }
        MinecraftForge.EVENT_BUS.register(PlayerProgressEventHandler.class);
        MinecraftForge.EVENT_BUS.register(UnlockManagerRegistry.class);
        MinecraftForge.EVENT_BUS.register(AdvancementUnlockHandler.class);
        MinecraftForge.EVENT_BUS.register(RetroactiveUnlockHandler.class);
        MinecraftForge.EVENT_BUS.register(EntityKillUnlockHandler.class);
        MinecraftForge.EVENT_BUS.register(ISSTweaksCommandRegistry.class);
        // Register as COMMON so the toml writes to the global config folder instead of per-world serverconfig.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SERVER_SPEC, Config.SERVER_CONFIG_FILE);
        logger.info("Iron's Spellbooks Tweaks loaded");
        if (!IronsSpellbooksCompat.isLoaded()) {
            logger.warn("Iron's Spellbooks not detected, all handlers will no-op");
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerProgress.class);
    }
}
