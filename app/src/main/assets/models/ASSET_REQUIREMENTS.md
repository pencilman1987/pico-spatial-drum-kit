# Production drum kit model

The current production asset is `models/drum_kit_refined.glb`, a PBR material refinement of **Drum Kit** by smoj, distributed under CC BY 3.0. The untouched source remains at `models/drum_kit.glb`. Source, derivative relationship, license, hashes, and triangle counts are recorded in `LICENSES.json`. Do not rename the template `box.usdz` or use it as the kit.

The refined variant keeps the original geometry and attribution while replacing the toy-like palette with lacquered navy shells, bronze cymbals, chrome hardware, and tuned metallic/roughness values. Keep both files so the derivative is reproducible and the original is never overwritten.

The project scene-builder measurement and runtime transform are recorded in `.spatialsdk/scene_transforms.json`. Re-run the bounding-box tool whenever this asset changes. A replacement must use meters and keep draw calls and texture memory suitable for PICO.

Preferred surface node names for a future asset revision:

- `CRASH`
- `RIDE`
- `HI_HAT`
- `TOM_HIGH`
- `TOM_MID`
- `SNARE`
- `TOM_FLOOR`
- `KICK`

The model's source node names are preserved. Gameplay collision uses the calibrated `DrumSurface` definitions rather than depending on author-specific mesh names.
