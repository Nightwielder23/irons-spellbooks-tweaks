// Hooks ISSTweaksCommand into the server's command dispatcher during the registration phase.
package com.nightwielder.ironsspellbookstweaks.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ISSTweaksCommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ISSTweaksCommand.register(event.getDispatcher());
    }
}
