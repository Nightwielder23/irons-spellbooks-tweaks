package com.nightwielder.ironsspellbookstweaks;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.DoubleValue BASE_MANA_REGEN_FLAT;
    public static final ForgeConfigSpec.DoubleValue BASE_MANA_REGEN_PERCENT;
    public static final ForgeConfigSpec.IntValue STARTING_MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_REDUCTION_BONUS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_MANA_REGEN_ENTIRELY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("mana");
        BASE_MANA_REGEN_FLAT = builder
                .comment("Flat mana per regen tick. Set to -1 to leave Iron's default alone. Takes priority over baseManaRegenPercent if both are set above -1. Iron's regen tick interval is 10 game ticks.")
                .defineInRange("baseManaRegenFlat", -1.0, -1.0, 1000.0);
        BASE_MANA_REGEN_PERCENT = builder
                .comment("Base MANA_REGEN attribute value as a percent of max mana per regen tick. Set to -1 to leave alone. Iron's vanilla default is around 1.0.")
                .defineInRange("baseManaRegenPercent", -1.0, -1.0, 100.0);
        STARTING_MAX_MANA = builder
                .comment("Max mana value for new players on first join. Set to -1 to leave alone. Iron's vanilla default is 100.")
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
