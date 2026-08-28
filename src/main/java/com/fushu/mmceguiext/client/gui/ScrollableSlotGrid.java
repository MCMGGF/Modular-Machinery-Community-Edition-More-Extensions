package com.fushu.mmceguiext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.Locale;

public final class ScrollableSlotGrid {
    public static final int HIDDEN_SLOT_X = -10000;
    public static final int HIDDEN_SLOT_Y = -10000;

    private final String key;
    private final Metrics metrics;
    private final int baseIndex;
    private final int baseX;
    private final int baseY;
    private final int rows;
    private final int columns;
    private final int visibleRows;
    private final int visibleColumns;
    private final int spacingX;
    private final int spacingY;
    private final int slotSize;
    private final Axis scrollAxis;
    private final ScrollMode scrollMode;
    private final int maxScroll;
    private final int pageSize;
    private final boolean scrollbarEnabled;
    @Nullable
    private final SlotGridScrollbar scrollbar;

    private ScrollableSlotGrid(Config config, Metrics metrics) {
        this.key = config.key == null ? "" : config.key;
        this.metrics = metrics;
        this.baseIndex = config.baseIndex;
        this.baseX = config.baseX;
        this.baseY = config.baseY;
        this.rows = Math.max(1, config.rows);
        this.columns = Math.max(1, config.columns);
        this.visibleRows = normalizeVisible(config.visibleRows, this.rows);
        this.visibleColumns = normalizeVisible(config.visibleColumns, this.columns);
        this.spacingX = Math.max(0, config.spacingX);
        this.spacingY = Math.max(0, config.spacingY);
        this.slotSize = Math.max(1, config.slotSize);
        this.scrollMode = config.scrollMode == ScrollMode.PAGE ? ScrollMode.PAGE : ScrollMode.ROW;
        this.scrollAxis = resolveScrollAxis(config.scrollAxis, this.rows, this.columns, this.visibleRows, this.visibleColumns);
        this.maxScroll = computeMaxScroll(this.scrollAxis, this.rows, this.columns, this.visibleRows, this.visibleColumns);
        this.pageSize = this.scrollMode == ScrollMode.PAGE
            ? (this.scrollAxis == Axis.HORIZONTAL ? this.visibleColumns : this.visibleRows)
            : 1;
        this.scrollbarEnabled = config.scrollbarEnabled;
        this.scrollbar = this.maxScroll > 0 ? createScrollbar(config) : null;
    }

    public static ScrollableSlotGrid create(Config config, Metrics metrics) {
        return new ScrollableSlotGrid(config, metrics);
    }

    public String getKey() {
        return this.key;
    }

    public int getBaseIndex() {
        return this.baseIndex;
    }

    public int getRows() {
        return this.rows;
    }

    public int getColumns() {
        return this.columns;
    }

    public int getVisibleRows() {
        return this.visibleRows;
    }

    public int getVisibleColumns() {
        return this.visibleColumns;
    }

    public Axis getScrollAxis() {
        return this.scrollAxis;
    }

    public ScrollMode getScrollMode() {
        return this.scrollMode;
    }

