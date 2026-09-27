# Changelog

## 1.0.0 (unreleased)

### Companions

- Nightfox, T-Rex, Baby Dragon, Mini Player, Gremlin, Quokka and Direwolf.
- Baby Dragon is immune to fire and lava. Nightfox is immune to wither. Gremlin is immune
  to poison.
- T-Rex knockback resistance raised from 0.6 to 0.9.
- Use any dye on your companion to recolour its fur, hide or scales; eyes, teeth, claws,
  markings and gear keep their own colours, and dark companions take bright colours. A water
  bucket washes it off. The Mini Player keeps its player skin instead.
- Spirit Bandage: two paper and a string make two, and each heals a hurt companion 3 hearts.

### Obtaining

- Wild companions are tamed only with their species' taming item: sculk, steak, dragon's
  breath, golden carrot (`#kindredspirits:taming/<species>` tags, 33% per item).
- T-Rex Egg turns up in desert archaeology and hatches after 5 minutes, faster next to a
  torch or campfire.
- Lava flowing against a placed dragon egg hatches a Baby Dragon.
- Wandering traders always sell a Mini Player egg.
- Sculk used on a fox turns it into a Nightfox.
- A rabbit fed a carrot, then cooked chicken by the same player between 18000 and 23000,
  turns into a Gremlin.
- Quokkas spawn wild in jungles and are tamed with shrubs.
- A Golden Bone (a bone in eight gold nuggets) used on a wolf turns it into a Direwolf. Your
  own tamed wolf comes out tamed, keeps its name and its wolf armour; a wild wolf comes out
  wild and is tamed with more Golden Bones.

### Bond

- Bond is a level from 0 to 30, earned at 1 point a second while the companion is out
  with you and you are not AFK.
- Feeding the taming item adds 5% of current bond on a 10 minute cooldown. Ordinary
  companion food adds 1 bond point.
- Dying above bond 10 costs a bond level.
- Bond scales revive time, storage slots and experience rate. Bond 5 unlocks the species
  special, bond 15 and 30 speed up the base buff, bond 30 gives the owner +2 armour.

### Leveling

- Companions gain half of the experience their owner picks up, with a saturation limit
  that resets when you move 3 chunks, and rested experience after 5 idle minutes.
- Experience from Bottles o' Enchanting is not shared with companions.
- Rested experience also builds while the companion rests in the charm.
- Armour and speed now grow with level alongside health and attack.
- Prestige at level 30 for a star (up to 5). Stars raise growth and add attributes.

### Abilities

- Three tiers per species on top of its attack: a base buff at level 1, a bond 5 special
  and a level 30 ultimate.
- Nightfox: Wisplight, Night Light, One With The Night.
- T-Rex: Alpha, Alpha Boost, X-Ray Stomp (shakes your screen).
- T-Rex Crushing Might now crushes ore: every 5 seconds, raw ore and ore items lying within 5
  blocks of it become 3 of the matching dust each (`c:raw_materials/<x>` or `c:ores/<x>` to
  `c:dusts/<x>`, for any ore a mod makes a dust for). It never breaks blocks. The dust count is
  `crushing_might_dust` in the common config, and the `#kindredspirits:crushing_blacklist` item
  tag keeps chosen ores safe.
- Baby Dragon: Forge Draft (now level 1), Dragonfire, Kiln Breath (`/kindredspirits smelt`).
- Mini Player: Are You Gonna Eat That?, Helping Hand, Friendly Face.
- Gremlin: Tinker, Snack Thief, Energized Chaos.
- Quokka: Smile, Brighter Side, Always Happy.
- Direwolf: Not Another Hole (digs up loot on soft ground, archaeology tables on sand and
  gravel), Best Friend (+50% experience from your kills), Leader of the Pack (Resistance II
  and hostiles glowing for you alone every 90 seconds).
- Abilities can be switched off individually from the charm screen.

### Equipment

- One equipment slot per species: Boxing Gloves (T-Rex), Dragon Tablet (Baby Dragon),
  Running Shoes (Nightfox), Battery (Gremlin), Quokka Snack (Quokka), any chestplate
  (Mini Player), any wolf armour (Direwolf). Right-click to equip.
- The T-Rex wears its Boxing Gloves: a gloved model, texture and animation set replace the
  base ones while they are equipped, and its attack becomes a punch.
- The Baby Dragon holds its Dragon Tablet: a tablet model, texture and animation set replace
  the base ones while it is equipped, with the screen glowing in the dark.

### Charm screen

- Stats panel on the left showing current and base values.
- Abilities panel on the right with per-ability toggles.
- Equipment row with a remove button, Prestige button at level 30, active ability button.
- The portrait holds still instead of following the cursor. Drag it to turn the
  companion, double-click to reset.

### Commands

- `/kindredspirits bond <points>` and `/kindredspirits smelt`.

### Sounds

- The Direwolf's hurt, death and attack sounds are Minecraft's wolf hurt, death and growl.
- The Mini Player swings with the player sword sweep, fires with the bow sound, and its hurt
  and death sounds are a higher player hurt and a higher villager death.
- The Quokka's death sound is the villager death.
- The Gremlin's Snack Thief uses the eating sound.
- New sounds for every companion's voice and abilities, the meteor impact, and the charm's
  summon, revive and prestige.
- A companion levelling up now finishes with the experience orb chime.

### Textures and models

- New spawn eggs for the T-Rex, Baby Dragon, Mini Player, Gremlin, Nightfox and Direwolf,
  each showing the companion's face in its own colours.
- New Golden Bone, Spirit Bandage and T-Rex Egg icons.
- The T-Rex Egg block is now a speckled tan egg, with cracks that spread as it gets closer to
  hatching.
- Darker, coloured outlines on the Battery and Dragon Tablet.
- Meteor Call drops a tumbling, burning meteor instead of a ghast fireball. Players can no
  longer punch it back.
- Wisplight's wisps are visible now: a small glowing orb that pulses gently.
- The T-Rex can sit (it lies down on its belly), and the T-Rex, Baby Dragon and Gremlin now
  react when hurt and when fed or petted.
- The Mini Player has a new animation set: idle, walk, run, sword swing, bow shot, Mirror
  Strike whirl, sitting, waving, flinching, popping in and falling over.
- The Mini Player shoots glowing spirit arrows that trail sparks, cannot be picked up and fade
  after landing.

### Fixed

- Gremlin and Quokka animations failed to load, which crashed the client whenever one
  was rendered, including the spawn eggs in any item list.
- No companion ever played its attack, leap, dig, shoot, hurt, spawn or petting animation;
  each one was cut off on its first frame. They now play in full.
- The Oracle Index guide showed `oracle_index.title.kindredspirits` instead of the mod name.
- Ability descriptions in the charm's Abilities popup ran off the screen as one long line;
  they now wrap.
- The Cycle Companion Orders key and `/kindredspirits info`, `xp` and `bond` acted on the
  nearest companion you own rather than your bonded one.
- Breaking a Wisplight light showed missing-texture particles.

### Other

- In-game guide through Oracle Index.
- Jade shows your bonded companion's bond level under its level.
- New charm, spawn egg, egg and equipment textures.
