// Backing logic for /isstweaks requirements. Kept in its own class so the Iron's imports do not leak into ISSTweaksCommand, which is loaded unconditionally.
package com.nightwielder.ironsspellbookstweaks.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockDefinition;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RequirementsCommandHandler {

    private static final String[] RARITY_NAMES = {"common", "uncommon", "rare", "epic", "legendary"};

    public static final SuggestionProvider<CommandSourceStack> SPELL_ID_SUGGESTIONS = (context, builder) -> {
        List<ResourceLocation> ids = SpellRegistry.getEnabledSpells().stream()
                .map(AbstractSpell::getSpellResource)
                .toList();
        return SharedSuggestionProvider.suggestResource(ids, builder);
    };

    public static final SuggestionProvider<CommandSourceStack> RARITY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(RARITY_NAMES, builder);

    private RequirementsCommandHandler() {
    }

    public static int executeSpell(CommandContext<CommandSourceStack> context) {
        ResourceLocation spellId = ResourceLocationArgument.getId(context, "spell");
        CommandSourceStack source = context.getSource();
        // SpellRegistry.getSpell falls back to NoneSpell instead of returning null, so check the registry directly
        if (!SpellRegistry.REGISTRY.containsKey(spellId)) {
            source.sendFailure(Component.literal("Unknown spell: " + spellId));
            return 0;
        }
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        Component displayName = resolveDisplayName(spell, spellId);
        Optional<UnlockDefinition> unlock = UnlockManager.findUnlockForInscription(spellId);
        if (unlock.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Spell ").append(displayName).append(Component.literal(" is not locked")), false);
            return 1;
        }
        String requirementText = unlock.get().getRequirementText();
        if (requirementText == null || requirementText.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Spell ").append(displayName).append(Component.literal(" is locked but no requirement text was provided")), false);
            return 1;
        }
        boolean unlocked = isSpellUnlockedFor(source, spellId);
        source.sendSuccess(() -> withUnlockPrefix(unlocked)
                .append(Component.literal("To unlock "))
                .append(displayName)
                .append(Component.literal(": " + requirementText)), false);
        return 1;
    }

    public static int executeRarity(CommandContext<CommandSourceStack> context) {
        String rawRarity = StringArgumentType.getString(context, "rarity").toLowerCase();
        CommandSourceStack source = context.getSource();
        if (!isKnownRarity(rawRarity)) {
            source.sendFailure(Component.literal("Unknown rarity: " + rawRarity));
            return 0;
        }
        SpellRarity rarity = SpellRarity.valueOf(rawRarity.toUpperCase());
        Component rarityLabel = buildRarityLabel(rarity, rawRarity);
        boolean unlocked = isRarityUnlockedFor(source, rarity);
        List<UnlockDefinition> matches = findUnlocksForRarity(rarity);
        if (matches.isEmpty()) {
            // empty root so the trailing text does not inherit the rarity color
            source.sendSuccess(() -> Component.literal("").append(rarityLabel).append(Component.literal(" spells are not gated by any unlock")), false);
            return 1;
        }
        if (matches.size() == 1) {
            UnlockDefinition only = matches.get(0);
            String requirementText = only.getRequirementText();
            if (requirementText == null || requirementText.isEmpty()) {
                source.sendSuccess(() -> Component.literal("").append(rarityLabel).append(Component.literal(" spells are locked but no requirement text was provided")), false);
                return 1;
            }
            source.sendSuccess(() -> withUnlockPrefix(unlocked)
                    .append(Component.literal("To unlock "))
                    .append(rarityLabel)
                    .append(Component.literal(" spells: " + requirementText)), false);
            return 1;
        }
        // Multiple unlocks share the same rarity cap. Show one line per unlock so the player can see all paths.
        source.sendSuccess(() -> withUnlockPrefix(unlocked)
                .append(rarityLabel)
                .append(Component.literal(" spells can be unlocked through any of:")), false);
        for (UnlockDefinition match : matches) {
            String requirementText = match.getRequirementText();
            String line = "  " + match.getId() + ": " + (requirementText == null || requirementText.isEmpty() ? "(no requirement text)" : requirementText);
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return matches.size();
    }

    private static boolean isKnownRarity(String value) {
        for (String name : RARITY_NAMES) {
            if (name.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static List<UnlockDefinition> findUnlocksForRarity(SpellRarity rarity) {
        String target = rarity.name();
        List<UnlockDefinition> matches = new ArrayList<>();
        for (Map.Entry<ResourceLocation, UnlockDefinition> entry : UnlockManager.getAll().entrySet()) {
            String cap = entry.getValue().getGrants().getRarityCap();
            if (cap != null && cap.equals(target)) {
                matches.add(entry.getValue());
            }
        }
        return matches;
    }

    // Returns Optional.empty() for non-player sources (console, command blocks). Player sources always have a progress attachment, auto-created on first read.
    private static Optional<PlayerProgress> progressFor(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(player.getData(PlayerProgressAttachments.PLAYER_PROGRESS));
    }

    private static boolean isSpellUnlockedFor(CommandSourceStack source, ResourceLocation spellId) {
        return progressFor(source)
                .map(progress -> progress.getInscriptionsRemoved().contains(spellId))
                .orElse(false);
    }

    // Mirrors SpellRarityGateHandler: a player meets the rarity bar when their cap is at or above the queried tier.
    private static boolean isRarityUnlockedFor(CommandSourceStack source, SpellRarity queried) {
        String capName = progressFor(source).map(PlayerProgress::getRarityCap).orElse(null);
        if (capName == null) {
            return false;
        }
        SpellRarity playerCap;
        try {
            playerCap = SpellRarity.valueOf(capName);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        return playerCap.compareRarity(queried) >= 0;
    }

    // Empty literal root with the green tag as a sibling so subsequent appends do not inherit the green color.
    private static MutableComponent withUnlockPrefix(boolean unlocked) {
        MutableComponent root = Component.literal("");
        if (unlocked) {
            root.append(Component.literal("[Unlocked] ").withStyle(ChatFormatting.GREEN));
        }
        return root;
    }

    private static Component buildRarityLabel(SpellRarity rarity, String lowerName) {
        String capitalized = Character.toUpperCase(lowerName.charAt(0)) + lowerName.substring(1);
        ChatFormatting color = rarity.getChatFormatting();
        if (color == null) {
            return Component.literal(capitalized);
        }
        return Component.literal(capitalized).withStyle(color);
    }

    // getDisplayName shows some addon spells in the galactic font. build the translation key by hand instead. school color is just a visual cue.
    private static Component resolveDisplayName(AbstractSpell spell, ResourceLocation spellId) {
        try {
            String spellName = spell.getSpellName();
            MutableComponent name = Component.translatable("spell." + spellId.getNamespace() + "." + spellName);
            Style schoolStyle = spell.getSchoolType().getDisplayName().getStyle();
            if (schoolStyle.getColor() != null) {
                return name.withStyle(Style.EMPTY.withColor(schoolStyle.getColor()));
            }
            return name;
        } catch (Exception fallback) {
            return Component.literal(spellId.toString());
        }
    }
}
