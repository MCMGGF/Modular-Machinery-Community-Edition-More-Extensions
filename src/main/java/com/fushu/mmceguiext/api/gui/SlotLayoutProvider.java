package com.fushu.mmceguiext.api.gui;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Implemented by containers or tile entities that want to expose their slot layout
 * so the GUI framework can arrange inventory slots and player inventory position
 * without hard-coding positions.
 */
public interface SlotLayoutProvider {

    /**
     * Returns the slot groups owned by this provider.
     * Each group describes a rectangular grid of slots at a given GUI position.
     */
    List<SlotGroupDescriptor> getSlotGroups();

    /**
     * Returns the player inventory layout for this GUI, or null to use the default.
     */
    @Nullable
    PlayerInventoryDescriptor getPlayerInventory();
}
