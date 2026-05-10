// Hooks ISSTweaksCommand into the server's command dispatcher during the registration phase.
package com.nightwielder.ironsspellbookstweaks.command;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = IronsSpellbooksTweaks.MOD_ID)
public class ISSTweaksCommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ISSTweaksCommand.register(event.getDispatcher());
    }
}
