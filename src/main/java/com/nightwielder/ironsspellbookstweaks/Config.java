package com.nightwielder.ironsspellbookstweaks;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.DoubleValue BASE_MANA_REGEN_PERCENT;
    public static final ForgeConfigSpec.IntValue STARTING_MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_REDUCTION_BONUS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MANA_REGEN_ENTIRELY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("mana");
        BASE_MANA_REGEN_PERCENT = builder
                .comment("Base MANA_REGEN attribute value as a percent of max mana per regen tick. Set to -1 to leave Iron's default alone. Iron's vanilla default is around 1.0. Iron's regen ticks every 10 game ticks.")
                .defineInRange("baseManaRegenPercent", -1.0, -1.0, 100.0);
        STARTING_MAX_MANA = builder
                .comment("Base MAX_MANA attribute value for the player entity type. Applies to existing and new players on next login. Set to -1 to leave Iron's default alone. Iron's vanilla default is 100.")
                .defineInRange("startingMaxMana", -1, -1, 100000);
        DISABLE_MANA_REGEN_ENTIRELY = builder
                .comment("Fully disable passive mana regen. Implemented as a tick drainback because Iron's regen path does not fire a cancellable event.")
                .define("disableManaRegenEntirely", false);
        builder.pop();
        builder.push("cooldown");
        COOLDOWN_REDUCTION_BONUS = builder
                .comment("Additive bonus applied to the COOLDOWN_REDUCTION attribute for every player. Around 0.5 cuts cooldowns roughly in half. Negative values lengthen them. Stacks with gear and effects.")
                .defineInRange("cooldownReductionBonus", 0.0, -10.0, 10.0);
        builder.pop();
        SERVER_SPEC = builder.build();
    }
}
