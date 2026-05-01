// Admin command tree for inspecting and editing PlayerProgress capability state. Op-level (permission 2) on every subcommand.
package com.nightwielder.ironsspellbookstweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgressProvider;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockApplicator;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockDefinition;
import com.nightwielder.ironsspellbookstweaks.unlocks.UnlockManager;
import java.util.Collection;
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
        dispatcher.register(
                Commands.literal("isstweaks").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("unlock_id", ResourceLocationArgument.id())
                                                .suggests(UNLOCK_ID_SUGGESTIONS)
                                                .executes(ISSTweaksCommand::executeUnlock))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("unlock_id", ResourceLocationArgument.id())
                                                .suggests(UNLOCK_ID_SUGGESTIONS)
                                                .executes(ISSTweaksCommand::executeRevoke))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ISSTweaksCommand::executeStatus)))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ISSTweaksCommand::executeReset)))
        );
    }

    private static int executeUnlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation unlockId = ResourceLocationArgument.getId(context, "unlock_id");
        UnlockDefinition definition = UnlockManager.getById(unlockId).orElse(null);
        if (definition == null) {
            context.getSource().sendFailure(Component.literal("unknown unlock: " + unlockId));
            return 0;
        }
        UnlockApplicator.apply(player, definition);
        context.getSource().sendSuccess(() -> Component.literal("granted " + unlockId + " to " + player.getName().getString()), true);
        return 1;
    }

    private static int executeRevoke(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation unlockId = ResourceLocationArgument.getId(context, "unlock_id");
        boolean removed = player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                .map(progress -> progress.removeUnlockGranted(unlockId))
                .orElse(false);
        if (!removed) {
            context.getSource().sendFailure(Component.literal(player.getName().getString() + " did not have unlock " + unlockId));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("revoked " + unlockId + " from " + player.getName().getString() + " (cumulative bonuses are not undone, run reset for a clean slate)"), true);
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        // LazyOptional has no ifPresentOrElse, resolve() promotes it to a plain Optional
        player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).resolve().ifPresentOrElse(progress -> {
            CommandSourceStack source = context.getSource();
            source.sendSuccess(() -> Component.literal("isstweaks status for " + player.getName().getString() + ":"), false);
            source.sendSuccess(() -> Component.literal("  spellLevelCap: " + progress.getSpellLevelCap()), false);
            source.sendSuccess(() -> Component.literal("  cooldownReductionBonus: " + progress.getCooldownReductionBonus()), false);
            source.sendSuccess(() -> Component.literal("  castTimeReductionBonus: " + progress.getCastTimeReductionBonus()), false);
            source.sendSuccess(() -> Component.literal("  dimensionsRemoved: " + progress.getDimensionsRemoved()), false);
            source.sendSuccess(() -> Component.literal("  inscriptionsRemoved: " + progress.getInscriptionsRemoved()), false);
            source.sendSuccess(() -> Component.literal("  grantedUnlocks: " + progress.getGrantedUnlocks()), false);
        }, () -> context.getSource().sendFailure(Component.literal(player.getName().getString() + " has no isstweaks progress data")));
        return 1;
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).ifPresent(PlayerProgress::reset);
        context.getSource().sendSuccess(() -> Component.literal("reset isstweaks progress for " + player.getName().getString()), true);
        return 1;
    }
}
