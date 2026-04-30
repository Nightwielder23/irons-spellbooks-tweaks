# Iron's Spellbooks Tweaks

Config tweaks for [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) aimed at modpack makers. Adds TOML knobs that Iron's hasn't shipped yet.

Soft dependency, no mixins, no access transformers. The mod loads cleanly without Iron's Spellbooks present and does nothing in that case.

## What it does

Iron's Spellbooks ships with `MANA_REGEN_MULTIPLIER`, `MANA_SPAWN_PERCENT`, and a few sword-related knobs in its serverconfig. There are several open issues asking for more direct control over mana regen, starting mana, and cooldowns ([#161](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/161), [#162](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/162), [#391](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/391)) that haven't been addressed.

This mod fills those gaps without touching Iron's serverconfig (which has known multiplayer sync bugs per [#1033](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/1033)). Settings live in their own TOML at `config/irons_spellbooks_tweaks-server.toml` and apply via attribute modifications and runtime hooks on Iron's public events.

## Config

All settings are server-side. Use `-1` (or `false` / empty list) to disable any individual override.

### `[mana]`

**`baseManaRegenPercent`** (default `-1.0`, range `-1.0` to `100.0`)
Bonus added to the player's `MANA_REGEN` attribute on every login. Stacks as a flat addition on top of Iron's vanilla default of around `1.0`. So setting this to `4.0` gives players an effective regen rate of about `5.0`. Set to `-1` to disable.

**`startingMaxMana`** (default `-1`, range `-1` to `100000`)
Bonus added to the player's `MAX_MANA` attribute on every login. Stacks as a flat addition on top of Iron's vanilla default of `100`. So setting this to `400` gives players `500` max mana total. Set to `-1` to disable.

**`disableManaRegen`** (default `false`)
Fully disables passive mana regen. Implemented as a per-tick drainback because Iron's regen path doesn't fire a cancellable event. Spell casting still works normally, only passive regeneration is blocked.

### `[cooldown]`

**`cooldownReductionBonus`** (default `0.0`, range `-10.0` to `10.0`)
Additive bonus applied to the `COOLDOWN_REDUCTION` attribute for every player. Around `0.5` cuts cooldowns roughly in half. Negative values lengthen them. Stacks with gear and effects.

**`castTimeReductionBonus`** (default `0.0`, range `-10.0` to `10.0`)
Additive bonus applied to the `CAST_TIME_REDUCTION` attribute for every player. Around `0.5` makes spells cast roughly twice as fast. Negative values lengthen cast times. Stacks with gear and effects.

### `[restrictions]`

**`spellCastingDisabledDimensions`** (default empty list)
Dimensions where players cannot cast spells. Use full namespaced dimension IDs as strings:
```toml
spellCastingDisabledDimensions = ["minecraft:nether", "twilightforest:twilight_forest"]
```
Mob casters are not affected. Iron's wizards and bosses still cast normally in blocked dimensions.

**`maxSpellLevelGlobal`** (default `-1`, range `-1` to `100`)
Hard cap on the level any player-cast spell can fire at. Higher levels still consume scroll/spellbook slots normally, they just cap at the configured ceiling when cast. Mob casters bypass this so Iron's wizards and bosses keep their full power.

**`inscriptionBlacklist`** (default empty list)
Spell IDs that cannot be inscribed at the inscription table. Use full namespaced spell IDs:
```toml
inscriptionBlacklist = ["irons_spellbooks:fireball", "irons_spellbooks:fire_breath"]
```
Players attempting to inscribe a blacklisted spell will see the action cancelled silently.

## Compatibility

- Minecraft 1.20.1 Forge only (1.20.1 is the supported branch of Iron's Spellbooks at time of writing)
- Iron's Spells 'n Spellbooks 3.0.0 or later
- Forge 47.2.0 or later
- No conflicts expected with other Iron's addons. The mod hooks `PlayerLoggedInEvent`, `SpellPreCastEvent`, `ModifySpellLevelEvent`, `InscribeSpellEvent`, and `PlayerTickEvent`. None of these are commonly competed for in destructive ways.

## For modpack makers

Drop the jar in your pack's `mods` folder, edit `config/irons_spellbooks_tweaks-server.toml`, ship the config alongside the pack. All knobs are server-side so clients don't need matching configs.

## License

MIT. See [LICENSE](LICENSE).

## Issues

Bug reports and feature requests welcome at [GitHub issues](https://github.com/nightwielder23/irons-spellbooks-tweaks/issues).