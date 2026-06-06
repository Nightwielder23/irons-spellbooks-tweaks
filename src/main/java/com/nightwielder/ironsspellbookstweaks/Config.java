package com.nightwielder.ironsspellbookstweaks;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

// Defines global server settings that a per-world serverconfig copy overrides per key.
public class Config {

    public static final String SERVER_CONFIG_FILE = "irons_spellbooks_tweaks-server.toml";

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue BASE_MANA_REGEN_PERCENT;
    public static final ModConfigSpec.IntValue STARTING_MAX_MANA;
    public static final ModConfigSpec.DoubleValue COOLDOWN_REDUCTION_BONUS;
    public static final ModConfigSpec.DoubleValue CAST_TIME_REDUCTION_BONUS;
    public static final ModConfigSpec.BooleanValue DISABLE_MANA_REGEN;
    public static final ModConfigSpec.DoubleValue SPELL_POWER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BUFF_DURATION_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BUFF_DURATION_NAMESPACES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPELL_CASTING_DISABLED_DIMENSIONS;
    public static final ModConfigSpec.ConfigValue<String> MAX_SPELL_RARITY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INSCRIPTION_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKHOLE_IMMUNITY;
    public static final ModConfigSpec.DoubleValue SUMMON_VEX_HP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SUMMON_VEX_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RAISE_DEAD_HP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RAISE_DEAD_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SUMMON_POLAR_BEAR_HP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SUMMON_POLAR_BEAR_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SUMMON_HORSE_HP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SUMMON_SWORDS_DAMAGE_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
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
        builder.push("spells");
        SPELL_POWER_MULTIPLIER = builder
                .comment("Multiplier applied to every player's SPELL_POWER attribute. 1.0 leaves spell power unchanged. Stacks multiplicatively with gear and other modifiers.")
                .defineInRange("spellPowerMultiplier", 1.0, 0.0, 10.0);
        BUFF_DURATION_MULTIPLIER = builder
                .comment("Multiplier applied to the duration of buff and debuff effects from Iron's Spellbooks spells. Does not affect vanilla potions, food effects, beacon effects, or effects from other mods. 1.0 leaves durations unchanged.")
                .defineInRange("buffDurationMultiplier", 1.0, 0.0, 10.0);
        BUFF_DURATION_NAMESPACES = builder
                .comment("Namespaces of mod effects scaled by buffDurationMultiplier. Defaults to Iron's Spellbooks and the known Iron's addons that register their own effects. Add other addon namespaces to scale their effects too, or remove entries to stop scaling them. Vanilla 'minecraft' is intentionally not supported and will be ignored if present.")
                .defineList("buffDurationNamespaces", List.of("irons_spellbooks", "cataclysm_spellbooks", "dacxirons", "gametechbcs_spellbooks", "gtbcs_geomancy_plus", "hazennstuff", "ias_spellbooks", "traveloptics"), () -> "irons_spellbooks", entry -> entry instanceof String namespace && namespace.matches("[a-z0-9_.-]+"));
        builder.pop();
        builder.push("restrictions");
        SPELL_CASTING_DISABLED_DIMENSIONS = builder
                .comment("Dimensions where spell casting is fully blocked. List of dimension IDs like \"minecraft:nether\" or \"twilightforest:twilight_forest\". Empty list disables this feature. Affects players only, not mob casters.")
                .defineList("spellCastingDisabledDimensions", List.of(), () -> "", entry -> entry instanceof String);
        MAX_SPELL_RARITY = builder
                .comment("Highest spell rarity players are allowed to cast. Spells with a higher minimum rarity are blocked. Valid values: common, uncommon, rare, epic, legendary, or empty to disable. For example, 'uncommon' allows common and uncommon spells but blocks rare, epic, and legendary. 'legendary' allows everything. Mob casters are not affected.")
                .define("maxSpellRarity", "");
        INSCRIPTION_BLACKLIST = builder
                .comment("Spell IDs that cannot be inscribed at the inscription table. List of spell IDs like \"irons_spellbooks:fireball\". Empty list disables this feature. Players will see their inscription cancelled silently.")
                .defineList("inscriptionBlacklist", List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
        builder.push("blackhole");
        BLACKHOLE_IMMUNITY = builder
                .comment("Per-entity-type resistance to black hole pull. Format: \"entity_id:strength\" where strength is 0.0 (no effect) to 1.0 (fully immune). Iron's hardcodes a 0.3 minimum pull regardless of KNOCKBACK_RESISTANCE attribute, so this exists to push past that floor for specific bosses. Affects black hole only, not other movement spells or vanilla knockback.")
                .defineList("blackholeImmunity", List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
        builder.push("summons");
        SUMMON_VEX_HP_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned vex's base HP. 1.0 leaves HP unchanged. Stacks multiplicatively with any HP scaling another mod applies (Apotheosis-style scalers, difficulty mods, etc).")
                .defineInRange("summonVexHpMultiplier", 1.0, 0.0, 10.0);
        SUMMON_VEX_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned vex's melee damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero.")
                .defineInRange("summonVexDamageMultiplier", 1.0, 0.0, 10.0);
        RAISE_DEAD_HP_MULTIPLIER = builder
                .comment("Multiplier applied to the base HP of Raise Dead summons. 1.0 leaves HP unchanged. Applies to both summoned skeletons and zombies.")
                .defineInRange("raiseDeadHpMultiplier", 1.0, 0.0, 10.0);
        RAISE_DEAD_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to a Raise Dead summon's melee damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero. Applies to both summoned skeletons and zombies.")
                .defineInRange("raiseDeadDamageMultiplier", 1.0, 0.0, 10.0);
        SUMMON_POLAR_BEAR_HP_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned polar bear's base HP. 1.0 leaves HP unchanged. Iron's already scales polar bear base HP with spell level and spell power before this multiplier is applied.")
                .defineInRange("summonPolarBearHpMultiplier", 1.0, 0.0, 10.0);
        SUMMON_POLAR_BEAR_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned polar bear's melee damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero.")
                .defineInRange("summonPolarBearDamageMultiplier", 1.0, 0.0, 10.0);
        SUMMON_HORSE_HP_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned horse's base HP. 1.0 leaves HP unchanged. Iron's already scales horse base HP with spell power before this multiplier is applied.")
                .defineInRange("summonHorseHpMultiplier", 1.0, 0.0, 10.0);
        SUMMON_SWORDS_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to Summon Swords damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero.")
                .defineInRange("summonSwordsDamageMultiplier", 1.0, 0.0, 10.0);
        builder.pop();
        SERVER_SPEC = builder.build();
    }
}
