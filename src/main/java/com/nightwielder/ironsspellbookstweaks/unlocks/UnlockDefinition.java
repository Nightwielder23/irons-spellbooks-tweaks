// One parsed unlock JSON: identity, trigger condition, what it grants, and an optional flavor message.
package com.nightwielder.ironsspellbookstweaks.unlocks;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class UnlockDefinition {

    private final ResourceLocation id;
    private final UnlockTrigger trigger;
    private final UnlockGrants grants;
    private final Optional<String> message;
    private final String requirementText;

    public UnlockDefinition(ResourceLocation id, UnlockTrigger trigger, UnlockGrants grants, Optional<String> message, String requirementText) {
        this.id = id;
        this.trigger = trigger;
        this.grants = grants;
        this.message = message;
        this.requirementText = requirementText;
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

    @Override
    public String toString() {
        return "UnlockDefinition{id=" + id + ", trigger=" + trigger.type() + "}";
    }
}
