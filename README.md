# Iron's Spellbooks Tweaks

Config tweaks for [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) aimed at modpack makers. Adds TOML config options Iron's doesn't expose.

Soft dependency, no mixins, no access transformers. The mod loads cleanly without Iron's Spellbooks present and does nothing in that case.

## What it does

Iron's Spellbooks exposes `MANA_REGEN_MULTIPLIER`, `MANA_SPAWN_PERCENT`, and a few sword-related options in its serverconfig. Open issues asking for more direct control over mana regen and starting mana ([#161](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/161), [#162](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/162), and [#391](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/391)) haven't been addressed.

The mod fills those gaps without touching Iron's serverconfig (which has known multiplayer sync bugs per [#1033](https://github.com/iron431/Irons-Spells-n-Spellbooks/issues/1033)). Settings are kept in a separate TOML at `config/irons_spellbooks_tweaks-server.toml` and apply via attribute modifications and runtime hooks on Iron's public events.

## Config

All settings are server-side. Use `-1` (or `false` / empty list) to disable any individual override.

### `[mana]`

**`baseManaRegenPercent`** (default `-1.0`, range `-1.0` to `100.0`)
Bonus added to the player's `MANA_REGEN` attribute on every login. Stacks as a flat addition on top of Iron's vanilla default of around `1.0`. So setting this to `4.0` gives players an effective regen rate of about `5.0`. Set to `-1` to disable.

**`startingMaxMana`** (default `-1`, range `-1` to `100000`)
Bonus added to the player's `MAX_MANA` attribute on every login. Stacks as a flat addition on top of Iron's vanilla default of `100`. So setting this to `400` gives players `500` max mana total. Set to `-1` to disable.

**`disableManaRegen`** (default `false`)
Fully disables passive mana regen. Implemented as a per-tick drainback because Iron's regen path doesn't fire a cancellable event. Spell casting still works normally; only passive regeneration is blocked.

### `[cooldown]`

**`cooldownReductionBonus`** (default `0.0`, range `-10.0` to `10.0`)
Additive bonus applied to the `COOLDOWN_REDUCTION` attribute for every player. Around `0.5` cuts cooldowns roughly in half. Negative values lengthen them. Stacks with gear and effects.

**`castTimeReductionBonus`** (default `0.0`, range `-10.0` to `10.0`)
Additive bonus applied to the `CAST_TIME_REDUCTION` attribute for every player. Around `0.5` makes spells cast roughly twice as fast. Negative values lengthen cast times. Stacks with gear and effects.

### `[spells]`

**`spellPowerMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to every player's `SPELL_POWER` attribute. `1.0` leaves spell power unchanged. Values above `1.0` strengthen every spell, and values below `1.0` weaken them. Stacks multiplicatively with gear and other modifiers.

**`buffDurationMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to the duration of buff and debuff effects from Iron's Spellbooks spells. `1.0` leaves durations unchanged. Vanilla potions, food effects, and beacon effects are never affected. Which mod effects are scaled is controlled by `buffDurationNamespaces`. Composes multiplicatively with `spellPowerMultiplier` for Iron's spells whose duration scales with spell power.

**`buffDurationNamespaces`** (default includes Iron's Spellbooks and known addons)
Namespaces of mod effects that `buffDurationMultiplier` scales. The default covers Iron's Spellbooks plus the known addons that add their own effects (Cataclysm Spellbooks, dacxirons, GTBC's Geomancy Plus, and Traveloptics). Add more addon namespaces to scale their effects too, or remove entries to stop scaling them:
```toml
buffDurationNamespaces = ["irons_spellbooks", "cataclysm_spellbooks", "dacxirons", "gtbcs_geomancy_plus", "traveloptics"]
```
Vanilla `minecraft` is intentionally not supported and is ignored if listed.

### `[restrictions]`

**`spellCastingDisabledDimensions`** (default empty list)
Dimensions where players cannot cast spells. Use full namespaced dimension IDs as strings:
```toml
spellCastingDisabledDimensions = ["minecraft:nether", "twilightforest:twilight_forest"]
```
Mob casters are not affected. Iron's wizards and bosses still cast normally in blocked dimensions.

**`maxSpellRarity`** (default empty)
Highest spell rarity players are allowed to cast. Spells with a higher minimum rarity are blocked. Valid values: `common`, `uncommon`, `rare`, `epic`, `legendary`, or empty to disable:
```toml
maxSpellRarity = "uncommon"
```
The example above allows common and uncommon spells but blocks rare, epic, and legendary. Setting it to `"legendary"` allows everything. Mob casters are not affected, so Iron's wizards and bosses cast at full strength regardless.

