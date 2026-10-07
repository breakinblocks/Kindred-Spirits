# Changelog

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
