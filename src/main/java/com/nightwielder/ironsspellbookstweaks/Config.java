package com.nightwielder.ironsspellbookstweaks;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.DoubleValue BASE_MANA_REGEN_PERCENT;
    public static final ForgeConfigSpec.IntValue STARTING_MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_REDUCTION_BONUS;
    public static final ForgeConfigSpec.DoubleValue CAST_TIME_REDUCTION_BONUS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MANA_REGEN;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SPELL_CASTING_DISABLED_DIMENSIONS;
    public static final ForgeConfigSpec.ConfigValue<String> MAX_SPELL_RARITY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INSCRIPTION_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKHOLE_IMMUNITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("mana");
        BASE_MANA_REGEN_PERCENT = builder
                .comment("Bonus added to the player's MANA_REGEN attribute on every login. Stacks as a flat addition. Set to -1 to disable. Iron's vanilla default is around 1.0, so a value of 4.0 here gives an effective regen rate of 5.0.")
                .defineInRange("baseManaRegenPercent", -1.0, -1.0, 100.0);
        STARTING_MAX_MANA = builder
                .comment("Bonus added to the player's MAX_MANA attribute on every login. Stacks with the value as a flat addition. Set to -1 to disable. Iron's vanilla default is 100, so a value of 400 here gives players 500 max mana total.")
                .defineInRange("startingMaxMana", -1, -1, 100000);
        DISABLE_MANA_REGEN = builder
                .comment("Fully disable passive mana regen. Implemented as a tick drainback because Iron's regen path does not fire a cancellable event.")
                .define("disableManaRegen", false);
        builder.pop();
        builder.push("cooldown");
        COOLDOWN_REDUCTION_BONUS = builder
                .comment("Additive bonus applied to the COOLDOWN_REDUCTION attribute for every player. Around 0.5 cuts cooldowns roughly in half. Negative values lengthen them. Stacks with gear and effects.")
                .defineInRange("cooldownReductionBonus", 0.0, -10.0, 10.0);
        CAST_TIME_REDUCTION_BONUS = builder
                .comment("Additive bonus applied to the CAST_TIME_REDUCTION attribute for every player. Around 0.5 makes spells cast roughly twice as fast. Negative values lengthen cast times. Stacks with gear and effects.")
                .defineInRange("castTimeReductionBonus", 0.0, -10.0, 10.0);
        builder.pop();
        builder.push("restrictions");
        SPELL_CASTING_DISABLED_DIMENSIONS = builder
                .comment("Dimensions where spell casting is fully blocked. List of dimension IDs like \"minecraft:nether\" or \"twilightforest:twilight_forest\". Empty list disables this feature. Affects players only, not mob casters.")
                .defineList("spellCastingDisabledDimensions", List.of(), entry -> entry instanceof String);
        MAX_SPELL_RARITY = builder
                .comment("Highest spell rarity players are allowed to cast. Spells with a higher minimum rarity are blocked. Valid values: common, uncommon, rare, epic, legendary, or empty to disable. For example, 'uncommon' allows common and uncommon spells but blocks rare, epic, and legendary. 'legendary' allows everything. Mob casters are not affected.")
                .define("maxSpellRarity", "");
        INSCRIPTION_BLACKLIST = builder
                .comment("Spell IDs that cannot be inscribed at the inscription table. List of spell IDs like \"irons_spellbooks:fireball\". Empty list disables this feature. Players will see their inscription cancelled silently.")
                .defineList("inscriptionBlacklist", List.of(), entry -> entry instanceof String);
        builder.pop();
        builder.push("blackhole");
        BLACKHOLE_IMMUNITY = builder
                .comment("Per-entity-type resistance to black hole pull. Format: \"entity_id:strength\" where strength is 0.0 (no effect) to 1.0 (fully immune). Iron's hardcodes a 0.3 minimum pull regardless of KNOCKBACK_RESISTANCE attribute, so this exists to push past that floor for specific bosses. Affects black hole only, not other movement spells or vanilla knockback.")
                .defineList("blackholeImmunity", List.of(), entry -> entry instanceof String);
        builder.pop();
        SERVER_SPEC = builder.build();
    }
}