**`inscriptionBlacklist`** (default empty list)
Spell IDs that cannot be inscribed at the inscription table. Use full namespaced spell IDs:
```toml
inscriptionBlacklist = ["irons_spellbooks:fireball", "irons_spellbooks:fire_breath"]
```
Players attempting to inscribe a blacklisted spell will see the action cancelled silently.

### `[blackhole]`

**`blackholeImmunity`** (default empty list)
Per-entity-type resistance to black hole pull. Format `"entity_id:strength"` where strength is `0.0` (no effect) to `1.0` (fully immune):
```toml
blackholeImmunity = ["irons_spellbooks:dead_king:1.0", "minecraft:wither:0.8"]
```
Iron's hardcodes a 30% minimum pull regardless of `KNOCKBACK_RESISTANCE` attribute, so this exists to push past that floor for specific bosses. Affects black hole only, not other movement spells or vanilla knockback.

### `[summons]`

Scales the HP of Iron's summoned mobs and the damage dealt by Summon Swords. A multiplier of `1.0` means no scaling. HP scaling uses a `MULTIPLY_BASE` attribute modifier so it composes with any other mod that already scales mob HP (Apotheosis, ScalingHealth, difficulty mods, etc): a multiplier of `3.0` here always means "3x the base HP Iron's intended for this summon", on top of whatever the pack already does.

**`summonVexHpMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to a summoned vex's base HP.

**`summonVexDamageMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to a summoned vex's melee damage per hit. `0.0` reduces every hit to zero.

**`raiseDeadHpMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to the base HP of Raise Dead summons. Applies to both summoned skeletons and zombies.

**`raiseDeadDamageMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to a Raise Dead summon's melee damage per hit. Applies to both summoned skeletons and zombies. `0.0` reduces every hit to zero.

**`summonPolarBearHpMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to a summoned polar bear's base HP. Iron's already scales polar bear base HP with spell level and the caster's spell power before this multiplier is applied.

**`summonPolarBearDamageMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to a summoned polar bear's melee damage per hit. `0.0` reduces every hit to zero.

**`summonHorseHpMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to a summoned horse's base HP. Iron's already scales horse base HP with the caster's spell power before this multiplier is applied.

**`summonSwordsDamageMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to Summon Swords damage per hit. `0.0` reduces every hit to zero.

## Per-player progression unlocks

A datapack-driven unlock system gates Iron's features behind advancements or boss kills. Unlock JSONs live at `data/<namespace>/isstweaks/unlocks/<id>.json`.

### Triggers

**`advancement`**: fires when a player earns a specific advancement. Retroactively scans on login so existing players get unlocks they qualify for.

**`entity_kill`**: fires when a player kills an entity matching the given type ID. Useful for mods that don't include boss kill advancements (Iron's Spellbooks itself, Ice and Fire, etc). Doesn't replay retroactively since past kills aren't recorded.

### Grants

Each unlock can grant any combination of:
- `rarity_cap`: raise the player's allowed rarity ceiling. Value is one of `common`, `uncommon`, `rare`, `epic`, or `legendary`. The player can cast any spell with minimum rarity at or below this ceiling. Stacks with the `maxSpellRarity` config; the effective ceiling is whichever is looser. The cap only rises, so a later unlock can never tighten an earlier loosening.
- `cooldown_reduction_bonus`: add to the player's cooldown reduction attribute
- `cast_time_reduction_bonus`: add to the player's cast time reduction attribute
- `max_mana_bonus`: integer flat addition to the player's MAX_MANA attribute. Stacks across unlocks (cumulative). Negative values subtract.
- `mana_regen_bonus`: flat addition to the player's MANA_REGEN attribute. Stacks across unlocks (cumulative). Negative values subtract.
- `remove_dimensions`: exempt the player from the casting dimension blacklist for these dimensions
- `remove_inscriptions`: exempt the player from the inscription blacklist for these spells

### Optional fields

**`requirement_text`** (optional string)
Hint text shown to players when they run `/isstweaks requirements`. Should describe what the player needs to do to earn the unlock. If omitted the command tells the player no requirement text was provided.

### Examples

Lock fireball behind killing the Dead King. In your pack's datapack at `data/mypack/isstweaks/unlocks/fireball_unlock.json`:

```json
{
  "trigger": {
    "type": "entity_kill",
    "id": "irons_spellbooks:dead_king"
  },
  "grants": {
    "remove_inscriptions": ["irons_spellbooks:fireball"]
  },
  "message": "Fireball unlocked.",
  "requirement_text": "Defeat the Dead King to unlock"
}
```