    public int getMaxScroll() {
        return this.maxScroll;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getCurrentScroll() {
        return this.scrollbar == null ? 0 : this.scrollbar.getCurrentScroll();
    }

    public boolean canScroll() {
        return this.maxScroll > 0 && this.scrollbar != null;
    }

    public boolean isScrollbarEnabled() {
        return this.scrollbarEnabled;
    }

    @Nullable
    public SlotGridScrollbar getScrollbar() {
        return this.scrollbar;
    }

    public boolean containsAbsoluteIndex(int absoluteIndex) {
        int localIndex = absoluteIndex - this.baseIndex;
        return localIndex >= 0 && localIndex < this.rows * this.columns;
    }

    public SlotPosition positionForAbsoluteIndex(int absoluteIndex) {
        return positionForLocalIndex(absoluteIndex - this.baseIndex);
    }

    public SlotPosition positionForLocalIndex(int localIndex) {
        if (localIndex < 0 || localIndex >= this.rows * this.columns) {
            return SlotPosition.hidden();
        }
        int row = localIndex / this.columns;
        int column = localIndex % this.columns;
        int firstVisibleRow = this.scrollAxis == Axis.VERTICAL ? getCurrentScroll() : 0;
        int firstVisibleColumn = this.scrollAxis == Axis.HORIZONTAL ? getCurrentScroll() : 0;
        if (row < firstVisibleRow || row >= firstVisibleRow + this.visibleRows
            || column < firstVisibleColumn || column >= firstVisibleColumn + this.visibleColumns) {
            return SlotPosition.hidden();
        }
        int visibleRow = row - firstVisibleRow;
        int visibleColumn = column - firstVisibleColumn;
        int x = this.metrics.scaledX(this.baseX + visibleColumn * stepX()) - this.metrics.backgroundOffsetX();
        int y = this.metrics.scaledY(this.baseY + visibleRow * stepY()) - this.metrics.backgroundOffsetY();
        return new SlotPosition(true, x, y, row, column, visibleRow, visibleColumn);
    }

    public boolean wheel(int delta) {
        return this.scrollbar != null && this.scrollbar.wheel(delta);
    }

    public boolean clickScrollbar(int mouseX, int mouseY) {
        if (!this.scrollbarEnabled || this.scrollbar == null || !this.scrollbar.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollbar.click(mouseX, mouseY);
        return true;
    }

    public void releaseScrollbar() {
        if (this.scrollbar != null) {
            this.scrollbar.release();
        }
    }

    public boolean dragScrollbar(int mouseX, int mouseY) {
        return this.scrollbarEnabled && this.scrollbar != null && this.scrollbar.isPressed() && this.scrollbar.dragTo(mouseX, mouseY);
    }

    public boolean isDraggingScrollbar() {
        return this.scrollbarEnabled && this.scrollbar != null && this.scrollbar.isPressed();
    }

    public void drawScrollbar(Gui gui, Minecraft mc, int mouseX, int mouseY) {
        if (this.scrollbarEnabled && this.scrollbar != null && this.maxScroll > 0) {
            this.scrollbar.draw(gui, mc, mouseX, mouseY);
        }
    }

    public boolean isMouseOverGrid(int mouseX, int mouseY) {
        Rectangle bounds = getVisibleGridBounds();
        return mouseX >= bounds.x && mouseX < bounds.x + bounds.width
            && mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
    }

    public Rectangle getVisibleGridBounds() {
        return new Rectangle(
            this.metrics.guiLeft() + this.metrics.scaledX(this.baseX) - this.metrics.backgroundOffsetX(),
            this.metrics.guiTop() + this.metrics.scaledY(this.baseY) - this.metrics.backgroundOffsetY(),
            this.metrics.scaledWidth(visibleWidth()),
            this.metrics.scaledHeight(visibleHeight())
        );
    }

    public void unionBounds(Rectangle bounds) {
        Rectangle grid = getVisibleGridBounds();
        union(bounds, grid.x, grid.y, grid.width, grid.height);
        if (this.scrollbarEnabled && this.scrollbar != null && this.maxScroll > 0) {
            Rectangle scrollbarBounds = this.scrollbar.getBounds();
            union(bounds, scrollbarBounds.x, scrollbarBounds.y, scrollbarBounds.width, scrollbarBounds.height);
        }
    }

    public int visibleWidth() {
        return this.visibleColumns * this.slotSize + Math.max(0, this.visibleColumns - 1) * this.spacingX;
    }

    public int visibleHeight() {
        return this.visibleRows * this.slotSize + Math.max(0, this.visibleRows - 1) * this.spacingY;
    }

    public static Axis resolveScrollAxis(@Nullable String rawAxis, int rows, int columns, int visibleRows, int visibleColumns) {
        Axis explicit = parseAxis(rawAxis);
        if (explicit != null) {
            return explicit;
        }
        boolean canScrollColumns = normalizeVisible(visibleColumns, Math.max(1, columns)) < Math.max(1, columns);
        boolean canScrollRows = normalizeVisible(visibleRows, Math.max(1, rows)) < Math.max(1, rows);
        return canScrollColumns && !canScrollRows ? Axis.HORIZONTAL : Axis.VERTICAL;
    }

    @Nullable
    public static Axis parseAxis(@Nullable String rawAxis) {
        if (rawAxis == null) {
            return null;
        }
        String axis = rawAxis.trim().toLowerCase(Locale.ROOT);
        if (axis.isEmpty()) {
            return null;
        }
        if ("horizontal".equals(axis) || "x".equals(axis) || "left_to_right".equals(axis) || "ltr".equals(axis)) {
            return Axis.HORIZONTAL;
        }
        if ("vertical".equals(axis) || "y".equals(axis) || "top_to_bottom".equals(axis) || "ttb".equals(axis)
            || "bottom_to_top".equals(axis) || "btt".equals(axis)) {
            return Axis.VERTICAL;
        }
        return null;
    }

    public static ScrollMode parseScrollMode(@Nullable String rawMode) {
        return rawMode != null && "page".equalsIgnoreCase(rawMode.trim()) ? ScrollMode.PAGE : ScrollMode.ROW;
    }

    public static int computeMaxScroll(Axis axis, int rows, int columns, int visibleRows, int visibleColumns) {
        return axis == Axis.HORIZONTAL
            ? Math.max(0, Math.max(1, columns) - normalizeVisible(visibleColumns, Math.max(1, columns)))
            : Math.max(0, Math.max(1, rows) - normalizeVisible(visibleRows, Math.max(1, rows)));
    }

    private SlotGridScrollbar createScrollbar(Config config) {
        boolean horizontal = this.scrollAxis == Axis.HORIZONTAL;
        int scrollbarX = config.scrollbarX != 0
            ? config.scrollbarX
            : horizontal ? this.baseX : this.baseX + this.visibleColumns * (this.slotSize + this.spacingX) + 2;
        int scrollbarY = config.scrollbarY != 0
            ? config.scrollbarY
            : horizontal ? this.baseY + this.visibleRows * (this.slotSize + this.spacingY) + 2 : this.baseY;
        int scrollbarLength = config.scrollbarLength > 0
            ? config.scrollbarLength
            : horizontal ? (config.scrollbarWidth != 12 ? config.scrollbarWidth : visibleWidth()) : (config.scrollbarHeight > 0 ? config.scrollbarHeight : visibleHeight());

        SlotGridScrollbar.Config scrollbarConfig = new SlotGridScrollbar.Config();
        scrollbarConfig.horizontal = horizontal;
        scrollbarConfig.left = this.metrics.guiLeft() + this.metrics.scaledX(scrollbarX) - this.metrics.backgroundOffsetX();
        scrollbarConfig.top = this.metrics.guiTop() + this.metrics.scaledY(scrollbarY) - this.metrics.backgroundOffsetY();
        scrollbarConfig.width = horizontal
            ? Math.max(15, this.metrics.scaledWidth(scrollbarLength))
            : Math.max(4, this.metrics.scaledWidth(config.scrollbarWidth));
        scrollbarConfig.height = horizontal
            ? Math.max(4, this.metrics.scaledHeight(config.scrollbarHeight > 0 ? config.scrollbarHeight : 12))
            : Math.max(15, this.metrics.scaledHeight(scrollbarLength));
        scrollbarConfig.thumbWidth = horizontal
            ? Math.max(8, this.metrics.scaledWidth(config.scrollbarThumbWidth > 0 ? config.scrollbarThumbWidth : config.scrollbarThumbHeight))
            : scrollbarConfig.width;
        scrollbarConfig.thumbHeight = horizontal
            ? scrollbarConfig.height
            : Math.max(8, this.metrics.scaledHeight(config.scrollbarThumbHeight));
        scrollbarConfig.minScroll = 0;
        scrollbarConfig.maxScroll = this.maxScroll;
        scrollbarConfig.pageSize = this.pageSize;
        scrollbarConfig.texture = config.scrollbarTexture;
        scrollbarConfig.hoverTexture = config.scrollbarHoverTexture;
        scrollbarConfig.pressedTexture = config.scrollbarPressedTexture;
        scrollbarConfig.disabledTexture = config.scrollbarDisabledTexture;
        scrollbarConfig.textureWidth = Math.max(1, config.scrollbarTextureWidth);
        scrollbarConfig.textureHeight = Math.max(1, config.scrollbarTextureHeight);
        scrollbarConfig.u = config.scrollbarU;
        scrollbarConfig.v = config.scrollbarV;
        scrollbarConfig.hoverU = config.scrollbarHoverU;
        scrollbarConfig.hoverV = config.scrollbarHoverV;
        scrollbarConfig.pressedU = config.scrollbarPressedU;
        scrollbarConfig.pressedV = config.scrollbarPressedV;
        scrollbarConfig.disabledU = config.scrollbarDisabledU;
        scrollbarConfig.disabledV = config.scrollbarDisabledV;
        return new SlotGridScrollbar(scrollbarConfig);
    }

    private int stepX() {
        return this.slotSize + this.spacingX;
    }

    private int stepY() {
        return this.slotSize + this.spacingY;
    }

    private static int normalizeVisible(int visible, int total) {
        int safeTotal = Math.max(1, total);
        return visible > 0 ? Math.min(visible, safeTotal) : safeTotal;
    }

    private static void union(Rectangle bounds, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            bounds.add(new Rectangle(x, y, width, height));
        }
    }

    public enum ScrollMode {
        ROW,
        PAGE
    }

    public enum Axis {
        VERTICAL,
        HORIZONTAL
    }

    public interface Metrics {
        int guiLeft();

        int guiTop();

        int backgroundOffsetX();

        int backgroundOffsetY();

        int scaledX(int textureX);

        int scaledY(int textureY);

        int scaledWidth(int textureWidth);

        int scaledHeight(int textureHeight);
    }

    public static final class Config {
        public String key;
        public int baseIndex;
        public int baseX;
        public int baseY;
        public int rows = 1;
        public int columns = 1;
        public int visibleRows;
        public int visibleColumns;
        public int spacingX = 2;
        public int spacingY = 2;
        public int slotSize = 16;
        public ScrollMode scrollMode = ScrollMode.ROW;
        @Nullable
        public String scrollAxis;
        public boolean scrollbarEnabled = true;
        public int scrollbarX;
        public int scrollbarY;
        public int scrollbarLength;
        public int scrollbarHeight;
        public int scrollbarWidth = 12;
        public int scrollbarThumbHeight = 15;
        public int scrollbarThumbWidth;
        @Nullable
        public ResourceLocation scrollbarTexture;
        @Nullable
        public ResourceLocation scrollbarHoverTexture;
        @Nullable
        public ResourceLocation scrollbarPressedTexture;
        @Nullable
        public ResourceLocation scrollbarDisabledTexture;
        public int scrollbarTextureWidth = 256;
        public int scrollbarTextureHeight = 256;
        public int scrollbarU = 232;
        public int scrollbarV = 0;
        public int scrollbarHoverU = 232;
        public int scrollbarHoverV = 0;
        public int scrollbarPressedU = 232;
        public int scrollbarPressedV = 0;
        public int scrollbarDisabledU = 244;
        public int scrollbarDisabledV = 0;
    }

    public static final class SlotPosition {
        public final boolean visible;
        public final int x;
        public final int y;
        public final int row;
        public final int column;
        public final int visibleRow;
        public final int visibleColumn;

        private SlotPosition(boolean visible, int x, int y, int row, int column, int visibleRow, int visibleColumn) {
            this.visible = visible;
            this.x = x;
            this.y = y;
            this.row = row;
            this.column = column;
            this.visibleRow = visibleRow;
            this.visibleColumn = visibleColumn;
        }

        private static SlotPosition hidden() {
            return new SlotPosition(false, HIDDEN_SLOT_X, HIDDEN_SLOT_Y, -1, -1, -1, -1);
        }
    }
}
