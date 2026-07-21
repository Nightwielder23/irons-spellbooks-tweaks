// Turns an unlock's trigger into player-facing requirement text and progress, for the /isstweaks unlocks command.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import com.nightwielder.ironsspellbookstweaks.capability.PlayerProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class UnlockDescriber {

    private UnlockDescriber() {
    }

    // The display_name override wins; without it the unlock id gets prettified.
    public static String displayName(UnlockDefinition unlock) {
        String override = unlock.getDisplayName();
        if (override != null && !override.isBlank()) {
            return override;
        }
        return prettyName(unlock.getId());
    }

    // Drops the namespace, turns underscores into spaces and slashes into " - ", then title-cases each word. So "mypack:bosses/wither_storm" reads as "Bosses - Wither Storm".
    public static String prettyName(ResourceLocation id) {
        String path = id.getPath().replace('_', ' ').replace("/", " - ");
        StringBuilder pretty = new StringBuilder(path.length());
        boolean wordStart = true;
        for (int i = 0; i < path.length(); i++) {
            char letter = path.charAt(i);
            if (letter == ' ') {
                pretty.append(letter);
                wordStart = true;
            } else if (wordStart) {
                pretty.append(Character.toUpperCase(letter));
                wordStart = false;
            } else {
                pretty.append(Character.toLowerCase(letter));
            }
        }
        return pretty.toString();
    }

    // The requirement_text field wins; without it the trigger gets turned into a single-line description.
    public static String requirementLine(UnlockDefinition unlock) {
        String text = unlock.getRequirementText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return describeInline(unlock.getTrigger());
    }

    public static String describeInline(UnlockTrigger trigger) {
        if (trigger instanceof AdvancementTrigger advancement) {
            return "Earn the " + advancement.advancementId() + " advancement";
        }
        if (trigger instanceof EntityKillTrigger entityKill) {
            return "Kill any " + prettyName(entityKill.entityTypeId());
        }
        if (trigger instanceof EntityKillCountTrigger killCount) {
            return "Kill " + killCount.requiredCount() + " " + prettyName(killCount.entityTypeId());
        }
        if (trigger instanceof AllOfTrigger allOf) {
            return "All of: " + joinChildren(allOf.children());
        }
        if (trigger instanceof AnyOfTrigger anyOf) {
            return "Any of: " + joinChildren(anyOf.children());
        }
        return "Unknown requirement";
    }

    private static String joinChildren(List<UnlockTrigger> children) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                joined.append(", ");
            }
            joined.append(describeInline(children.get(i)));
        }
        return joined.toString();
    }

    // Multi-line description for the detail view. A composite becomes a header line plus its children indented further.
    public static void appendDescription(UnlockTrigger trigger, String indent, List<String> out) {
        if (trigger instanceof AllOfTrigger allOf) {
            out.add(indent + "All of:");
            for (UnlockTrigger child : allOf.children()) {
                appendDescription(child, indent + "  ", out);
            }
        } else if (trigger instanceof AnyOfTrigger anyOf) {
            out.add(indent + "Any of:");
            for (UnlockTrigger child : anyOf.children()) {
                appendDescription(child, indent + "  ", out);
            }
        } else {
            out.add(indent + describeInline(trigger));
        }
    }

    public static void appendChildrenProgress(UnlockTrigger trigger, String indent, ServerPlayer player, PlayerProgress progress, List<String> out) {
        for (UnlockTrigger child : children(trigger)) {
            appendProgress(child, indent, player, progress, out);
        }
    }

    private static void appendProgress(UnlockTrigger trigger, String indent, ServerPlayer player, PlayerProgress progress, List<String> out) {
        if (trigger instanceof AllOfTrigger allOf) {
            out.add(indent + "All of:");
            for (UnlockTrigger child : allOf.children()) {
                appendProgress(child, indent + "  ", player, progress, out);
            }
        } else if (trigger instanceof AnyOfTrigger anyOf) {
            out.add(indent + "Any of:");
            for (UnlockTrigger child : anyOf.children()) {
                appendProgress(child, indent + "  ", player, progress, out);
            }
        } else {
            int[] counts = leafProgress(trigger, player, progress);
            out.add(indent + leafLabel(trigger) + ": " + counts[0] + "/" + counts[1]);
        }
    }

    // Trailing "(3/8)" for the list view. Composites roll their children up into one ratio.
    public static String inlineProgress(UnlockTrigger trigger, ServerPlayer player, PlayerProgress progress) {
        int[] counts = aggregate(trigger, player, progress);
        return " (" + counts[0] + "/" + counts[1] + ")";
    }

    // all_of sums current and required over every child; any_of reports the child nearest to satisfying. Recurses so nested composites fold into a single ratio.
    private static int[] aggregate(UnlockTrigger trigger, ServerPlayer player, PlayerProgress progress) {
        if (trigger instanceof AllOfTrigger allOf) {
            int current = 0;
            int required = 0;
            for (UnlockTrigger child : allOf.children()) {
                int[] childCounts = aggregate(child, player, progress);
                current += childCounts[0];
                required += childCounts[1];
            }
            return new int[]{current, required};
        }
        if (trigger instanceof AnyOfTrigger anyOf) {
            int[] best = null;
            for (UnlockTrigger child : anyOf.children()) {
                int[] childCounts = aggregate(child, player, progress);
                if (best == null || ratio(childCounts) > ratio(best)) {
                    best = childCounts;
                }
            }
            return best == null ? new int[]{0, 1} : best;
        }
        return leafProgress(trigger, player, progress);
    }

    private static double ratio(int[] counts) {
        if (counts[1] <= 0) {
            return 0.0;
        }
        return (double) counts[0] / counts[1];
    }

    public enum UnlockStatus { UNLOCKED, IN_PROGRESS, NOT_STARTED }

    // Shared by the list view tag and the in-progress/not-started filters so the two cannot disagree. Any headway counts: a leaf count above zero, or an earned advancement the trigger references.
    public static UnlockStatus statusOf(UnlockTrigger trigger, ServerPlayer player, PlayerProgress progress, boolean granted) {
        if (granted) {
            return UnlockStatus.UNLOCKED;
        }
        if (aggregate(trigger, player, progress)[0] > 0) {
            return UnlockStatus.IN_PROGRESS;
        }
        return UnlockStatus.NOT_STARTED;
    }

    // For the detail summary line: leaf counts for a leaf, satisfied-children count for a composite.
    public static int[] summary(UnlockTrigger trigger, ServerPlayer player, PlayerProgress progress) {
        if (isComposite(trigger)) {
            List<UnlockTrigger> kids = children(trigger);
            int met = 0;
            for (UnlockTrigger child : kids) {
                if (UnlockEvaluator.isSatisfied(child, player, progress)) {
                    met++;
                }
            }
            return new int[]{met, kids.size()};
        }
        return leafProgress(trigger, player, progress);
    }

    public static boolean isComposite(UnlockTrigger trigger) {
        return trigger instanceof AllOfTrigger || trigger instanceof AnyOfTrigger;
    }

    public static List<UnlockTrigger> children(UnlockTrigger trigger) {
        if (trigger instanceof AllOfTrigger allOf) {
            return allOf.children();
        }
        if (trigger instanceof AnyOfTrigger anyOf) {
            return anyOf.children();
        }
        return List.of();
    }

    private static int[] leafProgress(UnlockTrigger trigger, ServerPlayer player, PlayerProgress progress) {
        if (trigger instanceof AdvancementTrigger advancement) {
            return new int[]{UnlockEvaluator.hasAdvancement(player, advancement.advancementId()) ? 1 : 0, 1};
        }
        if (trigger instanceof EntityKillTrigger entityKill) {
            int count = progress.getKillCount(entityKill.entityTypeId());
            return new int[]{Math.min(count, 1), 1};
        }
        if (trigger instanceof EntityKillCountTrigger killCount) {
            int count = progress.getKillCount(killCount.entityTypeId());
            return new int[]{Math.min(count, killCount.requiredCount()), killCount.requiredCount()};
        }
        return new int[]{0, 1};
    }

    private static String leafLabel(UnlockTrigger trigger) {
        if (trigger instanceof AdvancementTrigger advancement) {
            return advancement.advancementId().toString();
        }
        if (trigger instanceof EntityKillTrigger entityKill) {
            return prettyName(entityKill.entityTypeId());
        }
        if (trigger instanceof EntityKillCountTrigger killCount) {
            return prettyName(killCount.entityTypeId());
        }
        return describeInline(trigger);
    }

    // Parenthetical summary for an auto-generated unlock message: one clause per non-empty grant, in the order shown here.
    public static String grantSummary(UnlockGrants grants) {
        List<String> parts = new ArrayList<>();
        if (grants.getMaxManaBonus() != 0) {
            parts.add(signed(grants.getMaxManaBonus()) + " max mana");
        }
        if (grants.getManaRegenBonus() != 0.0) {
            parts.add(signedDecimal(grants.getManaRegenBonus()) + " mana regen");
        }
        if (grants.getCooldownReductionBonus() != 0.0) {
            parts.add(signedPercent(grants.getCooldownReductionBonus()) + " cooldown reduction");
        }
        if (grants.getCastTimeReductionBonus() != 0.0) {
            parts.add(signedPercent(grants.getCastTimeReductionBonus()) + " cast time reduction");
        }
        if (grants.getRarityCap() != null) {
            parts.add("spell rarity cap raised to " + grants.getRarityCap().toLowerCase(Locale.ROOT));
        }
        Set<ResourceLocation> dimensions = grants.getDimensionsRemoved();
        if (!dimensions.isEmpty()) {
            parts.add(describeRemovedDimensions(dimensions));
        }
        Set<ResourceLocation> inscriptions = grants.getInscriptionsRemoved();
        if (!inscriptions.isEmpty()) {
            parts.add(describeRemovedInscriptions(inscriptions));
        }
        return String.join(", ", parts);
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String signedDecimal(double value) {
        String magnitude = String.format(Locale.ROOT, "%.1f", value);
        return value >= 0 ? "+" + magnitude : magnitude;
    }

    private static String signedPercent(double value) {
        long percent = Math.round(value * 100.0);
        return (percent >= 0 ? "+" + percent : Long.toString(percent)) + "%";
    }

    private static String describeRemovedDimensions(Set<ResourceLocation> dimensions) {
        if (dimensions.size() < 3) {
            List<String> names = dimensions.stream().map(ResourceLocation::toString).sorted().toList();
            return "can now cast in " + String.join(", ", names);
        }
        return "can now cast in " + dimensions.size() + " dimensions";
    }

    private static String describeRemovedInscriptions(Set<ResourceLocation> inscriptions) {
        if (inscriptions.size() < 3) {
            List<String> names = inscriptions.stream().map(UnlockDescriber::prettyName).sorted().toList();
            return "can now inscribe " + String.join(", ", names);
        }
        return "can now inscribe " + inscriptions.size() + " spells";
    }
}
