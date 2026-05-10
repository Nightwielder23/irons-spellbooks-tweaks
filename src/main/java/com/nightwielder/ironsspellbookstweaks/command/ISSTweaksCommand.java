// Command tree for /isstweaks. Admin subcommands (grant/revoke/status/reset) require permission level 2; the requirements lookup is open to all players when registered.
package com.nightwielder.ironsspellbookstweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockApplicator;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockDefinition;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockManager;
import com.nightwielder.ironsspellbookstweaks.util.IronsSpellbooksCompat;
import java.util.Collection;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ISSTweaksCommand {

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
                                .then(Commands.argument("unlock_id", ResourceLocationArgument.id())
                                        .suggests(UNLOCK_ID_SUGGESTIONS)
                                        .executes(ISSTweaksCommand::executeRevoke))))
                .then(Commands.literal("status").requires(isOp)
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ISSTweaksCommand::executeStatus)))
                .then(Commands.literal("reset").requires(isOp)
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ISSTweaksCommand::executeReset)));
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
            boolean removed = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                    .map(progress -> progress.removeUnlockGranted(unlockId))
                    .orElse(false);
            if (removed) {
                succeeded++;
                source.sendSuccess(() -> Component.literal("revoked " + unlockId + " from " + player.getName().getString() + " (cumulative bonuses are not undone, run reset for a clean slate)"), true);
            } else {
                source.sendFailure(Component.literal(player.getName().getString() + " did not have unlock " + unlockId));
            }
        }
        return succeeded;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        CommandSourceStack source = context.getSource();
        boolean first = true;
        for (ServerPlayer player : players) {
            if (!first) {
                source.sendSuccess(() -> Component.literal(""), false);
            }
            first = false;
            // LazyOptional has no ifPresentOrElse, resolve() promotes it to a plain Optional
            player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).resolve().ifPresentOrElse(progress -> {
                source.sendSuccess(() -> Component.literal("isstweaks status for " + player.getName().getString() + ":"), false);
                source.sendSuccess(() -> Component.literal("  rarityCap: " + (progress.getRarityCap() == null ? "(none)" : progress.getRarityCap())), false);
                source.sendSuccess(() -> Component.literal("  cooldownReductionBonus: " + progress.getCooldownReductionBonus()), false);
                source.sendSuccess(() -> Component.literal("  castTimeReductionBonus: " + progress.getCastTimeReductionBonus()), false);
                source.sendSuccess(() -> Component.literal("  maxManaBonus: " + progress.getMaxManaBonus()), false);
                source.sendSuccess(() -> Component.literal("  manaRegenBonus: " + progress.getManaRegenBonus()), false);
                source.sendSuccess(() -> Component.literal("  dimensionsRemoved: " + progress.getDimensionsRemoved()), false);
                source.sendSuccess(() -> Component.literal("  inscriptionsRemoved: " + progress.getInscriptionsRemoved()), false);
                source.sendSuccess(() -> Component.literal("  grantedUnlocks: " + progress.getGrantedUnlocks()), false);
            }, () -> source.sendFailure(Component.literal(player.getName().getString() + " has no isstweaks progress data")));
        }
        return players.size();
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        CommandSourceStack source = context.getSource();
        for (ServerPlayer player : players) {
            player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(PlayerProgress::reset);
            source.sendSuccess(() -> Component.literal("reset isstweaks progress for " + player.getName().getString()), true);
        }
        return players.size();
    }
}
