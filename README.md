# Kindred Spirits

A pet companion mod for Minecraft 1.21.1 on NeoForge. Bond with a spirit
companion, keep it alive and fed, and it grows with you: gaining levels, raising
its bond, and unlocking abilities that change how it fights and helps you.

Seven companions are available: Nightfox, T-Rex, Baby Dragon, Mini Player, Gremlin,
Quokka, and Direwolf. The Quokka is a jungle support companion with Luck, animal care,
monster charm, and up to 27 storage slots.

Around a dozen distinct companions are planned, each with its own model,
animations, stat curve and ability set.

## Integrations

- **GeckoLib** (required) - companion models and animations
- **Jade** (optional) - your bonded companion's level, stars and bond level on its tooltip
- **JEI** (optional) - info pages for Kindred Spirits items

## Building

```bash
./gradlew build
```

Output jar: `build/libs/kindredspirits-1.21.1-<version>.jar`

Releases use a plain version, `mod_version=1.0.0` in `gradle.properties`, and
publish to the BreakInBlocks releases Maven repository with `./gradlew publishMaven`,
using `MAVEN_USER` and `MAVEN_TOKEN` from the environment. Development builds between
releases use a numbered snapshot of the next version, for example
`mod_version=1.0.1-SNAPSHOT.<n>`, which publishes to the snapshots repository instead;
increment the final snapshot number for each development build. The Maven version
includes the Minecraft prefix, for example `1.21.1-1.0.0`; the in-game mod version is
`1.0.0`.

Run the dev client with `./gradlew runClient`.

## Verification and item art

Run the dedicated integration suite with `./gradlew runGameTestServer`. It creates an isolated world under `run-gametest/` and exits nonzero if a required test fails. The GameTests cover companion storage/revival, ownership, cross-dimension recall, combat, progression, archaeology, egg hatching, persistence, and light cleanup. Test code and fixtures are excluded from the release jar.

`./gradlew build` also checks that every item texture is a native 16x16 PNG with hard alpha and a transparent silhouette.

## License

All Rights Reserved. See [LICENSE.md](LICENSE.md). This mod may not be
redistributed, modified, or included in a modpack without written permission.

## Authors

Saereth, AlfredGG

## Credits

The Nightfox model, texture and animations are by **samus2002**, from the
RPG Pet Pack Vol.2 (Oriental) asset pack, used under its licence. The pack's terms
permit editing and use in content with credit to the creator, and prohibit resale.
