// One parsed unlock JSON: identity, trigger condition, what it grants, an optional flavor message, requirement text, and a display name.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class UnlockDefinition {

    private final ResourceLocation id;
    private final UnlockTrigger trigger;
    private final UnlockGrants grants;
    private final Optional<String> message;
    private final String requirementText;
    private final String displayName;

    public UnlockDefinition(ResourceLocation id, UnlockTrigger trigger, UnlockGrants grants, Optional<String> message, String requirementText, String displayName) {
        this.id = id;
        this.trigger = trigger;
        this.grants = grants;
        this.message = message;
        this.requirementText = requirementText;
        this.displayName = displayName;
    }

    public ResourceLocation getId() {
        return id;
    }

    public UnlockTrigger getTrigger() {
        return trigger;
    }

    public UnlockGrants getGrants() {
        return grants;
    }

    public Optional<String> getMessage() {
        return message;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return "UnlockDefinition{id=" + id + ", trigger=" + trigger.type() + "}";
    }
}
