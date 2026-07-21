// Permission level 2 gates the admin subcommands (grant/revoke/reset/copyconfig and status for another player). Self status, the unlocks listing, and the requirements lookup are open to all players.
package com.nightwielder.ironsspellbookstweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nightwielder.ironsspellbookstweaks.Config;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressAttachments;
import com.nightwielder.ironsspellbookstweaks.handlers.ManaAttributeHandler;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockApplicator;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockDefinition;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockDescriber;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockDescriber.UnlockStatus;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockEvaluator;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockManager;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockTrigger;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ISSTweaksCommand {

    private static final Logger logger = LogManager.getLogger("irons_spellbooks_tweaks/ISSTweaksCommand");

    private static final SuggestionProvider<CommandSourceStack> UNLOCK_ID_SUGGESTIONS = (context, builder) -> {
        Collection<ResourceLocation> ids = UnlockManager.getAll().keySet();
        return SharedSuggestionProvider.suggestResource(ids, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        Predicate<CommandSourceStack> isOp = source -> source.hasPermission(2);
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("isstweaks")
                .then(Commands.literal("grant").requires(isOp)
                        .then(Commands.argument("player", EntityArgument.players())
                                .then(Commands.argument("unlock_id", ResourceLocationArgument.id())
                                        .suggests(UNLOCK_ID_SUGGESTIONS)
                                        .executes(ISSTweaksCommand::executeGrant))))
                .then(Commands.literal("revoke").requires(isOp)
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ISSTweaksCommand::executeRevokeAll)
                                .then(Commands.argument("unlock_id", ResourceLocationArgument.id())
                                        .suggests(UNLOCK_ID_SUGGESTIONS)
                                        .executes(ISSTweaksCommand::executeRevoke))))
                .then(Commands.literal("status")
                        .executes(ISSTweaksCommand::executeStatusSelf)
                        .then(Commands.argument("player", EntityArgument.players()).requires(isOp)
                                .executes(ISSTweaksCommand::executeStatus)))
                .then(Commands.literal("unlocks")
                        .executes(ISSTweaksCommand::executeUnlocksAll)
                        .then(Commands.literal("all").executes(ISSTweaksCommand::executeUnlocksAll))
                        .then(Commands.literal("completed").executes(ISSTweaksCommand::executeUnlocksCompleted))
                        .then(Commands.literal("incomplete").executes(ISSTweaksCommand::executeUnlocksIncomplete))
                        .then(Commands.literal("in-progress").executes(ISSTweaksCommand::executeUnlocksInProgress))
                        .then(Commands.literal("not-started").executes(ISSTweaksCommand::executeUnlocksNotStarted))
                        .then(Commands.argument("unlock_id", ResourceLocationArgument.id())
                                .suggests(UNLOCK_ID_SUGGESTIONS)
                                .executes(ISSTweaksCommand::executeUnlockDetail)))
                .then(Commands.literal("reset").requires(isOp)
                        .executes(ISSTweaksCommand::executeResetSelf)
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ISSTweaksCommand::executeReset)))
                .then(Commands.literal("copyconfig").requires(isOp)
                        .executes(ISSTweaksCommand::executeCopyConfig));
        // Skip the requirements subcommand when Iron's is absent. The handler class references Iron's API types, so loading it without Iron's would fail.
        if (IronsSpellbooksCompat.isLoaded()) {
            root.then(Commands.literal("requirements")
                    .then(Commands.literal("spell")
                            .then(Commands.argument("spell", ResourceLocationArgument.id())
                                    .suggests(RequirementsCommandHandler.SPELL_ID_SUGGESTIONS)
                                    .executes(RequirementsCommandHandler::executeSpell)))
                    .then(Commands.literal("rarity")
                            .then(Commands.argument("rarity", StringArgumentType.word())
                                    .suggests(RequirementsCommandHandler.RARITY_SUGGESTIONS)
                                    .executes(RequirementsCommandHandler::executeRarity))));
        }
        dispatcher.register(root);
    }

    private static int executeGrant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        ResourceLocation unlockId = ResourceLocationArgument.getId(context, "unlock_id");
        UnlockDefinition definition = UnlockManager.getById(unlockId).orElse(null);
        if (definition == null) {
            context.getSource().sendFailure(Component.literal("unknown unlock: " + unlockId));
            return 0;
        }
        CommandSourceStack source = context.getSource();
        for (ServerPlayer player : players) {
            UnlockApplicator.apply(player, definition);
            source.sendSuccess(() -> Component.literal("granted " + unlockId + " to " + player.getName().getString()), true);
        }
        return players.size();
    }

    private static int executeRevoke(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        ResourceLocation unlockId = ResourceLocationArgument.getId(context, "unlock_id");
        CommandSourceStack source = context.getSource();
        int succeeded = 0;
        for (ServerPlayer player : players) {
            PlayerProgress progress = progressOf(player);
            if (progress == null) {
                source.sendFailure(Component.literal(player.getName().getString() + " has no isstweaks progress data"));
                continue;
            }
            if (!progress.removeUnlockGranted(unlockId)) {
                source.sendFailure(Component.literal(player.getName().getString() + " did not have unlock " + unlockId));
                continue;
            }
            progress.clearBonuses();
            dropOrphanedKillCounts(progress, unlockId);
            rebuildProgressAttributes(player, progress);
            succeeded++;
            source.sendSuccess(() -> Component.literal("revoked " + unlockId + " from " + player.getName().getString()), true);
        }
        return succeeded;
    }

    private static int executeRevokeAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        CommandSourceStack source = context.getSource();
        for (ServerPlayer player : players) {
            PlayerProgress progress = progressOf(player);
            if (progress == null) {
                source.sendFailure(Component.literal(player.getName().getString() + " has no isstweaks progress data"));
                continue;
            }
            int count = progress.getGrantedUnlocks().size();
            String noun = count == 1 ? "unlock" : "unlocks";
            progress.clearAllGrants();
            progress.clearKillCounts();
            rebuildProgressAttributes(player, progress);
            source.sendSuccess(() -> Component.literal("revoked " + count + " " + noun + " from " + player.getName().getString()), true);
        }
        return players.size();
    }

    // Recompute the additive bonuses and their attribute modifiers from whatever unlocks the player still holds. Stripping first clears any modifier whose bonus is now zero, since the refresh only touches non-zero ones.
    private static void rebuildProgressAttributes(ServerPlayer player, PlayerProgress progress) {
        ManaAttributeHandler.stripProgressModifiers(player);
        UnlockApplicator.reapplyGrants(player, progress);
    }

    // Forget the player's kills of any entity that only the just-revoked unlock referenced, so a stale count does not linger with nothing gating it. Entities another loaded unlock still needs are kept.
    private static void dropOrphanedKillCounts(PlayerProgress progress, ResourceLocation revokedId) {
        UnlockDefinition revoked = UnlockManager.getById(revokedId).orElse(null);
        if (revoked == null) {
            return;
        }
        for (ResourceLocation entityTypeId : UnlockEvaluator.referencedEntityTypes(revoked.getTrigger())) {
            if (!UnlockEvaluator.entityReferencedByOtherUnlock(entityTypeId, revokedId)) {
                progress.removeKillCount(entityTypeId);
            }
        }
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return printStatus(context.getSource(), EntityArgument.getPlayers(context, "player"));
    }

    private static int executeStatusSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return printStatus(context.getSource(), List.of(context.getSource().getPlayerOrException()));
    }

    private static int printStatus(CommandSourceStack source, Collection<ServerPlayer> players) {
        boolean first = true;
        for (ServerPlayer player : players) {
            if (!first) {
                source.sendSuccess(() -> Component.literal(""), false);
            }
            first = false;
            // attachment auto-creates on first read, so a value is always present
            printPlayerStatus(source, player, player.getData(PlayerProgressAttachments.PLAYER_PROGRESS));
        }
        return players.size();
    }

    // Only print fields that carry a value so a near-empty player reads at a glance.
    private static void printPlayerStatus(CommandSourceStack source, ServerPlayer player, PlayerProgress progress) {
        source.sendSuccess(() -> Component.literal("isstweaks status for " + player.getName().getString() + ":").withStyle(ChatFormatting.YELLOW), false);
        boolean anyShown = false;
        if (progress.getRarityCap() != null) {
            source.sendSuccess(() -> Component.literal("  Rarity cap: " + progress.getRarityCap()), false);
            anyShown = true;
        }
        if (progress.getCooldownReductionBonus() != 0.0) {
            source.sendSuccess(() -> Component.literal("  Cooldown reduction bonus: " + progress.getCooldownReductionBonus()), false);
            anyShown = true;
        }
        if (progress.getCastTimeReductionBonus() != 0.0) {
            source.sendSuccess(() -> Component.literal("  Cast time reduction bonus: " + progress.getCastTimeReductionBonus()), false);
            anyShown = true;
        }
        if (progress.getMaxManaBonus() != 0) {
            source.sendSuccess(() -> Component.literal("  Max mana bonus: " + progress.getMaxManaBonus()), false);
            anyShown = true;
        }
        if (progress.getManaRegenBonus() != 0.0) {
            source.sendSuccess(() -> Component.literal("  Mana regen bonus: " + progress.getManaRegenBonus()), false);
            anyShown = true;
        }
        if (!progress.getDimensionsRemoved().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  Dimensions removed:"), false);
            for (String dimension : sortedIds(progress.getDimensionsRemoved())) {
                source.sendSuccess(() -> Component.literal("    " + dimension), false);
            }
            anyShown = true;
        }
        if (!progress.getInscriptionsRemoved().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  Inscriptions removed:"), false);
            for (String spell : sortedIds(progress.getInscriptionsRemoved())) {
                source.sendSuccess(() -> Component.literal("    " + spell), false);
            }
            anyShown = true;
        }
        if (!progress.getGrantedUnlocks().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  Granted unlocks:"), false);
            for (ResourceLocation unlockId : sortedLocations(progress.getGrantedUnlocks())) {
                String name = UnlockManager.getById(unlockId).map(UnlockDescriber::displayName).orElse(UnlockDescriber.prettyName(unlockId));
                source.sendSuccess(() -> Component.literal("    " + name), false);
            }
            anyShown = true;
        }
        Map<ResourceLocation, Integer> killRequirements = UnlockEvaluator.uncompletedEntityRequirements(progress);
        List<Map.Entry<ResourceLocation, Integer>> gatingKills = sortedKillCounts(progress.getEntityKillCounts()).stream()
                .filter(entry -> killRequirements.containsKey(entry.getKey()))
                .toList();
        if (!gatingKills.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  Kill counts:"), false);
            for (Map.Entry<ResourceLocation, Integer> entry : gatingKills) {
                int required = killRequirements.get(entry.getKey());
                source.sendSuccess(() -> Component.literal("    " + UnlockDescriber.prettyName(entry.getKey()) + ": " + entry.getValue() + "/" + required), false);
            }
            anyShown = true;
        }
        if (!anyShown) {
            source.sendSuccess(() -> Component.literal("  nothing to show").withStyle(ChatFormatting.GRAY), false);
        }
    }

    private static List<String> sortedIds(Set<ResourceLocation> ids) {
        return ids.stream().map(ResourceLocation::toString).sorted().toList();
    }

    private static List<ResourceLocation> sortedLocations(Set<ResourceLocation> ids) {
        return ids.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private static List<Map.Entry<ResourceLocation, Integer>> sortedKillCounts(Map<ResourceLocation, Integer> counts) {
        return counts.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .toList();
    }

    private enum UnlockView { ALL, COMPLETED, INCOMPLETE, IN_PROGRESS, NOT_STARTED }

    private static int executeUnlocksAll(CommandContext<CommandSourceStack> context) {
        return listUnlocks(context.getSource(), UnlockView.ALL);
    }

    private static int executeUnlocksCompleted(CommandContext<CommandSourceStack> context) {
        return listUnlocks(context.getSource(), UnlockView.COMPLETED);
    }

    private static int executeUnlocksIncomplete(CommandContext<CommandSourceStack> context) {
        return listUnlocks(context.getSource(), UnlockView.INCOMPLETE);
    }

    private static int executeUnlocksInProgress(CommandContext<CommandSourceStack> context) {
        return listUnlocks(context.getSource(), UnlockView.IN_PROGRESS);
    }

    private static int executeUnlocksNotStarted(CommandContext<CommandSourceStack> context) {
        return listUnlocks(context.getSource(), UnlockView.NOT_STARTED);
    }

    private static int listUnlocks(CommandSourceStack source, UnlockView view) {
        Map<ResourceLocation, UnlockDefinition> all = UnlockManager.getAll();
        if (all.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No unlocks are currently loaded"), false);
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        PlayerProgress progress = player == null ? null : progressOf(player);
        List<ResourceLocation> ids = all.keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        int shown = 0;
        for (ResourceLocation id : ids) {
            UnlockDefinition unlock = all.get(id);
            boolean granted = progress != null && progress.hasUnlockGranted(id);
            if (!matchesView(view, unlock, player, progress, granted)) {
                continue;
            }
            Component line = renderUnlockLine(unlock, player, progress, granted);
            source.sendSuccess(() -> line, false);
            shown++;
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal(emptyViewMessage(view)).withStyle(ChatFormatting.GRAY), false);
        }
        return shown;
    }

    // A console or command block source has no progress to read, so the two progress views match nothing there.
    private static boolean matchesView(UnlockView view, UnlockDefinition unlock, ServerPlayer player, PlayerProgress progress, boolean granted) {
        return switch (view) {
            case ALL -> true;
            case COMPLETED -> granted;
            case INCOMPLETE -> !granted;
            case IN_PROGRESS -> progress != null
                    && UnlockDescriber.statusOf(unlock.getTrigger(), player, progress, granted) == UnlockStatus.IN_PROGRESS;
            case NOT_STARTED -> progress != null
                    && UnlockDescriber.statusOf(unlock.getTrigger(), player, progress, granted) == UnlockStatus.NOT_STARTED;
        };
    }

    private static String emptyViewMessage(UnlockView view) {
        return switch (view) {
            case COMPLETED -> "You have not completed any unlocks yet";
            case INCOMPLETE -> "You have completed every loaded unlock";
            case IN_PROGRESS -> "No unlocks in progress";
            case NOT_STARTED -> "All unlocks started";
            case ALL -> "No unlocks are currently loaded";
        };
    }

    private static Component renderUnlockLine(UnlockDefinition unlock, ServerPlayer player, PlayerProgress progress, boolean granted) {
        MutableComponent line = Component.literal("");
        if (progress != null) {
            appendStatusTag(line, unlock, player, progress, granted);
        }
        line.append(Component.literal(UnlockDescriber.displayName(unlock)).withStyle(ChatFormatting.YELLOW));
        line.append(Component.literal(": " + UnlockDescriber.requirementLine(unlock)));
        if (progress != null) {
            line.append(Component.literal(UnlockDescriber.inlineProgress(unlock.getTrigger(), player, progress)).withStyle(ChatFormatting.GRAY));
        }
        String grantSummary = UnlockDescriber.grantSummary(unlock.getGrants());
        if (!grantSummary.isEmpty()) {
            line.append(Component.literal(" | Grants: " + grantSummary));
        }
        return line;
    }

    private static void appendStatusTag(MutableComponent line, UnlockDefinition unlock, ServerPlayer player, PlayerProgress progress, boolean granted) {
        switch (UnlockDescriber.statusOf(unlock.getTrigger(), player, progress, granted)) {
            case UNLOCKED -> line.append(Component.literal("[Unlocked] ").withStyle(ChatFormatting.GREEN));
            case IN_PROGRESS -> line.append(Component.literal("[In Progress] ").withStyle(ChatFormatting.YELLOW));
            case NOT_STARTED -> line.append(Component.literal("[Not Started] ").withStyle(ChatFormatting.RED));
        }
    }

    private static int executeUnlockDetail(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation unlockId = ResourceLocationArgument.getId(context, "unlock_id");
        UnlockDefinition unlock = UnlockManager.getById(unlockId).orElse(null);
        if (unlock == null) {
            source.sendFailure(Component.literal("Unknown unlock: " + unlockId));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        PlayerProgress progress = player == null ? null : progressOf(player);
        UnlockTrigger trigger = unlock.getTrigger();
        source.sendSuccess(() -> Component.literal(UnlockDescriber.displayName(unlock) + ":").withStyle(ChatFormatting.YELLOW), false);

        List<String> lines = new ArrayList<>();
        String requirementText = unlock.getRequirementText();
        if (requirementText != null && !requirementText.isBlank()) {
            lines.add("  " + requirementText);
        } else {
            UnlockDescriber.appendDescription(trigger, "  ", lines);
        }
        String grantSummary = UnlockDescriber.grantSummary(unlock.getGrants());
        if (!grantSummary.isEmpty()) {
            lines.add("  Grants: " + grantSummary);
        }
        if (progress != null && UnlockDescriber.isComposite(trigger)) {
            lines.add("  Progress:");
            UnlockDescriber.appendChildrenProgress(trigger, "    ", player, progress, lines);
        }
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal(line), false);
        }

        if (progress != null) {
            if (progress.hasUnlockGranted(unlockId)) {
                source.sendSuccess(() -> Component.literal("[Unlocked]").withStyle(ChatFormatting.GREEN), false);
            } else {
                int[] summary = UnlockDescriber.summary(trigger, player, progress);
                ChatFormatting progressColor = summary[0] >= summary[1] ? ChatFormatting.GREEN : ChatFormatting.GRAY;
                source.sendSuccess(() -> Component.literal("  Progress: " + summary[0] + "/" + summary[1]).withStyle(progressColor), false);
            }
        }
        return 1;
    }

    private static PlayerProgress progressOf(ServerPlayer player) {
        return player.getData(PlayerProgressAttachments.PLAYER_PROGRESS);
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return resetProgress(context.getSource(), EntityArgument.getPlayers(context, "player"));
    }

    private static int executeResetSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return resetProgress(context.getSource(), List.of(context.getSource().getPlayerOrException()));
    }

    private static int resetProgress(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            PlayerProgress progress = progressOf(player);
            if (progress == null) {
                source.sendFailure(Component.literal(player.getName().getString() + " has no isstweaks progress data"));
                continue;
            }
            progress.reset();
            ManaAttributeHandler.stripProgressModifiers(player);
            source.sendSuccess(() -> Component.literal("reset isstweaks progress for " + player.getName().getString()), true);
        }
        return players.size();
    }

    private static int executeCopyConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        Path globalConfig = FMLPaths.CONFIGDIR.get().resolve(Config.SERVER_CONFIG_FILE);
        Path perWorldConfig = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(Config.SERVER_CONFIG_FILE);
        if (!Files.exists(globalConfig)) {
            source.sendFailure(Component.literal("global config file not found at " + globalConfig));
            return 0;
        }
        if (Files.exists(perWorldConfig)) {
            source.sendFailure(Component.literal("per-world config already exists at " + perWorldConfig + ". delete or rename the existing file first."));
            return 0;
        }
        try {
            Files.createDirectories(perWorldConfig.getParent());
            Files.copy(globalConfig, perWorldConfig);
        } catch (IOException copyFailed) {
            logger.warn("failed to copy global config to {}", perWorldConfig, copyFailed);
            source.sendFailure(Component.literal("failed to copy config: " + copyFailed.getMessage()));
            return 0;
        }
        Path displayPath = FMLPaths.GAMEDIR.get().relativize(perWorldConfig);
        source.sendSuccess(() -> Component.literal("copied global config to " + displayPath + ". edit the per-world copy and reload the world to apply overrides."), true);
        return 1;
    }
}
