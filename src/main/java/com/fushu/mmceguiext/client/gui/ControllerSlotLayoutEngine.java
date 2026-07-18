package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.api.gui.PlayerInventoryDescriptor;
import com.fushu.mmceguiext.api.gui.SlotGroupDescriptor;
import com.fushu.mmceguiext.api.gui.SlotLayoutProvider;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared slot-layout implementation for normal and factory controller GUIs.
 */
public final class ControllerSlotLayoutEngine {
    public static final int HIDDEN_SLOT_COORDINATE = -1000;
    private static final int DEFAULT_MACHINE_SLOT_START = 36;

    public interface WarningSink {
        void warn(String message);
    }

    private static final WarningSink LOG_WARNING_SINK = new WarningSink() {
        @Override
        public void warn(String message) {
            MMCEGuiExt.logger().warn(message);
        }
    };

    private ControllerSlotLayoutEngine() {
    }

    public static void apply(List<Slot> slots,
                             @Nullable SlotLayoutProvider provider,
                             @Nullable List<MachineGuiStyleManager.SlotGroupStyle> styleGroups,
                             @Nullable MachineGuiStyleManager.PlayerInventoryStyle playerStyle) {
        apply(slots, provider, styleGroups, playerStyle, LOG_WARNING_SINK);
    }

    static void apply(List<Slot> slots,
                      @Nullable SlotLayoutProvider provider,
                      @Nullable List<MachineGuiStyleManager.SlotGroupStyle> styleGroups,
                      @Nullable MachineGuiStyleManager.PlayerInventoryStyle playerStyle,
                      @Nullable WarningSink warningSink) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        WarningSink warnings = warningSink == null ? new WarningSink() {
            @Override
            public void warn(String message) {
            }
        } : warningSink;

        PlayerInventoryDescriptor player = resolvePlayerInventory(provider, playerStyle);
        Set<Integer> occupied = new HashSet<Integer>();
        if (player != null) {
            applyPlayerInventory(slots, player, occupied, warnings);
        }

