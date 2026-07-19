# Long 容量配方需求 V2

[首页](Home) · [English](Long-Capacity-Requirements-EN)

## 问题

MMCE 的 `RequirementFluid` / `RequirementGas` 用 Java `int` 存储配方 `amount`。`int` 超过 **2,147,483,647**（约 21 亿 mB）就会溢出。一旦你的机器和自定义仓口进入十亿/万亿 mB 级别，原版 MMCE 配方会悄然出错。

## MMCEGE 的做法

MMCEGE 1.4.0 为流体 / 气体配方引入了 Long V2。MMCEGE 不再在后台改写普通 `fluid` / `gas` 需求；需要长容量时，请改用显式的 `mmceguiext:fluid_long` / `mmceguiext:gas_long` 需求类型。

长容量路径当前是**实验性功能，默认关闭**，因为在部分整合包中可能导致流体 / 气体配方无法生效。开启后覆盖整个配方生命周期：

- 解析（`createRequirement`）
- “能否开始合成”判定
- 消耗输入（`startCrafting`）
- 产出填充（`finishCrafting`）
- 最大并行度计算

原版小数值配方不受影响。只要继续使用普通 `fluid` / `gas`，它们就仍然走原本的 MMCE 路径。

## 如何使用

先开启实验性配置：

```properties
experimental {
    B:enableLongFluidGasRequirements=true
}
```

然后完整重启游戏，再在普通的 MMCE 配方 JSON 里写大数值：

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

`amount` 可以写成非负整数，也可以写成十进制字符串；大数值推荐用字符串。只有 `experimental.enableLongFluidGasRequirements` 开启时，两种 Long V2 需求才允许解析和运行。关闭时类型仍会注册，以保持客户端与服务端注册表一致，但加载对应配方会给出明确的配置错误。

## 旧方案迁移

- 普通 `fluid` / `gas` 只要还在 `int` 范围内，就继续保留，不用改。
- 需要长容量的配方条目改成 `mmceguiext:fluid_long` / `mmceguiext:gas_long`。
- 把旧的 `io` 改成 `io-type`。
- 大数值 `amount` 尽量写成字符串。

## 它为何重要

正是这一改造，让 **long 容量的自定义仓口** 与 **AE2 混合总线** 能在配方中真正可用——它们的储罐以 `long` 存储数量，配方也必须能够无溢出地索取/产出对应的 `long` 数量。参见 [自定义仓口](Custom-Hatches-ZH) 与 [自定义 AE2 总线](Custom-AE-Buses-ZH)。

## 注意与限制

- 普通 `fluid` / `gas` 不会被改写；当数值超过 `Integer.MAX_VALUE` 时，解析器会明确提示改用 Long V2。
- 自定义 long 仓口与 AE2 混合总线可完成真实 long IO；普通 Forge/Mekanism handler 仍只能按其真实 `int` 容量参与。
- `FluidStack` / `GasStack` 只用于表示种类、NBT 和 JEI 图标；完整数量保存在 Long V2 requirement 的 `long` 字段中。
- 无配方修改器时支持完整正 `long` 范围。MMCE 的 `RecipeModifier` 使用 `double`，修改超过 `2^53 - 1` 的数量时可能发生舍入，MMCEGE 会记录一次警告。
- 1.4.0 仅提供普通 `fluid_long` / `gas_long`；long per-tick 类型尚未包含。

## 实现细节（贡献者向）

- `RequirementTypeLongFluid` / `RequirementTypeLongGas` —— 解析显式 Long V2 JSON，并在实验选项关闭时给出错误。
- `RequirementLongFluid` / `RequirementLongGas` —— 保存完整 `long amount`，实现检查、输入、输出、并行和 JEI tooltip。
- `LongRequirementIO` / `LongGasRequirementIO` —— 对所有槽位建立共享快照，并统一适配 long handler 与普通 int handler。
- `MixinRequirementFluid` / `MixinRequirementGas` —— 只负责让普通需求与 Long V2 需求共享同一套 long 快照，不覆盖 MMCE 的配方执行。
- `MixinRequirementTypeFluidAmountGuard` / `MixinRequirementTypeGasAmountGuard` —— 阻止普通需求静默溢出，并提供迁移提示。
