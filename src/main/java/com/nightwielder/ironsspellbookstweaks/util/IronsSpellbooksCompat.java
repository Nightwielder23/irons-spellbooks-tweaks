package com.nightwielder.ironsspellbookstweaks.util;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

// Looks up Iron's Spellbooks attributes by registry name at runtime so the mod can soft-depend without compiling against their jar.
public class IronsSpellbooksCompat {

    public static final String IRONS_MOD_ID = "irons_spellbooks";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(IRONS_MOD_ID);
    }

    public static Optional<Attribute> getManaRegenAttribute() {
        return lookup("mana_regen");
    }

    public static Optional<Attribute> getMaxManaAttribute() {
        return lookup("max_mana");
    }

    public static Optional<Attribute> getCooldownReductionAttribute() {
        return lookup("cooldown_reduction");
    }

    public static Optional<Attribute> getCastTimeReductionAttribute() {
        return lookup("cast_time_reduction");
    }

    public static Optional<Attribute> getSpellPowerAttribute() {
        return lookup("spell_power");
    }

    private static Optional<Attribute> lookup(String path) {
        return Optional.ofNullable(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(IRONS_MOD_ID, path)));
    }
}
