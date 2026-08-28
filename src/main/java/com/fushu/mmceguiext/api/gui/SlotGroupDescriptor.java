package com.fushu.mmceguiext.api.gui;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable description of a machine-owned slot group.
 *
 * <p>Continuous groups use {@code firstSlot + slotCount}. Sparse groups use
 * {@code slotIndices}; when present, sparse indices take precedence.</p>
 */
public final class SlotGroupDescriptor {
    public final String id;
    public final int firstSlot;
    public final int slotCount;
    public final int x;
    public final int y;
    public final int rows;
    public final int columns;
    public final int spacingX;
    public final int spacingY;
    @Nullable
    public final String shiftTarget;
    public final boolean enabled;

    private final int[] slotIndices;

    public SlotGroupDescriptor(String id,
                               int firstSlot,
                               int slotCount,
                               int x,
                               int y,
                               int rows,
                               int columns,
                               int spacingX,
                               int spacingY,
                               @Nullable String shiftTarget,
                               boolean enabled) {
        this(id, firstSlot, slotCount, null, x, y, rows, columns,
            spacingX, spacingY, shiftTarget, enabled);
    }

    public SlotGroupDescriptor(String id,
                               int[] slotIndices,
                               int x,
                               int y,
                               int rows,
                               int columns,
                               int spacingX,
                               int spacingY,
                               @Nullable String shiftTarget,
                               boolean enabled) {
        this(id, -1, slotIndices == null ? 0 : slotIndices.length, slotIndices,
            x, y, rows, columns, spacingX, spacingY, shiftTarget, enabled);
    }

    private SlotGroupDescriptor(String id,
                                int firstSlot,
                                int slotCount,
                                @Nullable int[] slotIndices,
                                int x,
                                int y,
                                int rows,
                                int columns,
                                int spacingX,
                                int spacingY,
                                @Nullable String shiftTarget,
                                boolean enabled) {
        this.id = Objects.requireNonNull(id, "slotGroup id must not be null");
        this.firstSlot = firstSlot;
        this.slotIndices = slotIndices == null ? null : slotIndices.clone();
        this.slotCount = this.slotIndices == null
            ? Math.max(0, slotCount)
            : this.slotIndices.length;
        this.x = x;
        this.y = y;
        this.rows = Math.max(1, rows);
        this.columns = Math.max(1, columns);
        this.spacingX = Math.max(1, spacingX);
        this.spacingY = Math.max(1, spacingY);
        this.shiftTarget = shiftTarget;
        this.enabled = enabled;
    }

    /**
     * Returns a defensive copy of sparse slot indices, or {@code null} for a
     * continuous group.
     */
    @Nullable
    public int[] getSlotIndices() {
        return slotIndices == null ? null : slotIndices.clone();
    }

    public boolean hasExplicitSlotIndices() {
        return slotIndices != null && slotIndices.length > 0;
    }

    public static SlotGroupDescriptor copyOf(@Nullable SlotGroupDescriptor source) {
        if (source == null) {
            return null;
        }
        return new SlotGroupDescriptor(
            source.id,
            source.firstSlot,
            source.slotCount,
            source.slotIndices,
            source.x,
            source.y,
            source.rows,
            source.columns,
            source.spacingX,
            source.spacingY,
            source.shiftTarget,
            source.enabled
        );
    }

    @Override
    public String toString() {
        return "SlotGroupDescriptor{" +
            "id='" + id + '\'' +
            ", firstSlot=" + firstSlot +
            ", slotCount=" + slotCount +
            ", slotIndices=" + Arrays.toString(slotIndices) +
            ", x=" + x +
            ", y=" + y +
            ", rows=" + rows +
            ", columns=" + columns +
            ", spacingX=" + spacingX +
            ", spacingY=" + spacingY +
            ", enabled=" + enabled +
            '}';
    }
}
