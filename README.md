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
Multiplier applied to every player's `SPELL_POWER` attribute. `1.0` leaves spell power unchanged. Values above `1.0` strengthen every spell, and values below `1.0` weaken them. Stacks multiplicatively with gear and other modifiers. Damage values shown in spell tooltips reflect this multiplier automatically, since Iron's reads the SPELL_POWER attribute live.

**`buffDurationMultiplier`** (default `1.0`, range `0.0` to `10.0`)
Multiplier applied to the duration of buff and debuff effects from Iron's Spellbooks spells. `1.0` leaves durations unchanged. Vanilla potions, food effects, and beacon effects are never affected. Which mod effects are scaled is controlled by `buffDurationNamespaces`. Composes multiplicatively with `spellPowerMultiplier` for Iron's spells whose duration scales with spell power. Spell tooltips show the unmodified base duration. The effect applied in-game reflects the multiplier, and the active effect countdown in your inventory shows the actual time remaining.

**`buffDurationNamespaces`** (default includes Iron's Spellbooks and known addons)
Namespaces of mod effects that `buffDurationMultiplier` scales. The default covers Iron's Spellbooks plus the known addons that add their own effects (Cataclysm Spellbooks, dacxirons, GameTechBC's Spellbooks, GTBC's Geomancy Plus, Hazen 'n Stuff, Illage and Spell-age, and Traveloptics). Add more addon namespaces to scale their effects too, or remove entries to stop scaling them:
```toml
buffDurationNamespaces = ["irons_spellbooks", "cataclysm_spellbooks", "dacxirons", "gametechbcs_spellbooks", "gtbcs_geomancy_plus", "hazennstuff", "ias_spellbooks", "traveloptics"]
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

Scales the HP of Iron's summoned mobs and the damage dealt by Summon Swords. A multiplier of `1.0` means no scaling. HP scaling uses an `ADD_MULTIPLIED_BASE` attribute modifier so it composes with any other mod that already scales mob HP (Apotheosis, ScalingHealth, difficulty mods, etc): a multiplier of `3.0` here always means "3x the base HP Iron's intended for this summon", on top of whatever the pack already does.

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

### `[per_spell]`

Scales damage, cooldown, or mana cost for individual spells. Each setting is a list of `"spell_id:multiplier"` strings, where the multiplier is a number from `0.0` to `10.0`. A multiplier of `1.0` leaves a spell unchanged. A malformed entry is skipped and written to the log as a warning, so a bad string does not crash the mod and does not disappear without notice. The parser warns separately when an entry has no colon before the multiplier, when the spell ID does not parse, and when the multiplier is not a number, and each warning names the list the entry came from. A spell ID that no installed mod registers never matches anything. The colon between the spell ID and the multiplier is read from the end of the string, so a spell ID that already contains a colon still parses correctly.

**`damageMultipliers`** (default empty list)
Multiplier applied to the damage each listed spell deals. This stacks on top of `spellPowerMultiplier`, so a spell set to `2.0` here with `spellPowerMultiplier = 2.0` deals about four times its unscaled damage:
```toml
damageMultipliers = ["irons_spellbooks:fireball:1.5", "irons_spellbooks:magic_missile:0.5"]
```

Most spells deal their damage directly, and this multiplier reaches that damage through Iron's `SpellDamageEvent`. Some spells work another way: they apply a buff that raises the caster's own melee hits, and that damage never passes through `SpellDamageEvent`. A separate hook reads the same list and covers those spells. It handles Spider Aspect (`irons_spellbooks:spider_aspect`), and when Cataclysm Spellbooks is installed it also handles Abyssal Predator (`cataclysm_spellbooks:abyssal_predator`), Forgone Rage (`cataclysm_spellbooks:forgone_rage`), and Pharaoh's Wrath (`cataclysm_spellbooks:pharaohs_wrath`). Echoing Strike is not one of them, because its echo hit still uses the direct path, so you scale it with the key `irons_spellbooks:echoing_strikes`.

**`cooldownMultipliers`** (default empty list)
Multiplier applied to each listed spell's cooldown. Below `1.0` shortens the cooldown and above `1.0` lengthens it. This is a separate factor from the player's `COOLDOWN_REDUCTION` attribute, so it multiplies the cooldown that already has that attribute applied:
```toml
cooldownMultipliers = ["irons_spellbooks:teleport:0.5"]
```

**`manaCostMultipliers`** (default empty list)
Multiplier applied to each listed spell's mana cost. Below `1.0` makes a spell cheaper and above `1.0` makes it more expensive. This stacks on top of Iron's own per-spell `mana_cost_multiplier` config and multiplies the cost it produces. For a continuous spell the multiplier applies to each tick of mana the spell charges:
```toml
manaCostMultipliers = ["irons_spellbooks:fireball:0.75"]
```

## Per-player progression unlocks

A datapack-driven unlock system gates Iron's features behind advancements or boss kills. Unlock JSONs go in `data/<namespace>/isstweaks/unlocks/<id>.json`.

### Triggers

**`advancement`**: fires when a player earns a specific advancement. Retroactively scans on login so existing players get unlocks they qualify for.

**`entity_kill`**: fires when a player kills an entity matching the given type ID. Useful for mods that don't include boss kill advancements (Iron's Spellbooks itself, Ice and Fire, and similar). The player's kills of that entity type are counted and saved, so this trigger also resolves on login once the count is at least one. Only kills made after some unlock started referencing the entity type are counted.

**`entity_kill_count`**: fires once the player has killed the given entity type a set number of times. Takes an `entity` for the entity type and a `required_kills` value of at least one. The running total is saved with the player, so progress carries across logins and deaths.

**`all_of`**: a composite trigger that fires when every child trigger is satisfied. Takes a `children` array of one or more triggers. A child can itself be a composite, with up to ten levels of nesting.

**`any_of`**: a composite trigger that fires when any one of its child triggers is satisfied. Takes a `children` array of one or more triggers. A child can itself be a composite.

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

**`message`** (optional string)
The chat message the player sees when the unlock fires. If omitted the mod builds one from the display name and the listed grants, for example `Unlock earned: Zombie Slayer (+100 max mana, +10% cooldown reduction)`. Set the field to send your own message instead.

**`requirement_text`** (optional string)
Hint text shown to players when they run `/isstweaks requirements`. Should describe what the player needs to do to earn the unlock. If omitted the command tells the player no requirement text was provided.

**`display_name`** (optional string)
The name shown for the unlock in `/isstweaks unlocks` and the granted unlocks list in `/isstweaks status`. If omitted the command builds a name from the unlock id by dropping the namespace, swapping underscores for spaces and slashes for " - ", and capitalizing each word, so `mypack:bosses/dead_king` reads as "Bosses - Dead King".

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
  "requirement_text": "Defeat the Dead King to unlock",
  "display_name": "Fireball Mastery"
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

Require more than one condition with a nested composite. The player must defeat the Fire Boss and also either earn an advancement or kill ten blazes:

```json
{
  "trigger": {
    "type": "all_of",
    "children": [
      {
        "type": "entity_kill",
        "id": "irons_spellbooks:fire_boss"
      },
      {
        "type": "any_of",
        "children": [
          {
            "type": "advancement",
            "id": "minecraft:nether/all_effects"
          },
          {
            "type": "entity_kill_count",
            "entity": "minecraft:blaze",
            "required_kills": 10
          }
        ]
      }
    ]
  },
  "grants": {
    "rarity_cap": "epic"
  },
  "message": "Epic spells unlocked.",
  "requirement_text": "Defeat the Fire Boss, then either finish the Nether effects advancement or slay 10 blazes"
}
```

## Commands

OP-only:
- `/isstweaks grant <player> <unlock_id>`: grant an unlock manually
- `/isstweaks revoke <player> [<unlock_id>]`: take unlocks back and rebuild the player's bonuses so they are actually removed, not just dropped from the granted list. Name an unlock to revoke that one, or leave it off to revoke every unlock the player holds. Revoking one unlock also forgets the player's kills of any entity that no other loaded unlock still needs, and revoking every unlock clears all kill counts.
- `/isstweaks status <player>`: show another player's current progress
- `/isstweaks reset [<player>]`: wipe all progression data. Name a player to reset them, or leave the name off to reset your own progress. Run from the console with no name, it reports that the command must be run by a player. Both forms need permission level 2.
- `/isstweaks copyconfig`: copy the global config into the current world's serverconfig folder. Does not overwrite an existing per-world file.

Open to all players:
- `/isstweaks status`: show your own progress. Run from the console with no player named, it reports that the command must be run by a player.
- `/isstweaks unlocks`: list every loaded unlock. Each line shows a status tag, the unlock name, its requirement, your progress toward it, and the bonuses it grants. The tag reads `[Unlocked]` in green once you hold the unlock, `[In Progress]` in yellow while you have some progress toward it, or `[Not Started]` in red before you have started.
- `/isstweaks unlocks all`: list every loaded unlock, the same as the bare command with no filter word.
- `/isstweaks unlocks completed`: list only the unlocks you already hold.
- `/isstweaks unlocks incomplete`: list only the unlocks you have not earned.
- `/isstweaks unlocks in-progress`: list only the unlocks you have started but not finished, meaning the ones tagged `[In Progress]`.
- `/isstweaks unlocks not-started`: list only the unlocks you have no progress on at all, meaning the ones tagged `[Not Started]`.
- `/isstweaks unlocks <unlock_id>`: show the full requirement, the bonuses it grants, and the progress breakdown for one unlock.
- `/isstweaks requirements spell <spell_id>`: shows the unlock requirement for a specific spell. Output is prefixed with `[Unlocked]` if the calling player has already met the requirement.
- `/isstweaks requirements rarity <rarity>`: shows the unlock requirement for a rarity tier (`common`, `uncommon`, `rare`, `epic`, `legendary`). Same `[Unlocked]` indicator if the player's current rarity cap is at or above the queried tier.

## Compatibility

- Minecraft 1.20.1 Forge and 1.21.1 NeoForge (this branch is 1.21.1)
- Iron's Spells 'n Spellbooks 3.0.0 or later (1.20.1) or 1.21.1-3.15.0 or later (1.21.1)
- Forge 47.2.0 or later (1.20.1) or NeoForge 21.1.221 or later (1.21.1)
- No conflicts expected with other Iron's addons. The mod hooks `PlayerEvent.PlayerLoggedInEvent`, `SpellPreCastEvent`, `InscribeSpellEvent`, `PlayerTickEvent.Post`, `AdvancementEvent.AdvancementEarnEvent`, `LivingDeathEvent`, `LivingIncomingDamageEvent`, `EntityJoinLevelEvent`, `EntityLeaveLevelEvent`, `ServerTickEvent.Post`, `AddReloadListenerEvent`, `MobEffectEvent.Added`, `ServerAboutToStartEvent`, `ServerStoppedEvent`, and `RegisterCommandsEvent`. None of these are commonly competed for in destructive ways.

## For modpack makers

Drop the jar in your pack's `mods` folder, edit `config/irons_spellbooks_tweaks-server.toml`, and distribute the config alongside the pack. All settings are server-side so clients don't need matching configs.

Note: progression data is per-world and stored as a NeoForge data attachment on the player's NBT. It survives death and login/logout. Datapack unlock JSONs reload via `/reload`.

## Per-world overrides

The global `config/irons_spellbooks_tweaks-server.toml` is the source of truth. Every world uses its values unless a per-world override is present.

To override settings for one world, copy the global file into that world's `serverconfig` folder. In singleplayer the path is `saves/<world>/serverconfig/irons_spellbooks_tweaks-server.toml`. On a dedicated server it is `<server>/world/serverconfig/irons_spellbooks_tweaks-server.toml`.

Run `/isstweaks copyconfig` in-game (permission level 2) to copy the global file into the current world's `serverconfig` folder without opening a file explorer. The command will not overwrite an existing per-world file; delete or rename it first.

A per-world file overrides global per key. Keys present in the per-world file win, and keys absent from it fall back to the global value, so an override file copied from an older version still picks up new global settings. A per-world file that fails to parse logs a warning and falls back to global for that world.

Values resolve once at world load. Edits to the global file apply on the next world load. Delete the per-world file to return that world fully to global.

## License

MIT. See [LICENSE](LICENSE).

## Issues

Bug reports and feature requests welcome at [GitHub issues](https://github.com/nightwielder23/irons-spellbooks-tweaks/issues).