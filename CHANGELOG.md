# Changelog

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
