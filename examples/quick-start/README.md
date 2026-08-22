# Quick Start Examples

This folder contains small, copy-friendly MMCE GUI extension examples.
The project is branded **MMCE More Extensions (MMCEME)**. Older technical identifiers and config keys still use `MMCEGE` / `mmceguiext` for compatibility.
本项目现名为 **MMCE 更多扩展（MMCEME）**；为保持兼容，旧的技术标识和配置键仍使用 `MMCEGE` / `mmceguiext`。

Use them as a starting point:

- `background-only.json`: only changes the controller background
- `smart-interface-only.json`: only enables Smart Interface editor slots
- `factory-queue-only.json`: only customizes the factory queue and right expansion
- `sliders.json`: adds horizontal and vertical controller sliders that write Smart Interface / virtual DataPort values
- `dynamic-visuals.json`: demonstrates `dynamicVisuals[]`, including weighted multi-source combining, transform-by-value, visibility-by-value, renderer switching, and color interpolation
- `animated-texture.json`: demonstrates `dynamicVisuals[].renderer.type = "animatedTexture"` with both sprite sheet and ordered PNG frame animations
- `horizontal-slot-grid-hatch.json`: demonstrates a custom hatch `slot_grid` that scrolls horizontally with `visibleColumns` + `scrollAxis: "horizontal"`
- `long-requirements-v2.json`: valid MMCE recipe skeleton using experimental `mmceguiext:fluid_long` / `mmceguiext:gas_long`
- `error-cases.json`: intentionally broken or edge-case examples for log validation
- `subgui-page-reference.json`: main machine JSON, intended for `config/modularmachinery/machinery`
- `subgui-page-reference-settings.json`: sub GUI overlay JSON, intended for `config/mmceguiext/subgui`

All files are written as bilingual templates where needed:

- Chinese explains intent for pack authors
- English keeps the config portable and easy to reuse

Page / sub GUI naming rule used by the sample:

- `defaultPageId` should point to the landing page, usually `main`
- every page-specific field uses the same page id in `page`
- a jump button uses `action: "page"` plus `targetPage` with the destination page id
- keep the destination id stable and short, such as `main` and `settings`
- the sample keeps the same `registryname` in both files so the sub GUI can attach to the same machine definition
- `subGuis` entries live in the main machine JSON and use `id` plus `mode` / `openMode`
- a sub GUI open button uses `action: "subgui"` plus `targetSubGui`
- `close_subgui` closes the currently open sub GUI and keeps the main controller visible
- `hotkey` / `hotkeys` can be placed on a button; top-level `hotkeys[]` creates invisible GUI-only hotkey actions such as `C` opening a modal sub GUI
- the sample keeps the same `registryname` in both files so the sub GUI overlays can merge into the same machine definition
- `sliders[]` entries write numeric values through their `key`; use the same key from CraftTweaker via `ctrl.getSmartInterfaceData(key)` or `ctrl.customData`
- `dynamic-visuals.json` uses those slider-written keys (`heat`, `warning`, `load`) to demonstrate both weighted mixed-scale aggregation and same-scale weighted blending across fill, ring visibility, color, renderer switching, rotation, alpha, and scale
