package com.fushu.mmceguiext.client.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScrollableSlotGridTest {
    @Test
    public void resolvesExplicitAndAutomaticAxes() {
        assertEquals(ScrollableSlotGrid.Axis.HORIZONTAL, ScrollableSlotGrid.resolveScrollAxis("horizontal", 3, 9, 3, 4));
        assertEquals(ScrollableSlotGrid.Axis.HORIZONTAL, ScrollableSlotGrid.resolveScrollAxis("x", 3, 9, 3, 4));
        assertEquals(ScrollableSlotGrid.Axis.VERTICAL, ScrollableSlotGrid.resolveScrollAxis("vertical", 9, 3, 4, 3));
        assertEquals(ScrollableSlotGrid.Axis.HORIZONTAL, ScrollableSlotGrid.resolveScrollAxis(null, 3, 9, 3, 4));
        assertEquals(ScrollableSlotGrid.Axis.VERTICAL, ScrollableSlotGrid.resolveScrollAxis(null, 9, 3, 4, 3));
        assertEquals(ScrollableSlotGrid.Axis.VERTICAL, ScrollableSlotGrid.resolveScrollAxis(null, 9, 9, 4, 4));
    }

    @Test
    public void horizontalPageScrollMapsVisibleColumnsAndHidesOverflow() {
        ScrollableSlotGrid grid = ScrollableSlotGrid.create(horizontalConfig(), identityMetrics());

        assertEquals(ScrollableSlotGrid.Axis.HORIZONTAL, grid.getScrollAxis());
        assertEquals(3, grid.getMaxScroll());
        assertEquals(3, grid.getPageSize());
        assertPosition(grid.positionForLocalIndex(0), true, 10, 20, 0, 0);
        assertPosition(grid.positionForLocalIndex(3), false, ScrollableSlotGrid.HIDDEN_SLOT_X, ScrollableSlotGrid.HIDDEN_SLOT_Y, -1, -1);

        assertTrue(grid.wheel(-120));
        assertEquals(3, grid.getCurrentScroll());
        assertPosition(grid.positionForLocalIndex(0), false, ScrollableSlotGrid.HIDDEN_SLOT_X, ScrollableSlotGrid.HIDDEN_SLOT_Y, -1, -1);
        assertPosition(grid.positionForLocalIndex(3), true, 10, 20, 0, 3);
        assertPosition(grid.positionForLocalIndex(5), true, 46, 20, 0, 5);
    }

    @Test
    public void verticalRowScrollPreservesLegacyDefault() {
        ScrollableSlotGrid.Config config = baseConfig();
        config.rows = 5;
        config.columns = 2;
        config.visibleRows = 2;
        config.visibleColumns = 2;
        config.scrollAxis = null;
        config.scrollMode = ScrollableSlotGrid.ScrollMode.ROW;
        ScrollableSlotGrid grid = ScrollableSlotGrid.create(config, identityMetrics());

        assertEquals(ScrollableSlotGrid.Axis.VERTICAL, grid.getScrollAxis());
        assertEquals(3, grid.getMaxScroll());
        assertEquals(1, grid.getPageSize());
        assertFalse(grid.wheel(120));
        assertTrue(grid.wheel(-120));
        assertEquals(1, grid.getCurrentScroll());
        assertPosition(grid.positionForLocalIndex(0), false, ScrollableSlotGrid.HIDDEN_SLOT_X, ScrollableSlotGrid.HIDDEN_SLOT_Y, -1, -1);
        assertPosition(grid.positionForLocalIndex(2), true, 10, 20, 1, 0);
        assertPosition(grid.positionForLocalIndex(4), true, 10, 38, 2, 0);
    }

    @Test
    public void horizontalScrollbarUsesLengthAlongXAndThumbWidthFallback() {
        ScrollableSlotGrid.Config config = baseConfig();
        config.rows = 1;
        config.columns = 8;
        config.visibleRows = 1;
        config.visibleColumns = 4;
        config.scrollAxis = "horizontal";
        config.scrollbarX = 5;
        config.scrollbarY = 40;
        config.scrollbarLength = 80;
        config.scrollbarHeight = 6;
        config.scrollbarThumbHeight = 13;
        config.scrollbarThumbWidth = 0;
        ScrollableSlotGrid grid = ScrollableSlotGrid.create(config, fixedMetrics(100, 50, 0, 0));
        SlotGridScrollbar scrollbar = grid.getScrollbar();

        assertNotNull(scrollbar);
        assertTrue(scrollbar.isHorizontal());
        assertEquals(105, scrollbar.getLeft());
        assertEquals(90, scrollbar.getTop());
        assertEquals(80, scrollbar.getWidth());
        assertEquals(6, scrollbar.getHeight());
        assertEquals(13, scrollbar.getThumbWidth());
        assertEquals(6, scrollbar.getThumbHeight());
    }

    @Test
    public void scrollbarTrackClickDragAndReleaseClampToRange() {
        ScrollableSlotGrid.Config config = baseConfig();
        config.rows = 1;
        config.columns = 8;
        config.visibleRows = 1;
        config.visibleColumns = 4;
        config.scrollAxis = "horizontal";
        config.scrollbarX = 5;
        config.scrollbarY = 40;
        config.scrollbarLength = 80;
        config.scrollbarHeight = 6;
        config.scrollbarThumbWidth = 20;
        ScrollableSlotGrid grid = ScrollableSlotGrid.create(config, fixedMetrics(100, 50, 0, 0));

        assertTrue(grid.clickScrollbar(106, 91));
        assertTrue(grid.isDraggingScrollbar());
        assertTrue(grid.dragScrollbar(166, 91));
        assertEquals(4, grid.getCurrentScroll());
        grid.releaseScrollbar();
        assertFalse(grid.isDraggingScrollbar());

        ScrollableSlotGrid trackGrid = ScrollableSlotGrid.create(config, fixedMetrics(100, 50, 0, 0));
        assertTrue(trackGrid.clickScrollbar(184, 91));
        assertEquals(4, trackGrid.getCurrentScroll());
    }

    private static ScrollableSlotGrid.Config horizontalConfig() {
        ScrollableSlotGrid.Config config = baseConfig();
        config.rows = 2;
        config.columns = 6;
        config.visibleRows = 2;
        config.visibleColumns = 3;
        config.scrollAxis = "horizontal";
        config.scrollMode = ScrollableSlotGrid.ScrollMode.PAGE;
        return config;
    }

    private static ScrollableSlotGrid.Config baseConfig() {
        ScrollableSlotGrid.Config config = new ScrollableSlotGrid.Config();
        config.baseIndex = 0;
        config.baseX = 10;
        config.baseY = 20;
        config.slotSize = 16;
        config.spacingX = 2;
        config.spacingY = 2;
        return config;
    }

    private static void assertPosition(ScrollableSlotGrid.SlotPosition position, boolean visible, int x, int y, int row, int column) {
        assertEquals(visible, position.visible);
        assertEquals(x, position.x);
        assertEquals(y, position.y);
        assertEquals(row, position.row);
        assertEquals(column, position.column);
    }

    private static ScrollableSlotGrid.Metrics identityMetrics() {
        return fixedMetrics(0, 0, 0, 0);
    }

    private static ScrollableSlotGrid.Metrics fixedMetrics(final int guiLeft, final int guiTop, final int backgroundOffsetX, final int backgroundOffsetY) {
        return new ScrollableSlotGrid.Metrics() {
            @Override
            public int guiLeft() {
                return guiLeft;
            }

            @Override
            public int guiTop() {
                return guiTop;
            }

            @Override
            public int backgroundOffsetX() {
                return backgroundOffsetX;
            }

            @Override
            public int backgroundOffsetY() {
                return backgroundOffsetY;
            }

            @Override
            public int scaledX(int textureX) {
                return textureX;
            }

            @Override
            public int scaledY(int textureY) {
                return textureY;
            }

            @Override
            public int scaledWidth(int textureWidth) {
                return textureWidth;
            }

            @Override
            public int scaledHeight(int textureHeight) {
                return textureHeight;
            }
        };
    }
}
