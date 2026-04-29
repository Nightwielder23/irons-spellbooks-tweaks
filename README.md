# Iron's Spellbooks Tweaks

Config tweaks for [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) aimed at modpack makers. Adds TOML knobs that Iron's hasn't shipped yet.

Soft dependency, no mixins, no access transformers. The mod loads cleanly without Iron's Spellbooks present and does nothing in that case.

## What it does

Iron's Spellbooks ships with `MANA_REGEN_MULTIPLIER`, `MANA_SPAWN_PERCENT`, and a few sword-related knobs in its serverconfig. There are several open issues asking for more direct control over mana regen, starting mana, and cooldowns ([#161](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/161), [#162](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/162), [#391](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/391)) that haven't been addressed.

This mod fills those gaps without touching Iron's serverconfig (which has known multiplayer sync bugs per [#1033](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/1033)). Settings live in their own TOML at `config/irons_spellbooks_tweaks-server.toml` and apply via attribute modifications and runtime hooks.

## Config

All settings are server-side. Use `-1` (or `false` for booleans) to disable any individual override.

### `[mana]`

**`baseManaRegenPercent`** (default `-1.0`, range `-1.0` to `100.0`)
Sets the base `MANA_REGEN` attribute on the player entity type. Iron's calculates regen as a percent of max mana, so this is the percent value. Their vanilla default is around `1.0`.

**`startingMaxMana`** (default `-1`, range `-1` to `100000`)
Sets the base `MAX_MANA` attribute on the player entity type. Applies to existing and new players on next login. Iron's vanilla default is `100`.

**`disableManaRegenEntirely`** (default `false`)
Fully disables passive mana regen. Implemented as a per-tick drainback because Iron's regen path doesn't fire a cancellable event. Spell casting still works normally, only passive regeneration is blocked.

### `[cooldown]`

**`cooldownReductionBonus`** (default `0.0`, range `-10.0` to `10.0`)
Additive bonus applied to the `COOLDOWN_REDUCTION` attribute for every player. Around `0.5` cuts cooldowns roughly in half. Negative values lengthen them. Stacks with gear and effects.

## Compatibility

- Minecraft 1.20.1 Forge only (1.20.1 is the supported branch of Iron's Spellbooks at time of writing)
- Iron's Spells 'n Spellbooks 3.0.0 or later
- Forge 47.2.0 or later
- No conflicts expected with other Iron's addons. The mod hooks `EntityAttributeModificationEvent` for three of its features and `PlayerTickEvent` for the regen disable, neither of which other addons compete for in destructive ways.

## For modpack makers

Drop the jar in your pack's `mods` folder, edit `config/irons_spellbooks_tweaks-server.toml`, ship the config alongside the pack. All knobs are server-side so clients don't need matching configs.

## License

MIT. See [LICENSE](LICENSE).

## Issues

Bug reports and feature requests welcome at [GitHub issues](https://github.com/nightwielder23/irons-spellbooks-tweaks/issues).