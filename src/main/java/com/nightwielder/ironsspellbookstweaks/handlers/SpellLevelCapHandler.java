// Caps the level any player-cast spell can fire at. Mob casters bypass this so Iron's wizard mobs and bosses keep their full power.
package com.nightwielder.ironsspellbookstweaks.handlers;

import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
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
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int playerCap = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(PlayerProgress::getSpellLevelCap)
                .orElse(-1);
        int configCap = Config.MAX_SPELL_LEVEL_GLOBAL.get();
        // -1 is the disable sentinel on both sides, so plain Math.max would silently use it as a real cap
        int effectiveCap;
        if (configCap < 0 && playerCap < 0) {
            return;
        } else if (configCap < 0) {
            effectiveCap = playerCap;
        } else if (playerCap < 0) {
            effectiveCap = configCap;
        } else {
            effectiveCap = Math.max(configCap, playerCap);
        }
        if (event.getLevel() > effectiveCap) {
            event.setLevel(effectiveCap);
        }
    }
}
