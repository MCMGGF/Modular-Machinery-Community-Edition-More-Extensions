# Modular Machinery: Community Edition More Extensions 1.4.1

MMCE More Extensions 1.4.1 is the first release after 1.4.0 under the full project name. The project was formerly known as MMCE GUI Edit / MMCEGE.

This release keeps the existing technical identifiers for compatibility:

- Mod id: `mmceguiext`
- Config path: `config/mmceguiext/`
- Java packages and public API
- CraftTweaker namespace: `mods.mmceguiext`
- Runtime command prefix: `mmcege:`

## Fixed in 1.4.1

- Fixed controller button actions being dispatched more than once.
- Fixed automatic factory-controller scrollbar height calculation.
- Fixed controller slider dragging regressions, including default and custom background paths and modal sub-GUI input routing.
- Fixed virtual Smart Interface linkage and mirrored successful writes into controller `customData`.
- Improved Mouse Tweaks / Cleanroom class-loading compatibility without transforming or bundling Mouse Tweaks classes.

## Configuration and compatibility

- Added the shared `maxGuiConfigFileSizeMiB` setting in `config/mmceguiext/client.cfg`.
- The default limit is `8 MiB`; valid values are clamped to `1-64 MiB`.
- A file exactly equal to the configured limit is accepted. Files are skipped only when they exceed the limit.
- The shared limit applies to controller GUI JSON, standalone styles, sub-GUIs, external GUI styles, custom hatches, custom AE buses, capacity cards, and controller button policy files.
- This removes the previous hard-coded 1 MiB ceiling for MMCEME extension JSON files while keeping bounded reads.

## Controller GUI

- Resizable ordinary and factory-controller GUIs with custom backgrounds, nine-slice rendering, background offsets, panels, texture layers, and runtime directives.
- Smart Interface editors and virtual DataPort input boxes, including multiple editors and configurable visibility.
- GUI-local hotkeys on visible buttons and invisible top-level `hotkeys[]` actions.
- Hotkey actions can open modal or replacement sub-GUIs, close a sub-GUI, switch pages, fire server-side events, or write Smart Interface values.
- Text-field focus keeps normal typing priority so letters such as `C` are not consumed while entering data.
- Buttons support normal, hover, pressed, and disabled textures, with independent UV and source-size fields.
- Sliders support dragging, numeric stepping, custom track/thumb colors, textures, pages, and virtual Smart Interface values.
- Factory-controller thread queue positioning, row sizing, special-thread tinting, and configurable track/thumb scrollbar textures.
- Negative automatic scrollbar height (`height: -1`) remains supported for the factory thread queue.

## Dynamic visuals and animation

- Dynamic visual sources support controller `customData`, machine metrics, and combined sources.
- Added dynamic `minSource` / `maxSource` bounds with `customData`, `machine`, or `combined` sources.
- Added renderer switching, dynamic visibility, color, alpha, translation, scale, rotation, pivots, and history charts.
- Added fill bars, pie/ring charts, line charts, value-driven texture switching, and time-driven PNG animation.
- `animatedTexture` supports both PNG sprite sheets and ordered PNG frame lists. GIF decoding and automatic GIF playback are not included.
- Large values may still lose precision in float-oriented GUI paths; exact large integer handling should remain on the server/script side.

## Slot layouts and custom hatches

- Added a shared slot-layout engine for ordinary and factory-controller integrations.
- Custom hatch `slot_grid` / `slots` supports both vertical and horizontal scrolling.
- Added `visibleColumns`, `scrollAxis`, `scrollMode`, page scrolling, scrollbar textures, pressed/hover states, and horizontal thumb sizing.
- Sparse slot indexes, partially filled final rows, disabled groups, and bounds validation are supported.
- Existing vertical configurations remain compatible.
- Custom hatches can define item, fluid, gas, energy, and combined components from JSON, including long capacities and custom GUI layouts.

## Long-capacity requirements

- Long V2 requirement types remain experimental and disabled by default:
  - `mmceguiext:fluid_long`
  - `mmceguiext:gas_long`
- Enable them with `experimental.enableLongFluidGasRequirements=true`, then fully restart.
- Long amounts support JSON integers and decimal strings; quoted strings are recommended for large values.
- Parsing, start checks, input consumption, output production, and maximum parallelism calculation use the long path for ordinary start/finish requirements.
- Long per-tick requirement types are not included in this release.
- Normal `fluid` / `gas` requirements keep MMCE behavior and now report a migration hint instead of silently overflowing above `Integer.MAX_VALUE`.

## Custom AE2 buses

- Experimental JSON-defined ME item input, mixed item+fluid+gas input, and mixed item+fluid+gas output buses.
- Added sparse component indexes, improved storage/config mapping, capacity-card handling, external I/O wrappers, and clearer duplicate/bounds validation.
- Classic custom AE bus registration still targets the `appeng.*` API and requires the supported AE2 + Mekanism Energistics combination.
- AE2S may be detected as an AE implementation for startup compatibility, but native AE2S custom-bus support remains postponed.

## Optional static parallel-controller tiers

- Added an experimental opt-in extension for configurable static MMCE parallel-controller tiers.
- Enable `experimental.enableCustomParallelControllerTiers=true` and restart.
- Register `mmceme_0` through `mmceme_15` with `customParallelControllerTierCount`.
- Configure per-tier maximum parallelism with `customParallelControllerMaxParallelisms`, with a default fallback.
- The tiers reuse MMCE's native controller block, tile, GUI, recipe, and parallelism behavior.
- If WhimCraft already owns the same MMCE parallel-controller enum extension, MMCEME does not add a second extension.
- This feature does not implement runtime per-machine dynamic parallelism rules.

## Upgrade notes

- Replace the old `MMCEGE-1.4.0.jar` with `Modular-Machinery-Community-Edition-More-Extensions-1.4.1.jar`.
- Keep `mmceguiext` in dependency declarations and config paths.
- Existing GUI JSON, ZS scripts, custom hatch definitions, and custom AE bus definitions remain on their existing technical namespaces.
- Back up the instance before enabling Long V2, custom AE buses, or static parallel-controller tiers.
