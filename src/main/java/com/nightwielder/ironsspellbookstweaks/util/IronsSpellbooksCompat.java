package com.nightwielder.ironsspellbookstweaks.util;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

// Runtime lookup of Iron's Spellbooks attributes by registry name, so we can soft-depend without compiling against their jar.
public class IronsSpellbooksCompat {

    public static final String IRONS_MOD_ID = "irons_spellbooks";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(IRONS_MOD_ID);
    }

    public static Optional<Attribute> getManaRegenAttribute() {
        return Optional.ofNullable(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(IRONS_MOD_ID, "mana_regen")));
    }

    public static Optional<Attribute> getMaxManaAttribute() {
        return Optional.ofNullable(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(IRONS_MOD_ID, "max_mana")));
    }

    public static Optional<Attribute> getCooldownReductionAttribute() {
        return Optional.ofNullable(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(IRONS_MOD_ID, "cooldown_reduction")));
    }

    public static Optional<Attribute> getCastTimeReductionAttribute() {
        return Optional.ofNullable(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(IRONS_MOD_ID, "cast_time_reduction")));
    }
}
