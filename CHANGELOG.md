# Changelog

Newest first. Versions match the Forge and NeoForge builds.

## v1.6.0
- added a [per_spell] config section with damageMultipliers, cooldownMultipliers, and manaCostMultipliers. each is a list of "spell_id:multiplier" strings, clamped to the range 0.0 to 10.0, that scales one spell's damage, cooldown, or mana cost. per-spell damage stacks on top of spellPowerMultiplier.
- note on manaCostMultipliers: a value above 1.0 blocks a cast the player cannot afford at the higher cost. a value below 1.0 lowers the mana actually spent, but the player still needs the full base mana to begin the cast, because Iron's own affordability check runs before the mana cost is scaled.
- damageMultipliers now also covers spells that raise the caster's melee hits with a buff instead of dealing spell damage, which the SpellDamageEvent path never saw. Spider Aspect (irons_spellbooks:spider_aspect) is covered on both builds, and the NeoForge build also covers Cataclysm Spellbooks' abyssal_predator, forgone_rage, and pharaohs_wrath when that addon is present. Echoing Strike stays on the direct path under the key irons_spellbooks:echoing_strikes.
- added three unlock trigger types: entity_kill_count fires after a set number of kills of an entity type, all_of fires when every child trigger is met, and any_of fires when any child trigger is met. all_of and any_of nest, so triggers can combine up to ten levels deep.
- unlock kill counts are now saved per player and survive logins and deaths, and /isstweaks status lists them.
- added /isstweaks unlocks, which lists every loaded unlock with its requirement and the player's progress toward it. pass an unlock id for the full breakdown of one unlock. open to all players.
- reworked /isstweaks status into readable labelled lines instead of raw field names, and made it self-viewable without op: running it with no player named shows your own progress, while naming another player still needs permission level 2.
- polished the command display: unlock and entity names are now readable text built from the id instead of raw ids, unlock JSONs can set an optional display_name to override the shown name, and /isstweaks unlocks and status now color their output.
- /isstweaks unlocks now shows combined progress for all_of and any_of triggers in the list view. an all_of adds up progress across all its children, and an any_of shows the child closest to done.
- an unlock with no message field now sends a generated message when it fires. the message names the unlock and lists its grants, such as "Unlock earned: Zombie Slayer (+100 max mana, +10% cooldown reduction)". a message set in the unlock JSON still takes priority.
- /isstweaks reset now resets your own progress when you run it without naming a player, the same way /isstweaks status does. it still requires permission level 2 in both forms.
- a malformed entry in the per-spell damage, cooldown, or mana cost lists now logs a warning that names the list and the bad value instead of being skipped without notice. the parser flags an entry with no multiplier, an entry whose spell id does not parse, and an entry whose multiplier is not a number.
- fixed /isstweaks revoke and /isstweaks reset leaving unlock bonuses such as +100 max mana on the player until the next death. both commands now rebuild the player's attribute bonuses right away. /isstweaks revoke with no unlock id revokes every unlock the player holds, while naming one revokes only that unlock. reset still clears the player's kill counts along with everything else.
- /isstweaks unlocks now takes a filter word: all lists every unlock, completed lists the ones you already hold, and incomplete lists the rest. passing an unlock id still opens its detail view.
- /isstweaks unlocks takes two more filter words: in-progress lists the unlocks you have started but not finished, and not-started lists the ones you have no progress on at all. both read the same check that picks the [In Progress] and [Not Started] tags, so a filter and a tag can never disagree.
- /isstweaks unlocks with no filter word now lists every unlock, the same as the all filter, instead of only the incomplete ones. an in-progress filter that matches nothing shows "No unlocks in progress", and a not-started filter that matches nothing shows "All unlocks started".
- each unlock in the list now starts with a status tag, green for [Unlocked], yellow for [In Progress], and red for [Not Started], and ends with a short summary of the bonuses it grants. the detail view lists the grants too.
- /isstweaks status now shows a kill count only while an unearned unlock still needs that entity, and counts it against the highest remaining requirement, so an entity you no longer need to kill drops off the list.
- /isstweaks revoke of a single unlock now forgets the player's kills of any entity no other loaded unlock needs, while revoking every unlock clears all kill counts.
- rarity names in commands, configs, and unlock files now convert case with a fixed locale, so a server running under a locale such as Turkish no longer misreads a rarity like epic and disables the gate by mistake.
- the per-world config override and the global config now take their section and key names from one shared source, so renaming a setting can no longer leave an override pointing at the old name and quietly failing to apply.
- unlocks and bonuses now carry across a respawn or a return from the End through the same routine that saves them, so any progression value added in a later version comes along too instead of being dropped on death.
- unlock files that still use the retired spell_level_cap grant now log a warning on the NeoForge build as well, matching the Forge build and pointing pack makers to rarity_cap.
- migration note: an entity_kill unlock that a player has not yet earned starts counting from zero on upgrade, since kills made before v1.6 were never recorded. already-earned unlocks and all other progression carry over unchanged.

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
