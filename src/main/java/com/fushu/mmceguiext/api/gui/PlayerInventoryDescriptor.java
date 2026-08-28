package com.fushu.mmceguiext.api.gui;

import javax.annotation.Nullable;

/**
 * Immutable player-inventory layout description.
 */
public final class PlayerInventoryDescriptor {
    public final int x;
    public final int y;
    public final int hotbarX;
    public final int hotbarY;
    public final int mainStart;
    public final int hotbarStart;
    public final boolean enabled;

    public PlayerInventoryDescriptor(int x,
                                     int y,
                                     int hotbarX,
                                     int hotbarY,
                                     int mainStart,
                                     int hotbarStart,
                                     boolean enabled) {
        this.x = x;
        this.y = y;
        this.hotbarX = hotbarX;
        this.hotbarY = hotbarY;
        this.mainStart = mainStart;
        this.hotbarStart = hotbarStart;
        this.enabled = enabled;
    }

    public PlayerInventoryDescriptor(int x, int y, int hotbarX, int hotbarY, boolean enabled) {
        this(x, y, hotbarX, hotbarY, 0, 27, enabled);
    }

    public PlayerInventoryDescriptor(int x, int y, int hotbarX, int hotbarY) {
        this(x, y, hotbarX, hotbarY, 0, 27, true);
    }

    public static PlayerInventoryDescriptor copyOf(@Nullable PlayerInventoryDescriptor source) {
        if (source == null) {
            return null;
        }
        return new PlayerInventoryDescriptor(
            source.x,
            source.y,
            source.hotbarX,
            source.hotbarY,
            source.mainStart,
            source.hotbarStart,
            source.enabled
        );
    }
}
