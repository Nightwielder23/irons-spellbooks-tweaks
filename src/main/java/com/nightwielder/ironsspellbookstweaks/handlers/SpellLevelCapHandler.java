// Caps the level any player-cast spell can fire at. Mob casters bypass this so Iron's wizard mobs and bosses keep their full power.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpellLevelCapHandler {

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (!IronsSpellbooksCompat.isLoaded()) {
            return;
        }
        int cap = Config.MAX_SPELL_LEVEL_GLOBAL.get();
        if (cap < 0) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getLevel() > cap) {
            event.setLevel(cap);
        }
    }
}
