# Kindred Spirits

A pet companion mod for Minecraft 26.1.2 on NeoForge. Bond with a spirit
companion, keep it alive and fed, and it grows with you: gaining levels, raising
its bond, and unlocking abilities that change how it fights and helps you.

Around a dozen distinct companions are planned, each with its own model,
animations, stat curve and ability set.

## Integrations

- **GeckoLib** (required) - companion models and animations
- **Jade** (optional) - level, bond and ability readout on the companion tooltip
- **JEI** (optional) - info pages for Kindred Spirits items

## Building

```bash
./gradlew build
```

Output jar: `build/libs/kindredspirits-26.1.2-<version>.jar`

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
