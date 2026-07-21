package com.nightwielder.ironsspellbookstweaks;

import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

// Defines global server settings that a per-world serverconfig copy overrides per key.
public class Config {

    public static final String SERVER_CONFIG_FILE = "irons_spellbooks_tweaks-server.toml";

    // Section and key names shared with RuntimeConfig so the per-world override matches by constant, not by a literal that could drift.
    public static final class Keys {

        private Keys() {
        }

        public static final String SECTION_MANA = "mana";
        public static final String SECTION_COOLDOWN = "cooldown";
        public static final String SECTION_SPELLS = "spells";
        public static final String SECTION_PER_SPELL = "per_spell";
        public static final String SECTION_RESTRICTIONS = "restrictions";
        public static final String SECTION_BLACKHOLE = "blackhole";
        public static final String SECTION_SUMMONS = "summons";

        public static final String MANA_BASE_REGEN_PERCENT = "baseManaRegenPercent";
        public static final String MANA_STARTING_MAX = "startingMaxMana";
        public static final String MANA_DISABLE_REGEN = "disableManaRegen";

        public static final String COOLDOWN_REDUCTION_BONUS = "cooldownReductionBonus";
        public static final String CAST_TIME_REDUCTION_BONUS = "castTimeReductionBonus";

        public static final String SPELL_POWER_MULTIPLIER = "spellPowerMultiplier";
        public static final String BUFF_DURATION_MULTIPLIER = "buffDurationMultiplier";
        public static final String BUFF_DURATION_NAMESPACES = "buffDurationNamespaces";

        public static final String PER_SPELL_DAMAGE = "damageMultipliers";
        public static final String PER_SPELL_COOLDOWN = "cooldownMultipliers";
        public static final String PER_SPELL_MANA_COST = "manaCostMultipliers";

        public static final String SPELL_CASTING_DISABLED_DIMENSIONS = "spellCastingDisabledDimensions";
        public static final String MAX_SPELL_RARITY = "maxSpellRarity";
        public static final String INSCRIPTION_BLACKLIST = "inscriptionBlacklist";

        public static final String BLACKHOLE_IMMUNITY = "blackholeImmunity";

