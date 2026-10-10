# Changelog

## 1.0.7

- Releasing the bond with a companion that is resting in the Kindred Charm, or waiting to be revived, no longer deletes it. It now appears beside you, still tamed but unbonded, with its name, level and looks intact, and its gear goes to your inventory.
- Added `/kindredspirits skin <player>`, open to every player, which dresses your bonded Mini Player in that player's skin. Run it with no name to go back to your own skin. It follows the same `allow_skin_choice` setting as the skin box on the charm screen.
- Companions level about 50% faster. `base_experience` in the common config now defaults to 93 (was 140), so the climb to level 30 takes about 21,400 experience instead of 32,500. An existing config file keeps its old value until it is updated or deleted.

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
