# Changelog

## 1.0.9

- Fixed mod interaction edgecase possible dupes

## 1.0.8

- Added the Companion Bed. Hurt companions rest in a nearby bed and heal with Regeneration II. Dye the cushion any colour.
- Tamed companions on Stay now use their abilities without being bonded. Right-click one with an empty hand to toggle its abilities.
- A bonded T-Rex or Direwolf can be ridden: press Mount in its pack. Hold sprint to gallop.
- Night Ward now gives 30 seconds of Fire Resistance, and the Nightfox wards itself when it takes fire damage.
- Companion attack and armour grow faster with level, and each star adds 5% attack and 0.25 armour.
- Meteor Call and the Battery shock now scale with attack.
- Bond xp bonus raised to 2.5x at bond 30 (was 1.5x), and each star adds 20% xp.
- Fixed the bond xp bonus not applying to most xp orbs.
- Added `/kindredspirits minime [mc_name] ["custom name"]` to summon an unbonded Mini Player that wanders on its own. Operators only by default (`minime_permission_level`).
- New config options: `star_attack_bonus`, `star_armour_bonus`, `star_experience_bonus` and `bond_experience_bonus`. Existing configs keep their old `attack_per_level` and `armour_per_level` until updated or deleted.

## 1.0.7

- Companions level about 50% faster. Existing configs keep their old `base_experience` until updated or deleted.
- Releasing a resting or dead companion now brings it out beside you instead of deleting it.
- Added `/kindredspirits skin <player>` to dress your Mini Player in a player's skin.

## 1.0.6

- Raised every companion's level 1 health by 2.5 times: Baby Dragon and Gremlin 25, Nightfox 35, Mini Player, Quokka and Direwolf 45, T-Rex 75. These are the defaults in `kindredspirits-startup.toml`, so an existing config file keeps its old values until it is updated or deleted.

## 1.0.5

- Fixed the Kindred Charm losing track of a companion after its owner teleported away, which greyed out every button with no way to call, rest or release it. The charm now records where the companion is whenever it changes chunk or its chunk unloads.
- If the charm still cannot find a companion that is out, Call to me re-forms it beside you from what the charm remembers, and Release bond works too. The lost copy is removed if its chunk ever loads again, so its gear cannot be duplicated. Players already stuck this way can press Call to me after updating.
- The charm screen shows "Out of reach" instead of "Resting in the charm" for a companion it cannot find.

## 1.0.4

- Fixed companions levelling from xp returned by graves or xp storage blocks.
- Advancement and command xp no longer level companions.

## 1.0.3

- Fixed the Mini Player's Helping Hand duplicating items through reversible recipes, such as unpacking an iron block into ingots and packing them back (breakinblocks/Kindred-Spirits#10). Helping Hand now skips any craft whose result can be turned back into one of its ingredients within a few recipe steps, counting every recipe type, not only crafting.
- Added `helping_hand_recipe_blacklist` to the common config, a list of crafting recipe ids Helping Hand never copies. An entry ending in `*` matches by prefix, so `somemod:*` covers a whole mod.
- Added `helping_hand_reverse_depth` to the common config (default 4), how many recipe steps that check follows.

## 1.0.2

- Glowing parts of the Baby Dragon, Gremlin, Nightfox and T-Rex no longer flicker when using Iris with a shader pack.

## 1.0.1

Minor internal refactor

## 1.0.0

Initial Mod release
