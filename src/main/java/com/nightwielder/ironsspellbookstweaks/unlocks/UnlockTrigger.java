// Trigger condition for an unlock. Sealed so future trigger types must opt in here.
package com.nightwielder.ironsspellbookstweaks.unlocks;

public sealed interface UnlockTrigger permits AdvancementTrigger, EntityKillTrigger {

    String type();
}
