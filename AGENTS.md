# Spatial Drum Kit project guidance

## What this project does

This is the native PICO Spatial SDK version of the adjacent legacy web drum simulator. It renders an eight-piece drum kit in a Mixed Stage, reads both hands through Tracking Pack, recognizes a grip/pinch as an invisible strike extension, and triggers a drum when that swept point crosses a drum surface with sufficient velocity. No drum-stick geometry is rendered.

The original web project at `../架子鼓模拟器/` is reference-only and must never be overwritten by changes in this project.

## Why this structure

- The app uses the generated PICO `stage` template because hand tracking is a Full Space capability.
- `domain/model/DrumKit.kt` defines the shared physical layout and appearance.
- `interaction/DrumHitDetector.kt` is a pure, unit-tested swept-surface detector.
- `tracking/HandTrackingController.kt` is the only class that reads Tracking Pack callbacks. It copies data off the provider thread and never mutates UI or ECS state there.
- `content/DrumScene.kt` owns ECS entities and resources.
- `CalibrationProfile.kitOffset` moves the production model and every hit surface together; the SpatialUI placement page edits it live and persists it with the rest of the device calibration.
- `calibration/CalibrationProfile.kt` and `CalibrationStore.kt` own persistent device-specific thresholds and per-drum offsets.
- `audio/DrumAudioEngine.kt` loads eight local CC0 WAV samples only after validating the license manifest and SHA-256 hashes. Its deterministic synthesized sounds are fallback-only.
- `content/HomeStage.kt` coordinates lifecycle, UI state, hit feedback, and main-thread ECS updates.

## Spatial SDK capabilities in use

- `DefaultStage` / Full Space
- Spatial ECS `ModelEntity`, `TransformComponent`, `CollisionComponent`
- Tracking Pack `HandTrackingProvider` with 26-joint hand poses
- SpatialUI `PicoTheme`, `Text`, `AttachmentPanel`, and material background
- SpatialUI `Button` and `Slider` controls for in-headset calibration
- A measured GLB production model loaded asynchronously through `Entity.loadSuspend`

## Mandatory UI rule

All 2D UI must use `com.pico.spatial.ui.*`, be wrapped by `PicoTheme`, and prefer `com.pico.spatial.ui.design.*` built-ins. Do not add Material or Material3. Do not hardcode Compose colors or typography. Custom hover must use `Modifier.spatialHoverEffect`.

## Natural next steps

1. Connect a real PICO headset and complete `CALIBRATION.md`; emulator values are not accepted as device calibration.
2. Measure acoustic end-to-end latency on the target headset/output path. The in-app metric stops at successful `SoundPool` submission.
3. Consider PICO object audio after validating that its real-device latency is not worse than the current preloaded `SoundPool` path.
4. Add SpatialUI recording, playback, and metronome controls.

## Production assets

- Model: `app/src/main/assets/models/drum_kit_refined.glb`, a PBR material refinement of **Drum Kit** by smoj, CC BY 3.0. The untouched source remains at `drum_kit.glb`; the measured bounds and runtime transform are in `.spatialsdk/scene_transforms.json`.
- Audio: eight WAV files from **Drum Kit Samples** by CM Music / CarbonMonoxideMusic, CC0 1.0. Exact filenames, sources, licenses, and hashes are in `app/src/main/assets/drums/LICENSES.json`.
- Do not replace either asset without updating its license manifest and hashes. Re-run the scene-builder bounding-box tool for any model replacement.

## Build, install, and run

```bash
./gradlew testDebugUnitTest assembleDebug
./gradlew installDebug
adb shell am start -n com.example.spatialdemo/.platform.LaunchActivity
```

`app/src/debug/AndroidManifest.xml` contains an emulator-only compatibility override for PICO Emulator 0.13.0. The production manifest remains `STAGE_MIXED` with standards-compliant zero immersion values; the debug override avoids the emulator incorrectly throttling the stage to 24 FPS.