Combined with `inscriptionBlacklist = ["irons_spellbooks:fireball"]` in the server config, fireball is un-inscribable until the player kills the Dead King.

Tier-up the player's spell ceiling on a boss kill. With `maxSpellRarity = "rare"` set globally, players start capped at rare. Killing the Fire Boss raises that player's personal ceiling to epic:

```json
{
  "trigger": {
    "type": "entity_kill",
    "id": "irons_spellbooks:fire_boss"
  },
  "grants": {
    "rarity_cap": "epic"
  },
  "message": "Epic spells unlocked.",
  "requirement_text": "Defeat the Fire Boss to unlock epic spells"
}
```

The grant is raise-only, so a later unlock that tries to set `rarity_cap` to `uncommon` would be ignored.

## Commands

OP-only:
- `/isstweaks grant <player> <unlock_id>`: grant an unlock manually
- `/isstweaks revoke <player> <unlock_id>`: remove from the granted set (cumulative bonuses stay applied; use reset for a clean slate)
- `/isstweaks status <player>`: show the player's current progress
- `/isstweaks reset <player>`: wipe all progression data for the player

Open to all players:
- `/isstweaks requirements spell <spell_id>`: shows the unlock requirement for a specific spell. Output is prefixed with `[Unlocked]` if the calling player has already met the requirement.
- `/isstweaks requirements rarity <rarity>`: shows the unlock requirement for a rarity tier (`common`, `uncommon`, `rare`, `epic`, `legendary`). Same `[Unlocked]` indicator if the player's current rarity cap is at or above the queried tier.

## Compatibility

- Minecraft 1.20.1 Forge and 1.21.1 NeoForge (this branch is 1.20.1)
- Iron's Spells 'n Spellbooks 3.0.0 or later (1.20.1) or 1.21.1-3.15.0 or later (1.21.1)
- Forge 47.2.0 or later (1.20.1) or NeoForge 21.1.221 or later (1.21.1)
- No conflicts expected with other Iron's addons. The mod hooks `PlayerLoggedInEvent`, `SpellPreCastEvent`, `InscribeSpellEvent`, `PlayerTickEvent`, `AdvancementEvent.AdvancementEarnEvent`, `LivingDeathEvent`, `LivingHurtEvent`, `EntityJoinLevelEvent`, `EntityLeaveLevelEvent`, `ServerTickEvent`, `AttachCapabilitiesEvent`, `PlayerEvent.Clone`, `AddReloadListenerEvent`, `MobEffectEvent.Added`, `ServerAboutToStartEvent`, `ServerStoppedEvent`, and `RegisterCommandsEvent`. None of these are commonly competed for in destructive ways.

## For modpack makers

Drop the jar in your pack's `mods` folder, edit `config/irons_spellbooks_tweaks-server.toml`, and distribute the config alongside the pack. All settings are server-side so clients don't need matching configs.

Migration note (v1.5): earlier Forge 1.20.1 builds stored this config per-world at `saves/<world>/serverconfig/irons_spellbooks_tweaks-server.toml`. v1.5 moves it to the global `config/irons_spellbooks_tweaks-server.toml`, matching the NeoForge build. The filename and contents stay the same, so copy an existing world's file into the global `config/` folder before launching. A world left without the copied file reverts to defaults.

Note: progression data is per-world and stored on the player's NBT capability. It survives death and login/logout. Datapack unlock JSONs reload via `/reload`.

## Per-world overrides

The global `config/irons_spellbooks_tweaks-server.toml` is the source of truth. Every world uses its values unless a per-world override is present.

To override settings for one world, copy the global file into that world's `serverconfig` folder. On Forge 1.20.1 singleplayer the path is `saves/<world>/serverconfig/irons_spellbooks_tweaks-server.toml`. On a dedicated server it is `<server>/world/serverconfig/irons_spellbooks_tweaks-server.toml`.

A per-world file overrides global per key. Keys present in the per-world file win, and keys absent from it fall back to the global value, so an override file copied from an older version still picks up new global settings. A per-world file that fails to parse logs a warning and falls back to global for that world.

Values resolve once at world load. Edits to the global file apply on the next world load. Delete the per-world file to return that world fully to global.

## License

MIT. See [LICENSE](LICENSE).

## Issues

Bug reports and feature requests welcome at [GitHub issues](https://github.com/nightwielder23/irons-spellbooks-tweaks/issues).
