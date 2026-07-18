package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.api.gui.PlayerInventoryDescriptor;
import com.fushu.mmceguiext.api.gui.SlotGroupDescriptor;
import com.fushu.mmceguiext.api.gui.SlotLayoutProvider;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ControllerSlotLayoutEngineTest {
    @Test
    public void styleOnlyGroupsAllocateAfterPlayerSlotsAndAllowPartialLastRow() {
        List<Slot> slots = slots(42);
        MachineGuiStyleManager.SlotGroupStyle style = style("input", 10, 20, 2, 3);
        style.slotCount = Integer.valueOf(5);

        ControllerSlotLayoutEngine.apply(
            slots,
            null,
            Collections.singletonList(style),
            null,
            new RecordingWarnings()
        );

        assertPosition(slots.get(36), 10, 20);
        assertPosition(slots.get(37), 28, 20);
        assertPosition(slots.get(38), 46, 20);
        assertPosition(slots.get(39), 10, 38);
        assertPosition(slots.get(40), 28, 38);
        assertPosition(slots.get(41), 0, 0);
    }

    @Test
    public void styleGeometryOverridesProviderButSparseIndicesAreInherited() {
        List<Slot> slots = slots(64);
        SlotGroupDescriptor providerGroup = new SlotGroupDescriptor(
            "input",
            new int[] {52, 48, 60},
            1,
            2,
            1,
            3,
            18,
            18,
            null,
            true
        );
        SlotLayoutProvider provider = provider(
            Collections.singletonList(providerGroup),
            null
        );
        MachineGuiStyleManager.SlotGroupStyle style = style("input", 70, 12, 1, 3);
        style.spacingX = Integer.valueOf(20);

        ControllerSlotLayoutEngine.apply(
            slots,
            provider,
            Collections.singletonList(style),
            null,
            new RecordingWarnings()
        );

        assertPosition(slots.get(52), 70, 12);
        assertPosition(slots.get(48), 90, 12);
        assertPosition(slots.get(60), 110, 12);
    }

    @Test
    public void disabledGroupMovesEveryMappedSlotOffscreen() {
        List<Slot> slots = slots(48);
        MachineGuiStyleManager.SlotGroupStyle style = style("disabled", 10, 10, 1, 2);
        style.slotIndices = new int[] {40, 44};
        style.enabled = Boolean.FALSE;

        ControllerSlotLayoutEngine.apply(
            slots,
            null,
            Collections.singletonList(style),
            null,
            new RecordingWarnings()
        );

        assertPosition(
            slots.get(40),
            ControllerSlotLayoutEngine.HIDDEN_SLOT_COORDINATE,
            ControllerSlotLayoutEngine.HIDDEN_SLOT_COORDINATE
        );
        assertPosition(
            slots.get(44),
            ControllerSlotLayoutEngine.HIDDEN_SLOT_COORDINATE,
            ControllerSlotLayoutEngine.HIDDEN_SLOT_COORDINATE
        );
    }

    @Test
    public void duplicateAndOutOfBoundsMappingsWarnAndAreSkipped() {
        List<Slot> slots = slots(40);
        MachineGuiStyleManager.SlotGroupStyle first = style("first", 10, 10, 1, 1);
        first.slotIndices = new int[] {36};
        MachineGuiStyleManager.SlotGroupStyle second = style("second", 20, 20, 1, 2);
        second.slotIndices = new int[] {36, 99};
        RecordingWarnings warnings = new RecordingWarnings();

        ControllerSlotLayoutEngine.apply(
            slots,
            null,
            Arrays.asList(first, second),
            null,
            warnings
        );

        assertPosition(slots.get(36), 10, 10);
        assertTrue(warnings.contains("repeats slot index 36"));
        assertTrue(warnings.contains("outside container bounds"));
    }

    @Test
    public void playerInventoryUsesExplicitMainAndHotbarStarts() {
        List<Slot> slots = slots(50);
        SlotLayoutProvider provider = provider(
            Collections.<SlotGroupDescriptor>emptyList(),
            new PlayerInventoryDescriptor(8, 100, 8, 158, 5, 40, true)
        );

        ControllerSlotLayoutEngine.apply(
            slots,
            provider,
            null,
            null,
            new RecordingWarnings()
        );

        assertPosition(slots.get(5), 8, 100);
        assertPosition(slots.get(31), 152, 136);
        assertPosition(slots.get(40), 8, 158);
        assertPosition(slots.get(48), 152, 158);
    }

    @Test
    public void styleOnlyGroupsAllocateAfterCustomPlayerSlotIndicesEvenWhenHidden() {
        List<Slot> slots = slots(64);
        SlotLayoutProvider provider = provider(
            Collections.<SlotGroupDescriptor>emptyList(),
            new PlayerInventoryDescriptor(8, 100, 8, 158, 10, 50, false)
        );
        MachineGuiStyleManager.SlotGroupStyle style = style("machine", 20, 30, 1, 1);
        style.slotCount = Integer.valueOf(1);

        ControllerSlotLayoutEngine.apply(
            slots,
            provider,
            Collections.singletonList(style),
            null,
            new RecordingWarnings()
        );

        assertPosition(
            slots.get(10),
            ControllerSlotLayoutEngine.HIDDEN_SLOT_COORDINATE,
            ControllerSlotLayoutEngine.HIDDEN_SLOT_COORDINATE
        );
        assertPosition(slots.get(59), 20, 30);
    }

    @Test
    public void excessEnabledMappingsDoNotReserveSlotsForLaterGroups() {
        List<Slot> slots = slots(48);
        MachineGuiStyleManager.SlotGroupStyle overflow = style("overflow", 10, 10, 1, 1);
        overflow.slotIndices = new int[] {40, 41};
        MachineGuiStyleManager.SlotGroupStyle later = style("later", 30, 30, 1, 1);
        later.slotCount = Integer.valueOf(1);
        RecordingWarnings warnings = new RecordingWarnings();

        ControllerSlotLayoutEngine.apply(
            slots,
            null,
            Arrays.asList(overflow, later),
            null,
            warnings
        );

        assertPosition(slots.get(40), 10, 10);
        assertPosition(slots.get(41), 30, 30);
        assertTrue(warnings.contains("excess mappings skipped"));
    }

    private static List<Slot> slots(int count) {
        InventoryBasic inventory = new InventoryBasic("test", false, count);
        List<Slot> slots = new ArrayList<Slot>(count);
        for (int i = 0; i < count; i++) {
            slots.add(new Slot(inventory, i, 0, 0));
        }
        return slots;
    }

    private static MachineGuiStyleManager.SlotGroupStyle style(
        String id, int x, int y, int rows, int columns
    ) {
        MachineGuiStyleManager.SlotGroupStyle style = new MachineGuiStyleManager.SlotGroupStyle();
        style.id = id;
        style.x = Integer.valueOf(x);
        style.y = Integer.valueOf(y);
        style.rows = Integer.valueOf(rows);
        style.columns = Integer.valueOf(columns);
        return style;
    }

    private static SlotLayoutProvider provider(
        final List<SlotGroupDescriptor> groups,
        @Nullable final PlayerInventoryDescriptor player
    ) {
        return new SlotLayoutProvider() {
            @Override
            public List<SlotGroupDescriptor> getSlotGroups() {
                return groups;
            }

            @Nullable
            @Override
            public PlayerInventoryDescriptor getPlayerInventory() {
                return player;
            }
        };
    }

    private static void assertPosition(Slot slot, int x, int y) {
        assertEquals(x, slot.xPos);
        assertEquals(y, slot.yPos);
    }

    private static final class RecordingWarnings implements ControllerSlotLayoutEngine.WarningSink {
        private final List<String> messages = new ArrayList<String>();

        @Override
        public void warn(String message) {
            messages.add(message);
        }

        private boolean contains(String fragment) {
            for (String message : messages) {
                if (message.contains(fragment)) {
                    return true;
                }
            }
            return false;
        }
    }
}