        List<SlotGroupDescriptor> groups = resolveSlotGroups(
            provider,
            styleGroups,
            resolveMachineSlotStart(player),
            warnings
        );
        for (SlotGroupDescriptor group : groups) {
            applyGroup(slots, group, occupied, warnings);
        }
    }

    @Nullable
    static PlayerInventoryDescriptor resolvePlayerInventory(
        @Nullable SlotLayoutProvider provider,
        @Nullable MachineGuiStyleManager.PlayerInventoryStyle style
    ) {
        PlayerInventoryDescriptor base = provider == null ? null : provider.getPlayerInventory();
        if (base == null && style == null) {
            return null;
        }

        int x = style != null && style.x != null
            ? style.x.intValue() : base == null ? 0 : base.x;
        int y = style != null && style.y != null
            ? style.y.intValue() : base == null ? 0 : base.y;
        int hotbarX = style != null && style.hotbarX != null
            ? style.hotbarX.intValue() : base == null ? x : base.hotbarX;
        int hotbarY = style != null && style.hotbarY != null
            ? style.hotbarY.intValue() : base == null ? y + 58 : base.hotbarY;
        int mainStart = style != null && style.mainStart != null
            ? style.mainStart.intValue() : base == null ? 0 : base.mainStart;
        int hotbarStart = style != null && style.hotbarStart != null
            ? style.hotbarStart.intValue() : base == null ? 27 : base.hotbarStart;
        boolean enabled = style != null && style.enabled != null
            ? style.enabled.booleanValue() : base == null || base.enabled;
        return new PlayerInventoryDescriptor(
            x, y, hotbarX, hotbarY, mainStart, hotbarStart, enabled
        );
    }

    static List<SlotGroupDescriptor> resolveSlotGroups(
        @Nullable SlotLayoutProvider provider,
        @Nullable List<MachineGuiStyleManager.SlotGroupStyle> styles,
        @Nullable WarningSink warningSink
    ) {
        return resolveSlotGroups(provider, styles, DEFAULT_MACHINE_SLOT_START, warningSink);
    }

    private static List<SlotGroupDescriptor> resolveSlotGroups(
        @Nullable SlotLayoutProvider provider,
        @Nullable List<MachineGuiStyleManager.SlotGroupStyle> styles,
        int sequentialStart,
        @Nullable WarningSink warningSink
    ) {
        WarningSink warnings = warningSink == null ? new WarningSink() {
            @Override
            public void warn(String message) {
            }
        } : warningSink;
        List<SlotGroupDescriptor> providerGroups =
            provider == null ? null : provider.getSlotGroups();
        Map<String, MachineGuiStyleManager.SlotGroupStyle> styleById =
            new LinkedHashMap<String, MachineGuiStyleManager.SlotGroupStyle>();
        if (styles != null) {
            for (MachineGuiStyleManager.SlotGroupStyle style : styles) {
                if (style == null || style.id == null || style.id.trim().isEmpty()) {
                    continue;
                }
                String id = style.id.trim();
                if (styleById.put(id, style) != null) {
                    warnings.warn("Duplicate slot group style id '" + id + "'; the last definition wins.");
                }
            }
        }

        List<UnallocatedGroup> merged = new ArrayList<UnallocatedGroup>();
        Set<String> usedStyleIds = new HashSet<String>();
        Set<String> providerIds = new HashSet<String>();
        if (providerGroups != null) {
            for (SlotGroupDescriptor providerGroup : providerGroups) {
                if (providerGroup == null) {
                    continue;
                }
                if (!providerIds.add(providerGroup.id)) {
                    warnings.warn("Duplicate provider slot group id '" + providerGroup.id + "'; later group skipped.");
                    continue;
                }
                MachineGuiStyleManager.SlotGroupStyle overlay = styleById.get(providerGroup.id);
                merged.add(merge(providerGroup, overlay));
                if (overlay != null) {
                    usedStyleIds.add(providerGroup.id);
                }
            }
        }
        for (Map.Entry<String, MachineGuiStyleManager.SlotGroupStyle> entry : styleById.entrySet()) {
            if (!usedStyleIds.contains(entry.getKey())) {
                merged.add(merge(null, entry.getValue()));
            }
        }

        int nextSequential = Math.max(0, sequentialStart);
        for (UnallocatedGroup group : merged) {
            int geometryCapacity = Math.max(1, group.rows) * Math.max(1, group.columns);
            if (group.slotIndices != null) {
                int mappedCount = group.enabled
                    ? Math.min(group.slotIndices.length, geometryCapacity)
                    : group.slotIndices.length;
                for (int i = 0; i < mappedCount; i++) {
                    int index = group.slotIndices[i];
                    nextSequential = Math.max(nextSequential, index + 1);
                }
            } else if (group.firstSlot >= 0) {
                int mappedCount = group.enabled
                    ? Math.min(group.slotCount, geometryCapacity)
                    : group.slotCount;
                nextSequential = Math.max(nextSequential, group.firstSlot + mappedCount);
            }
        }

        List<SlotGroupDescriptor> resolved = new ArrayList<SlotGroupDescriptor>(merged.size());
        for (UnallocatedGroup group : merged) {
            int geometryCapacity = Math.max(1, group.rows) * Math.max(1, group.columns);
            int count = group.slotIndices == null
                ? (group.slotCount > 0 ? group.slotCount : geometryCapacity)
                : group.slotIndices.length;
            if (group.slotIndices != null) {
                resolved.add(new SlotGroupDescriptor(
                    group.id,
                    group.slotIndices,
                    group.x,
                    group.y,
                    group.rows,
                    group.columns,
                    group.spacingX,
                    group.spacingY,
                    group.shiftTarget,
                    group.enabled
                ));
            } else {
                int firstSlot = group.firstSlot;
                if (firstSlot < 0) {
                    firstSlot = nextSequential;
                    nextSequential += count;
                }
                resolved.add(new SlotGroupDescriptor(
                    group.id,
                    firstSlot,
                    count,
                    group.x,
                    group.y,
                    group.rows,
                    group.columns,
                    group.spacingX,
                    group.spacingY,
                    group.shiftTarget,
                    group.enabled
                ));
            }
        }
        return resolved;
    }

    private static int resolveMachineSlotStart(@Nullable PlayerInventoryDescriptor player) {
        if (player == null) {
            return DEFAULT_MACHINE_SLOT_START;
        }
        long mainEnd = (long) player.mainStart + 27L;
        long hotbarEnd = (long) player.hotbarStart + 9L;
        long next = Math.max(0L, Math.max(mainEnd, hotbarEnd));
        return next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next;
    }

    private static UnallocatedGroup merge(
        @Nullable SlotGroupDescriptor provider,
        @Nullable MachineGuiStyleManager.SlotGroupStyle style
    ) {
        UnallocatedGroup out = new UnallocatedGroup();
        out.id = style != null && style.id != null
            ? style.id.trim() : provider == null ? "" : provider.id;

        int[] providerIndices = provider == null ? null : provider.getSlotIndices();
        boolean styleHasIndexMapping = style != null
            && (style.slotIndices != null || style.firstSlot != null || style.slotCount != null);
        if (styleHasIndexMapping && style.slotIndices != null) {
            out.slotIndices = style.slotIndices.clone();
            out.firstSlot = -1;
            out.slotCount = out.slotIndices.length;
        } else if (styleHasIndexMapping) {
            out.slotIndices = null;
            out.firstSlot = style.firstSlot != null
                ? style.firstSlot.intValue() : provider == null ? -1 : provider.firstSlot;
            out.slotCount = style.slotCount != null
                ? style.slotCount.intValue() : provider == null ? 0 : provider.slotCount;
        } else {
            out.slotIndices = providerIndices;
            out.firstSlot = provider == null ? -1 : provider.firstSlot;
            out.slotCount = provider == null ? 0 : provider.slotCount;
        }

        out.x = value(style == null ? null : style.x, provider == null ? 0 : provider.x);
        out.y = value(style == null ? null : style.y, provider == null ? 0 : provider.y);
        out.rows = Math.max(1, value(style == null ? null : style.rows, provider == null ? 1 : provider.rows));
        out.columns = Math.max(1, value(
            style == null ? null : style.columns,
            provider == null ? Math.max(1, out.slotCount) : provider.columns
        ));
        out.spacingX = Math.max(1, value(
            style == null ? null : style.spacingX,
            provider == null ? 18 : provider.spacingX
        ));
        out.spacingY = Math.max(1, value(
            style == null ? null : style.spacingY,
            provider == null ? 18 : provider.spacingY
        ));
        out.shiftTarget = style != null && style.shiftTarget != null
            ? style.shiftTarget : provider == null ? null : provider.shiftTarget;
        out.enabled = style != null && style.enabled != null
            ? style.enabled.booleanValue() : provider == null || provider.enabled;
        return out;
    }

    private static int value(@Nullable Integer overlay, int fallback) {
        return overlay == null ? fallback : overlay.intValue();
    }

    private static void applyPlayerInventory(List<Slot> slots,
                                             PlayerInventoryDescriptor player,
                                             Set<Integer> occupied,
                                             WarningSink warnings) {
        for (int i = 0; i < 27; i++) {
            int slotIndex = player.mainStart + i;
            applyPlayerSlot(slots, slotIndex,
                player.x + (i % 9) * 18,
                player.y + (i / 9) * 18,
                player.enabled,
                occupied,
                warnings);
        }
        for (int i = 0; i < 9; i++) {
            int slotIndex = player.hotbarStart + i;
            applyPlayerSlot(slots, slotIndex,
                player.hotbarX + i * 18,
                player.hotbarY,
                player.enabled,
                occupied,
                warnings);
        }
    }

    private static void applyPlayerSlot(List<Slot> slots,
                                        int slotIndex,
                                        int x,
                                        int y,
                                        boolean enabled,
                                        Set<Integer> occupied,
                                        WarningSink warnings) {
        if (!isValidIndex(slots, slotIndex)) {
            warnings.warn("Player inventory slot index " + slotIndex + " is outside container bounds.");
            return;
        }
        if (!occupied.add(Integer.valueOf(slotIndex))) {
            warnings.warn("Player inventory slot index " + slotIndex + " is duplicated; later mapping skipped.");
            return;
        }
        Slot slot = slots.get(slotIndex);
        if (slot == null) {
            return;
        }
        slot.xPos = enabled ? x : HIDDEN_SLOT_COORDINATE;
        slot.yPos = enabled ? y : HIDDEN_SLOT_COORDINATE;
    }

    private static void applyGroup(List<Slot> slots,
                                   SlotGroupDescriptor group,
                                   Set<Integer> occupied,
                                   WarningSink warnings) {
        int[] indices = group.getSlotIndices();
        if (indices == null) {
            indices = new int[group.slotCount];
            for (int i = 0; i < group.slotCount; i++) {
                indices[i] = group.firstSlot + i;
            }
        }
        int geometryCapacity = group.rows * group.columns;
        if (group.enabled && indices.length > geometryCapacity) {
            warnings.warn("Slot group '" + group.id + "' maps " + indices.length
                + " slots into geometry capacity " + geometryCapacity + "; excess mappings skipped.");
        }
        for (int i = 0; i < indices.length; i++) {
            if (group.enabled && i >= geometryCapacity) {
                continue;
            }
            int slotIndex = indices[i];
            if (!isValidIndex(slots, slotIndex)) {
                warnings.warn("Slot group '" + group.id + "' index " + slotIndex
                    + " is outside container bounds; mapping skipped.");
                continue;
            }
            if (!occupied.add(Integer.valueOf(slotIndex))) {
                warnings.warn("Slot group '" + group.id + "' repeats slot index " + slotIndex
                    + "; later mapping skipped.");
                continue;
            }
            Slot slot = slots.get(slotIndex);
            if (slot == null) {
                continue;
            }
            if (!group.enabled) {
                slot.xPos = HIDDEN_SLOT_COORDINATE;
                slot.yPos = HIDDEN_SLOT_COORDINATE;
                continue;
            }
            int row = i / group.columns;
            int column = i % group.columns;
            slot.xPos = group.x + column * group.spacingX;
            slot.yPos = group.y + row * group.spacingY;
        }
    }

    private static boolean isValidIndex(List<Slot> slots, int index) {
        return index >= 0 && index < slots.size();
    }

    private static final class UnallocatedGroup {
        private String id;
        private int firstSlot;
        private int slotCount;
        @Nullable private int[] slotIndices;
        private int x;
        private int y;
        private int rows;
        private int columns;
        private int spacingX;
        private int spacingY;
        @Nullable private String shiftTarget;
        private boolean enabled;
    }
}
