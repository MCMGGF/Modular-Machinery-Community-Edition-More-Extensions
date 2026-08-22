# Long-Capacity Requirements V2

[Home](Home) · [中文版](Long-Capacity-Requirements-ZH)

## The problem

MMCE stores the recipe `amount` of `RequirementFluid` / `RequirementGas` as a Java `int`. An `int` overflows above **2,147,483,647** (~2.1 billion mB). Once your machines and custom hatches operate in the billions/trillions of mB, vanilla MMCE recipes silently break.

## What MMCE More Extensions does

MMCE More Extensions 1.4.0 adds Long V2 for recipe fluids / gases. MMCE More Extensions no longer rewrites normal `fluid` / `gas` requirements behind the scenes. Long-capacity recipes must use the explicit `mmceguiext:fluid_long` / `mmceguiext:gas_long` requirement types.

The long-capacity path is currently **experimental and disabled by default** because it may make some fluid/gas recipes fail to take effect in some packs. When enabled, it runs across the whole recipe lifecycle:

- parse (`createRequirement`)
- "can start crafting" check
- input consumption (`startCrafting`)
- output filling (`finishCrafting`)
- max-parallelism calculation

Vanilla, small-amount recipes are unaffected. If you keep using normal `fluid` / `gas`, they stay on the original MMCE path.

## How to use it

Enable the experimental option first:

```properties
experimental {
    B:enableLongFluidGasRequirements=true
}
```

Then fully restart the game and write a large `amount` in your normal MMCE recipe JSON:

```json
{
  "type": "mmceguiext:fluid_long",
  "io-type": "input",
  "fluid": "water",
  "amount": "5000000000"
}
```

```json
{
  "type": "mmceguiext:gas_long",
  "io-type": "output",
  "gas": "hydrogen",
  "amount": 8000000000
}
```

`amount` accepts either a non-negative JSON integer or a base-10 string. For very large values, a string is recommended. Long V2 requirements parse and run only while `experimental.enableLongFluidGasRequirements` is enabled. When disabled, the types stay registered to keep client/server registries identical, but loading a Long V2 recipe produces a clear configuration error.

## Migration from the old scheme

- Keep normal `fluid` / `gas` when the amount still fits in `int`.
- Switch long-capacity entries to `mmceguiext:fluid_long` / `mmceguiext:gas_long`.
- Replace `io` with `io-type`.
- Prefer quoted `amount` values for big numbers.

## Why it matters here

This patch is what makes the **long-capacity custom hatches** and **AE2 mixed buses** actually usable in recipes — their tanks store `long` amounts, and recipes must be able to demand/produce matching `long` amounts without overflow. See [Custom Hatches](Custom-Hatches-EN) and [Custom AE2 Buses](Custom-AE-Buses-EN).

## Notes & limits

- Normal `fluid` / `gas` requirements are not rewritten. Values above `Integer.MAX_VALUE` fail with a migration hint instead of overflowing.
- Custom long hatches and AE2 mixed buses perform real long IO. Plain Forge/Mekanism handlers still contribute only their real int-sized capacity.
- `FluidStack` / `GasStack` represent identity, NBT, and the JEI icon; the full amount is stored directly as a `long` on the Long V2 requirement.
- Without recipe modifiers, the full positive `long` range is supported. MMCE `RecipeModifier` uses `double`, so modifying values above `2^53 - 1` may round; MMCE More Extensions logs this once.
- 1.4.0 provides normal `fluid_long` / `gas_long` requirements only; long per-tick types are not included yet.

## Under the hood (for contributors)

- `RequirementTypeLongFluid` / `RequirementTypeLongGas` parse explicit Long V2 JSON and enforce the experimental option.
- `RequirementLongFluid` / `RequirementLongGas` own the full long amount and implement checks, transfer, parallelism, and JEI tooltips.
- `LongRequirementIO` / `LongGasRequirementIO` create shared all-slot snapshots and adapt both long and ordinary int handlers.
- `MixinRequirementFluid` / `MixinRequirementGas` only make normal and Long V2 requirements share the same long-aware snapshots; they do not replace MMCE execution.
- `MixinRequirementTypeFluidAmountGuard` / `MixinRequirementTypeGasAmountGuard` stop silent vanilla overflow and provide the migration hint.
