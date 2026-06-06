# Changelog

Newest first. Versions match the Forge and NeoForge builds.

## v1.5.0
- moved the Forge 1.20.1 config from per-world serverconfig to the global config folder, matching the NeoForge build. a world with customized settings needs the existing file copied into the global config folder before launching. a world left without the copied file reverts to defaults.
- added a per-world override system. drop a copy of the global config into a world's serverconfig folder to override values for that world. present keys win, absent keys fall back to global, an unparseable file falls back to global, and values resolve once at world load.
- added spellPowerMultiplier in the new [spells] config section. default 1.0, range 0.0 to 10.0. scales every player's SPELL_POWER attribute.
- added buffDurationMultiplier in the [spells] config section. default 1.0, range 0.0 to 10.0. scales the duration of buff and debuff effects from Iron's Spellbooks spells. does not affect vanilla potions, food effects, beacon effects, or effects from other mods.
- added buffDurationNamespaces in the [spells] config section. defaults to Iron's Spellbooks and known addons (cataclysm_spellbooks, dacxirons, gametechbcs_spellbooks, gtbcs_geomancy_plus, hazennstuff, ias_spellbooks, and traveloptics). controls which mod effect namespaces buffDurationMultiplier scales, and logs once per session when a player receives an effect from a namespace not in the list. vanilla minecraft is not supported.
- added the /isstweaks copyconfig command. copies the global config into the current world's serverconfig folder so per-world overrides can be set up in-game. requires permission level 2 and does not overwrite an existing per-world file.

## v1.4.1
- fixed active black hole references not getting cleared on server stop, so a closed world's black holes no longer linger into the next world loaded in the same session.

## v1.4.0
- added configurable HP and damage multipliers for summoned mobs, covering vexes, raise dead skeletons and zombies, polar bears, horses, and summon swords.

## v1.3.1
- fixed mana modifiers disappearing after death.
- fixed the mana cache growing for the lifetime of the server.
- fixed a malformed unlock file breaking datapack loading.
- fixed config reload race conditions.
- fixed players losing progression after returning from the End.

## v1.3.0
- added max_mana_bonus and mana_regen_bonus unlock grants.

## v1.2.1
- removed the broken requirementsCommandEnabled config option.

## v1.2.0
- added the /isstweaks requirements subcommand for looking up spell and rarity unlock requirements.

## v1.1.1
- fixed a crash when Iron's Spellbooks was not installed.
- renamed the /isstweaks unlock subcommand to grant and added multi-target support.

## v1.1.0
- added a datapack-driven per-player progression unlock system with advancement and entity_kill triggers, a retroactive scan on login, and the /isstweaks admin command.
- added black hole pull resistance configurable per entity type.
- replaced the spell level cap with a spell rarity gate.
- fixed black hole resistance not applying to fully immune mobs.

## v1.0.0
- released the first version with config overrides for mana regen, starting max mana, cooldown reduction, and cast time reduction.
- added an option to fully disable passive mana regen, implemented as a per-tick drainback.
- added a spell casting dimension block, a spell level cap, and an inscription blacklist.
