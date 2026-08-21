# Production drum samples

The app bundles eight verified CC0 WAV samples from **Drum Kit Samples** by CM Music / CarbonMonoxideMusic. Their original names, source, license, asset paths, and SHA-256 hashes are recorded in `LICENSES.json`. The runtime validates every entry and hash before treating a sample as licensed; deterministic synthesized audio remains fallback-only.

To replace a production sample, add a local uncompressed `.wav` or `.ogg` below this directory and update the corresponding `LICENSES.json` entry.

Each manifest entry must contain:

- `drumId`: one of `CRASH`, `RIDE`, `HI_HAT`, `TOM_HIGH`, `TOM_MID`, `SNARE`, `TOM_FLOOR`, `KICK`
- `file`: an asset path beginning with `drums/`
- `license`: the exact SPDX identifier or contract reference
- `sourceUrl`: the source or purchase page using HTTPS
- `licenseUrl`: the applicable license or authorization record using HTTPS

Example schema (do not copy it without the referenced licensed file):

```json
{
  "samples": [
    {
      "drumId": "SNARE",
      "file": "drums/snare.wav",
      "license": "CC0-1.0",
      "sourceUrl": "https://example.com/source",
      "licenseUrl": "https://creativecommons.org/publicdomain/zero/1.0/"
    }
  ]
}
```

The runtime only treats a file as a licensed production sample when its complete manifest entry is present. Missing or invalid entries keep the deterministic synthesized fallback active.
