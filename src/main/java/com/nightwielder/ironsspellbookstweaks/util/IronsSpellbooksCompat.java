package com.nightwielder.ironsspellbookstweaks.util;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.fml.ModList;

// Looks up Iron's Spellbooks attributes by registry name at runtime so the mod can soft-depend without compiling against their jar.
public class IronsSpellbooksCompat {

    public static final String IRONS_MOD_ID = "irons_spellbooks";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(IRONS_MOD_ID);
    }

    public static Optional<Holder<Attribute>> getManaRegenAttribute() {
        return lookup("mana_regen");
    }

    public static Optional<Holder<Attribute>> getMaxManaAttribute() {
        return lookup("max_mana");
    }

    public static Optional<Holder<Attribute>> getCooldownReductionAttribute() {
        return lookup("cooldown_reduction");
    }

    public static Optional<Holder<Attribute>> getCastTimeReductionAttribute() {
        return lookup("cast_time_reduction");
    }

    public static Optional<Holder<Attribute>> getSpellPowerAttribute() {
        return lookup("spell_power");
    }

    private static Optional<Holder<Attribute>> lookup(String path) {
        return BuiltInRegistries.ATTRIBUTE
                .getOptional(ResourceLocation.fromNamespaceAndPath(IRONS_MOD_ID, path))
                .map(BuiltInRegistries.ATTRIBUTE::wrapAsHolder);
    }
}
