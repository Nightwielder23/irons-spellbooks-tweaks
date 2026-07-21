// Overlays global config values with a per-world serverconfig file when one is present.
package com.nightwielder.ironsspellbookstweaks.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RuntimeConfig {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/RuntimeConfig");

    public static volatile double baseManaRegenPercent = -1.0;
    public static volatile int startingMaxMana = -1;
    public static volatile boolean disableManaRegen = false;
    public static volatile double cooldownReductionBonus = 0.0;
    public static volatile double castTimeReductionBonus = 0.0;
    public static volatile double spellPowerMultiplier = 1.0;
    public static volatile double buffDurationMultiplier = 1.0;
    public static volatile Set<String> buffDurationNamespaces = Set.of("irons_spellbooks");
    public static volatile List<String> spellCastingDisabledDimensions = List.of();
    public static volatile String maxSpellRarity = "";
    public static volatile List<String> inscriptionBlacklist = List.of();
    public static volatile List<String> blackholeImmunity = List.of();
    public static volatile List<String> spellDamageMultipliersRaw = List.of();
    public static volatile List<String> spellCooldownMultipliersRaw = List.of();
    public static volatile List<String> spellManaCostMultipliersRaw = List.of();
    public static volatile double summonVexHpMultiplier = 1.0;
    public static volatile double summonVexDamageMultiplier = 1.0;
    public static volatile double raiseDeadHpMultiplier = 1.0;
    public static volatile double raiseDeadDamageMultiplier = 1.0;
    public static volatile double summonPolarBearHpMultiplier = 1.0;
    public static volatile double summonPolarBearDamageMultiplier = 1.0;
    public static volatile double summonHorseHpMultiplier = 1.0;
    public static volatile double summonSwordsDamageMultiplier = 1.0;

    public static volatile ScalingConfig scaling;
    public static volatile Map<ResourceLocation, Double> blackholeImmunityMap = Map.of();
    public static volatile Map<String, Double> spellDamageMultipliers = Map.of();
    public static volatile Map<String, Double> spellCooldownMultipliers = Map.of();
    public static volatile Map<String, Double> spellManaCostMultipliers = Map.of();

    static {
        rebuildDerived();
    }

    private RuntimeConfig() {
    }

    public static void resolve(MinecraftServer server) {
        applyGlobal();
        Path overrideFile = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(Config.SERVER_CONFIG_FILE);
        if (Files.exists(overrideFile)) {
            overlayFromFile(overrideFile);
        }
        rebuildDerived();
    }

    public static void resetToGlobal() {
        applyGlobal();
        rebuildDerived();
    }

    private static void applyGlobal() {
        baseManaRegenPercent = clamp(Config.BASE_MANA_REGEN_PERCENT.get(), -1.0, 100.0);
        startingMaxMana = clampInt(Config.STARTING_MAX_MANA.get(), -1, 100000);
        disableManaRegen = Config.DISABLE_MANA_REGEN.get();
        cooldownReductionBonus = clamp(Config.COOLDOWN_REDUCTION_BONUS.get(), -10.0, 10.0);
        castTimeReductionBonus = clamp(Config.CAST_TIME_REDUCTION_BONUS.get(), -10.0, 10.0);
        spellPowerMultiplier = clamp(Config.SPELL_POWER_MULTIPLIER.get(), 0.0, 10.0);
        buffDurationMultiplier = clamp(Config.BUFF_DURATION_MULTIPLIER.get(), 0.0, 10.0);
        buffDurationNamespaces = namespaceSet(Config.BUFF_DURATION_NAMESPACES.get());
        spellCastingDisabledDimensions = copyStrings(Config.SPELL_CASTING_DISABLED_DIMENSIONS.get());
        maxSpellRarity = normalizeRarity(Config.MAX_SPELL_RARITY.get());
        inscriptionBlacklist = copyStrings(Config.INSCRIPTION_BLACKLIST.get());
        blackholeImmunity = copyStrings(Config.BLACKHOLE_IMMUNITY.get());
        spellDamageMultipliersRaw = copyStrings(Config.SPELL_DAMAGE_MULTIPLIERS.get());
        spellCooldownMultipliersRaw = copyStrings(Config.SPELL_COOLDOWN_MULTIPLIERS.get());
        spellManaCostMultipliersRaw = copyStrings(Config.SPELL_MANA_COST_MULTIPLIERS.get());
        summonVexHpMultiplier = clamp(Config.SUMMON_VEX_HP_MULTIPLIER.get(), 0.0, 10.0);
        summonVexDamageMultiplier = clamp(Config.SUMMON_VEX_DAMAGE_MULTIPLIER.get(), 0.0, 10.0);
        raiseDeadHpMultiplier = clamp(Config.RAISE_DEAD_HP_MULTIPLIER.get(), 0.0, 10.0);
        raiseDeadDamageMultiplier = clamp(Config.RAISE_DEAD_DAMAGE_MULTIPLIER.get(), 0.0, 10.0);
        summonPolarBearHpMultiplier = clamp(Config.SUMMON_POLAR_BEAR_HP_MULTIPLIER.get(), 0.0, 10.0);
        summonPolarBearDamageMultiplier = clamp(Config.SUMMON_POLAR_BEAR_DAMAGE_MULTIPLIER.get(), 0.0, 10.0);
        summonHorseHpMultiplier = clamp(Config.SUMMON_HORSE_HP_MULTIPLIER.get(), 0.0, 10.0);
        summonSwordsDamageMultiplier = clamp(Config.SUMMON_SWORDS_DAMAGE_MULTIPLIER.get(), 0.0, 10.0);
    }

    private static void overlayFromFile(Path overrideFile) {
        try (CommentedFileConfig file = CommentedFileConfig.builder(overrideFile).build()) {
            file.load();
            baseManaRegenPercent = overlayDouble(file, List.of(Config.Keys.SECTION_MANA, Config.Keys.MANA_BASE_REGEN_PERCENT), baseManaRegenPercent, -1.0, 100.0);
            startingMaxMana = overlayInt(file, List.of(Config.Keys.SECTION_MANA, Config.Keys.MANA_STARTING_MAX), startingMaxMana, -1, 100000);
            disableManaRegen = overlayBoolean(file, List.of(Config.Keys.SECTION_MANA, Config.Keys.MANA_DISABLE_REGEN), disableManaRegen);
            cooldownReductionBonus = overlayDouble(file, List.of(Config.Keys.SECTION_COOLDOWN, Config.Keys.COOLDOWN_REDUCTION_BONUS), cooldownReductionBonus, -10.0, 10.0);
            castTimeReductionBonus = overlayDouble(file, List.of(Config.Keys.SECTION_COOLDOWN, Config.Keys.CAST_TIME_REDUCTION_BONUS), castTimeReductionBonus, -10.0, 10.0);
            spellPowerMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SPELLS, Config.Keys.SPELL_POWER_MULTIPLIER), spellPowerMultiplier, 0.0, 10.0);
            buffDurationMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SPELLS, Config.Keys.BUFF_DURATION_MULTIPLIER), buffDurationMultiplier, 0.0, 10.0);
            buffDurationNamespaces = namespaceSet(overlayStrings(file, List.of(Config.Keys.SECTION_SPELLS, Config.Keys.BUFF_DURATION_NAMESPACES), List.copyOf(buffDurationNamespaces)));
            spellCastingDisabledDimensions = overlayStrings(file, List.of(Config.Keys.SECTION_RESTRICTIONS, Config.Keys.SPELL_CASTING_DISABLED_DIMENSIONS), spellCastingDisabledDimensions);
            maxSpellRarity = normalizeRarity(overlayString(file, List.of(Config.Keys.SECTION_RESTRICTIONS, Config.Keys.MAX_SPELL_RARITY), maxSpellRarity));
            inscriptionBlacklist = overlayStrings(file, List.of(Config.Keys.SECTION_RESTRICTIONS, Config.Keys.INSCRIPTION_BLACKLIST), inscriptionBlacklist);
            blackholeImmunity = overlayStrings(file, List.of(Config.Keys.SECTION_BLACKHOLE, Config.Keys.BLACKHOLE_IMMUNITY), blackholeImmunity);
            spellDamageMultipliersRaw = overlayStrings(file, List.of(Config.Keys.SECTION_PER_SPELL, Config.Keys.PER_SPELL_DAMAGE), spellDamageMultipliersRaw);
            spellCooldownMultipliersRaw = overlayStrings(file, List.of(Config.Keys.SECTION_PER_SPELL, Config.Keys.PER_SPELL_COOLDOWN), spellCooldownMultipliersRaw);
            spellManaCostMultipliersRaw = overlayStrings(file, List.of(Config.Keys.SECTION_PER_SPELL, Config.Keys.PER_SPELL_MANA_COST), spellManaCostMultipliersRaw);
            summonVexHpMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.SUMMON_VEX_HP), summonVexHpMultiplier, 0.0, 10.0);
            summonVexDamageMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.SUMMON_VEX_DAMAGE), summonVexDamageMultiplier, 0.0, 10.0);
            raiseDeadHpMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.RAISE_DEAD_HP), raiseDeadHpMultiplier, 0.0, 10.0);
            raiseDeadDamageMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.RAISE_DEAD_DAMAGE), raiseDeadDamageMultiplier, 0.0, 10.0);
            summonPolarBearHpMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.SUMMON_POLAR_BEAR_HP), summonPolarBearHpMultiplier, 0.0, 10.0);
            summonPolarBearDamageMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.SUMMON_POLAR_BEAR_DAMAGE), summonPolarBearDamageMultiplier, 0.0, 10.0);
            summonHorseHpMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.SUMMON_HORSE_HP), summonHorseHpMultiplier, 0.0, 10.0);
            summonSwordsDamageMultiplier = overlayDouble(file, List.of(Config.Keys.SECTION_SUMMONS, Config.Keys.SUMMON_SWORDS_DAMAGE), summonSwordsDamageMultiplier, 0.0, 10.0);
            logger.info("applied per-world config override from {}", overrideFile);
        } catch (Exception parseFailed) {
            applyGlobal();
            logger.warn("could not read per-world config override at {}, so global values stay in effect", overrideFile, parseFailed);
        }
    }

    private static void rebuildDerived() {
        scaling = new ScalingConfig(
                summonVexHpMultiplier, summonVexDamageMultiplier,
                raiseDeadHpMultiplier, raiseDeadDamageMultiplier,
                summonPolarBearHpMultiplier, summonPolarBearDamageMultiplier,
                summonHorseHpMultiplier, summonSwordsDamageMultiplier);
        blackholeImmunityMap = parseImmunityList(blackholeImmunity);
        spellDamageMultipliers = parseMultiplierList(Config.Keys.PER_SPELL_DAMAGE, spellDamageMultipliersRaw);
        spellCooldownMultipliers = parseMultiplierList(Config.Keys.PER_SPELL_COOLDOWN, spellCooldownMultipliersRaw);
        spellManaCostMultipliers = parseMultiplierList(Config.Keys.PER_SPELL_MANA_COST, spellManaCostMultipliersRaw);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> copyStrings(List<? extends String> source) {
        return List.copyOf(source);
    }

    // Drop blank entries and the vanilla namespace, which is never scaled.
    private static Set<String> namespaceSet(List<? extends String> source) {
        Set<String> result = new LinkedHashSet<>();
        for (String namespace : source) {
            if (namespace == null || namespace.isBlank() || namespace.equals("minecraft")) {
                continue;
            }
            result.add(namespace);
        }
        return Set.copyOf(result);
    }

    private static String normalizeRarity(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (PlayerProgress.RARITY_RANKS.containsKey(upper)) {
            return upper;
        }
        logger.warn("maxSpellRarity '{}' is not a valid rarity, so the gate stays disabled until corrected", raw);
        return "";
    }

    private static double overlayDouble(CommentedFileConfig file, List<String> path, double fallback, double min, double max) {
        if (!file.contains(path)) {
            return fallback;
        }
        Object raw = file.get(path);
        if (raw instanceof Number number) {
            return clamp(number.doubleValue(), min, max);
        }
        logger.warn("per-world override '{}' is not a number, so the global value stays in effect", String.join(".", path));
        return fallback;
    }

    private static int overlayInt(CommentedFileConfig file, List<String> path, int fallback, int min, int max) {
        if (!file.contains(path)) {
            return fallback;
        }
        Object raw = file.get(path);
        if (raw instanceof Number number) {
            return clampInt(number.intValue(), min, max);
        }
        logger.warn("per-world override '{}' is not a number, so the global value stays in effect", String.join(".", path));
        return fallback;
    }

    private static boolean overlayBoolean(CommentedFileConfig file, List<String> path, boolean fallback) {
        if (!file.contains(path)) {
            return fallback;
        }
        Object raw = file.get(path);
        if (raw instanceof Boolean flag) {
            return flag;
        }
        logger.warn("per-world override '{}' is not a boolean, so the global value stays in effect", String.join(".", path));
        return fallback;
    }

    private static String overlayString(CommentedFileConfig file, List<String> path, String fallback) {
        if (!file.contains(path)) {
            return fallback;
        }
        Object raw = file.get(path);
        if (raw instanceof String text) {
            return text;
        }
        logger.warn("per-world override '{}' is not a string, so the global value stays in effect", String.join(".", path));
        return fallback;
    }

    private static List<String> overlayStrings(CommentedFileConfig file, List<String> path, List<String> fallback) {
        if (!file.contains(path)) {
            return fallback;
        }
        Object raw = file.get(path);
        if (!(raw instanceof List<?> rawList)) {
            logger.warn("per-world override '{}' is not a list, so the global value stays in effect", String.join(".", path));
            return fallback;
        }
        List<String> collected = new ArrayList<>();
        for (Object element : rawList) {
            if (element instanceof String text) {
                collected.add(text);
            }
        }
        return List.copyOf(collected);
    }

    private static Map<ResourceLocation, Double> parseImmunityList(List<String> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Double> result = new HashMap<>();
        for (String entry : raw) {
            // Take the strength from after the LAST colon since entity ids already contain one colon.
            int separator = entry.lastIndexOf(':');
            if (separator < 0) {
                logger.warn("blackhole immunity entry '{}' missing strength suffix, skipping", entry);
                continue;
            }
            String idPart = entry.substring(0, separator);
            String strengthPart = entry.substring(separator + 1);
            ResourceLocation entityId = ResourceLocation.tryParse(idPart);
            if (entityId == null) {
                logger.warn("blackhole immunity entry '{}' has invalid entity id '{}', skipping", entry, idPart);
                continue;
            }
            double strength;
            try {
                strength = Double.parseDouble(strengthPart);
            } catch (NumberFormatException numberFailed) {
                logger.warn("blackhole immunity entry '{}' has non-numeric strength '{}', skipping", entry, strengthPart);
                continue;
            }
            double clamped = Math.max(0.0, Math.min(1.0, strength));
            result.put(entityId, clamped);
        }
        return Map.copyOf(result);
    }

    private static Map<String, Double> parseMultiplierList(String label, List<String> raw) {
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> result = new HashMap<>();
        for (String entry : raw) {
            // The multiplier comes after the last colon because spell ids already contain one.
            int separator = entry.lastIndexOf(':');
            if (separator < 0) {
                logger.warn("{} entry '{}' missing ':multiplier' suffix, skipping", label, entry);
                continue;
            }
            String idPart = entry.substring(0, separator);
            String multiplierPart = entry.substring(separator + 1);
            ResourceLocation spellId = ResourceLocation.tryParse(idPart);
            if (spellId == null) {
                logger.warn("{} entry '{}' has invalid spell id '{}', skipping", label, entry, idPart);
                continue;
            }
            double multiplier;
            try {
                multiplier = Double.parseDouble(multiplierPart);
            } catch (NumberFormatException notANumber) {
                logger.warn("{} entry '{}' has non-numeric multiplier '{}', skipping", label, entry, multiplierPart);
                continue;
            }
            result.put(spellId.toString(), clamp(multiplier, 0.0, 10.0));
        }
        return Map.copyOf(result);
    }

    public static final class ScalingConfig {
        public final double vexHpMultiplier;
        public final double vexDamageMultiplier;
        public final double raiseDeadHpMultiplier;
        public final double raiseDeadDamageMultiplier;
        public final double polarBearHpMultiplier;
        public final double polarBearDamageMultiplier;
        public final double horseHpMultiplier;
        public final double swordsDamageMultiplier;

        private ScalingConfig(double vexHpMultiplier, double vexDamageMultiplier,
                              double raiseDeadHpMultiplier, double raiseDeadDamageMultiplier,
                              double polarBearHpMultiplier, double polarBearDamageMultiplier,
                              double horseHpMultiplier, double swordsDamageMultiplier) {
            this.vexHpMultiplier = vexHpMultiplier;
            this.vexDamageMultiplier = vexDamageMultiplier;
            this.raiseDeadHpMultiplier = raiseDeadHpMultiplier;
            this.raiseDeadDamageMultiplier = raiseDeadDamageMultiplier;
            this.polarBearHpMultiplier = polarBearHpMultiplier;
            this.polarBearDamageMultiplier = polarBearDamageMultiplier;
            this.horseHpMultiplier = horseHpMultiplier;
            this.swordsDamageMultiplier = swordsDamageMultiplier;
        }
    }
}