        public static final String SUMMON_VEX_HP = "summonVexHpMultiplier";
        public static final String SUMMON_VEX_DAMAGE = "summonVexDamageMultiplier";
        public static final String RAISE_DEAD_HP = "raiseDeadHpMultiplier";
        public static final String RAISE_DEAD_DAMAGE = "raiseDeadDamageMultiplier";
        public static final String SUMMON_POLAR_BEAR_HP = "summonPolarBearHpMultiplier";
        public static final String SUMMON_POLAR_BEAR_DAMAGE = "summonPolarBearDamageMultiplier";
        public static final String SUMMON_HORSE_HP = "summonHorseHpMultiplier";
        public static final String SUMMON_SWORDS_DAMAGE = "summonSwordsDamageMultiplier";
    }

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
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPELL_DAMAGE_MULTIPLIERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPELL_COOLDOWN_MULTIPLIERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPELL_MANA_COST_MULTIPLIERS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push(Keys.SECTION_MANA);
        BASE_MANA_REGEN_PERCENT = builder
                .comment("Bonus added to the player's MANA_REGEN attribute on every login. Stacks as a flat addition. Set to -1 to disable. Iron's vanilla default is around 1.0, so a value of 4.0 here gives an effective regen rate of 5.0.")
                .defineInRange(Keys.MANA_BASE_REGEN_PERCENT, -1.0, -1.0, 100.0);
        STARTING_MAX_MANA = builder
                .comment("Bonus added to the player's MAX_MANA attribute on every login. Stacks with the value as a flat addition. Set to -1 to disable. Iron's vanilla default is 100, so a value of 400 here gives players 500 max mana total.")
                .defineInRange(Keys.MANA_STARTING_MAX, -1, -1, 100000);
        DISABLE_MANA_REGEN = builder
                .comment("Fully disable passive mana regen. Implemented as a tick drainback because Iron's regen path does not fire a cancellable event.")
                .define(Keys.MANA_DISABLE_REGEN, false);
        builder.pop();
        builder.push(Keys.SECTION_COOLDOWN);
        COOLDOWN_REDUCTION_BONUS = builder
                .comment("Additive bonus applied to the COOLDOWN_REDUCTION attribute for every player. Around 0.5 cuts cooldowns roughly in half. Negative values lengthen them. Stacks with gear and effects.")
                .defineInRange(Keys.COOLDOWN_REDUCTION_BONUS, 0.0, -10.0, 10.0);
        CAST_TIME_REDUCTION_BONUS = builder
                .comment("Additive bonus applied to the CAST_TIME_REDUCTION attribute for every player. Around 0.5 makes spells cast roughly twice as fast. Negative values lengthen cast times. Stacks with gear and effects.")
                .defineInRange(Keys.CAST_TIME_REDUCTION_BONUS, 0.0, -10.0, 10.0);
        builder.pop();
        builder.push(Keys.SECTION_SPELLS);
        SPELL_POWER_MULTIPLIER = builder
                .comment("Multiplier applied to every player's SPELL_POWER attribute. 1.0 leaves spell power unchanged. Stacks multiplicatively with gear and other modifiers.")
                .defineInRange(Keys.SPELL_POWER_MULTIPLIER, 1.0, 0.0, 10.0);
        BUFF_DURATION_MULTIPLIER = builder
                .comment("Multiplier applied to the duration of buff and debuff effects from Iron's Spellbooks spells. Does not affect vanilla potions, food effects, beacon effects, or effects from other mods. 1.0 leaves durations unchanged.")
                .defineInRange(Keys.BUFF_DURATION_MULTIPLIER, 1.0, 0.0, 10.0);
        BUFF_DURATION_NAMESPACES = builder
                .comment("Namespaces of mod effects scaled by buffDurationMultiplier. Defaults to Iron's Spellbooks and the known Iron's addons that register their own effects. Add other addon namespaces to scale their effects too, or remove entries to stop scaling them. Vanilla 'minecraft' is intentionally not supported and will be ignored if present.")
                .defineList(Keys.BUFF_DURATION_NAMESPACES, List.of(IronsSpellbooksCompat.IRONS_MOD_ID, "cataclysm_spellbooks", "dacxirons", "gametechbcs_spellbooks", "gtbcs_geomancy_plus", "hazennstuff", "ias_spellbooks", "traveloptics"), () -> IronsSpellbooksCompat.IRONS_MOD_ID, entry -> entry instanceof String namespace && namespace.matches("[a-z0-9_.-]+"));
        builder.pop();
        builder.push(Keys.SECTION_PER_SPELL);
        SPELL_DAMAGE_MULTIPLIERS = builder
                .comment("Per-spell damage multipliers. Format: \"spell_id:multiplier\" like \"irons_spellbooks:fireball:1.5\". Multiplies the damage that the named spell deals, and stacks on top of spellPowerMultiplier. 1.0 leaves a spell unchanged. Entries with an unknown or unparseable spell id are skipped. Multipliers are clamped to the range 0.0 to 10.0.")
                .defineList(Keys.PER_SPELL_DAMAGE, List.of(), () -> "", entry -> entry instanceof String);
        SPELL_COOLDOWN_MULTIPLIERS = builder
                .comment("Per-spell cooldown multipliers. Format: \"spell_id:multiplier\" like \"irons_spellbooks:fireball:0.5\". Below 1.0 shortens the cooldown, above 1.0 lengthens it, and 1.0 leaves it unchanged. This is a separate factor from the player's COOLDOWN_REDUCTION attribute, so it multiplies the cooldown that already has that attribute applied. Multipliers are clamped to the range 0.0 to 10.0.")
                .defineList(Keys.PER_SPELL_COOLDOWN, List.of(), () -> "", entry -> entry instanceof String);
        SPELL_MANA_COST_MULTIPLIERS = builder
                .comment("Per-spell mana cost multipliers. Format: \"spell_id:multiplier\" like \"irons_spellbooks:fireball:0.75\". Below 1.0 makes a spell cheaper, above 1.0 more expensive, and 1.0 leaves it unchanged. This stacks on top of Iron's own per-spell mana_cost_multiplier config and multiplies the cost it produces. Multipliers are clamped to the range 0.0 to 10.0.")
                .defineList(Keys.PER_SPELL_MANA_COST, List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
        builder.push(Keys.SECTION_RESTRICTIONS);
        SPELL_CASTING_DISABLED_DIMENSIONS = builder
                .comment("Dimensions where spell casting is fully blocked. List of dimension IDs like \"minecraft:nether\" or \"twilightforest:twilight_forest\". Empty list disables this feature. Affects players only, not mob casters.")
                .defineList(Keys.SPELL_CASTING_DISABLED_DIMENSIONS, List.of(), () -> "", entry -> entry instanceof String);
        MAX_SPELL_RARITY = builder
                .comment("Highest spell rarity players are allowed to cast. Spells with a higher minimum rarity are blocked. Valid values: common, uncommon, rare, epic, legendary, or empty to disable. For example, 'uncommon' allows common and uncommon spells but blocks rare, epic, and legendary. 'legendary' allows everything. Mob casters are not affected.")
                .define(Keys.MAX_SPELL_RARITY, "");
        INSCRIPTION_BLACKLIST = builder
                .comment("Spell IDs that cannot be inscribed at the inscription table. List of spell IDs like \"irons_spellbooks:fireball\". Empty list disables this feature. Players will see their inscription cancelled silently.")
                .defineList(Keys.INSCRIPTION_BLACKLIST, List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
        builder.push(Keys.SECTION_BLACKHOLE);
        BLACKHOLE_IMMUNITY = builder
                .comment("Per-entity-type resistance to black hole pull. Format: \"entity_id:strength\" where strength is 0.0 (no effect) to 1.0 (fully immune). Iron's hardcodes a 0.3 minimum pull regardless of KNOCKBACK_RESISTANCE attribute, so this exists to push past that floor for specific bosses. Affects black hole only, not other movement spells or vanilla knockback.")
                .defineList(Keys.BLACKHOLE_IMMUNITY, List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
        builder.push(Keys.SECTION_SUMMONS);
        SUMMON_VEX_HP_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned vex's base HP. 1.0 leaves HP unchanged. Stacks multiplicatively with any HP scaling another mod applies (Apotheosis-style scalers, difficulty mods, etc).")
                .defineInRange(Keys.SUMMON_VEX_HP, 1.0, 0.0, 10.0);
        SUMMON_VEX_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned vex's melee damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero.")
                .defineInRange(Keys.SUMMON_VEX_DAMAGE, 1.0, 0.0, 10.0);
        RAISE_DEAD_HP_MULTIPLIER = builder
                .comment("Multiplier applied to the base HP of Raise Dead summons. 1.0 leaves HP unchanged. Applies to both summoned skeletons and zombies.")
                .defineInRange(Keys.RAISE_DEAD_HP, 1.0, 0.0, 10.0);
        RAISE_DEAD_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to a Raise Dead summon's melee damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero. Applies to both summoned skeletons and zombies.")
                .defineInRange(Keys.RAISE_DEAD_DAMAGE, 1.0, 0.0, 10.0);
        SUMMON_POLAR_BEAR_HP_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned polar bear's base HP. 1.0 leaves HP unchanged. Iron's already scales polar bear base HP with spell level and spell power before this multiplier is applied.")
                .defineInRange(Keys.SUMMON_POLAR_BEAR_HP, 1.0, 0.0, 10.0);
        SUMMON_POLAR_BEAR_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned polar bear's melee damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero.")
                .defineInRange(Keys.SUMMON_POLAR_BEAR_DAMAGE, 1.0, 0.0, 10.0);
        SUMMON_HORSE_HP_MULTIPLIER = builder
                .comment("Multiplier applied to a summoned horse's base HP. 1.0 leaves HP unchanged. Iron's already scales horse base HP with spell power before this multiplier is applied.")
                .defineInRange(Keys.SUMMON_HORSE_HP, 1.0, 0.0, 10.0);
        SUMMON_SWORDS_DAMAGE_MULTIPLIER = builder
                .comment("Multiplier applied to Summon Swords damage per hit. 1.0 leaves damage unchanged. 0.0 reduces every hit to zero.")
                .defineInRange(Keys.SUMMON_SWORDS_DAMAGE, 1.0, 0.0, 10.0);
        builder.pop();
        SERVER_SPEC = builder.build();
    }
}
