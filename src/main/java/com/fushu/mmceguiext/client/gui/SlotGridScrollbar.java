package com.fushu.mmceguiext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.Rectangle;

public final class SlotGridScrollbar {
    private static final ResourceLocation DEFAULT_SCROLLBAR_TEXTURE =
        new ResourceLocation("minecraft", "textures/gui/container/creative_inventory/tabs.png");

    private final boolean horizontal;
    private final int left;
    private final int top;
    private final int width;
    private final int height;
    private final int thumbWidth;
    private final int thumbHeight;
    @Nullable
    private final ResourceLocation texture;
    @Nullable
    private final ResourceLocation hoverTexture;
    @Nullable
    private final ResourceLocation pressedTexture;
    @Nullable
    private final ResourceLocation disabledTexture;
    private final int textureWidth;
    private final int textureHeight;
    private final int u;
    private final int v;
    private final int hoverU;
    private final int hoverV;
    private final int pressedU;
    private final int pressedV;
    private final int disabledU;
    private final int disabledV;
    private int pageSize = 1;
    private int minScroll = 0;
    private int maxScroll = 0;
    private int currentScroll = 0;
    private boolean pressed;
    private int dragOffset;

    public SlotGridScrollbar(Config config) {
        this.horizontal = config.horizontal;
        this.left = config.left;
        this.top = config.top;
        this.width = Math.max(1, config.width);
        this.height = Math.max(1, config.height);
        this.thumbWidth = Math.max(1, config.thumbWidth);
        this.thumbHeight = Math.max(1, config.thumbHeight);
        this.texture = config.texture;
        this.hoverTexture = config.hoverTexture;
        this.pressedTexture = config.pressedTexture;
        this.disabledTexture = config.disabledTexture;
        this.textureWidth = Math.max(1, config.textureWidth);
        this.textureHeight = Math.max(1, config.textureHeight);
        this.u = config.u;
        this.v = config.v;
        this.hoverU = config.hoverU;
        this.hoverV = config.hoverV;
        this.pressedU = config.pressedU;
        this.pressedV = config.pressedV;
        this.disabledU = config.disabledU;
        this.disabledV = config.disabledV;
        setRange(config.minScroll, config.maxScroll, config.pageSize);
    }

    public void setRange(int min, int max, int pageSize) {
        this.minScroll = min;
        this.maxScroll = Math.max(min, max);
        this.pageSize = Math.max(1, pageSize);
        applyRange();
    }

    public void setCurrentScroll(int currentScroll) {
        this.currentScroll = currentScroll;
        applyRange();
    }

    public int getCurrentScroll() {
        return this.currentScroll;
    }

    public int getMinScroll() {
        return this.minScroll;
    }

    public int getMaxScroll() {
        return this.maxScroll;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getRange() {
        return this.maxScroll - this.minScroll;
    }

    public int getLeft() {
        return this.left;
    }

    public int getTop() {
        return this.top;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getThumbWidth() {
        return this.thumbWidth;
    }

    public int getThumbHeight() {
        return this.thumbHeight;
    }

    public boolean isHorizontal() {
        return this.horizontal;
    }

    public boolean isMouseOver(int x, int y) {
        return x >= this.left && x < this.left + this.width && y >= this.top && y < this.top + this.height;
    }

    public boolean isPressed() {
        return this.pressed;
    }

    public int getThumbTravel() {
        return Math.max(1, this.horizontal ? this.width - this.thumbWidth : this.height - this.thumbHeight);
    }

    public int getThumbOffset() {
        if (getRange() <= 0) {
            return 0;
        }
        return (this.currentScroll - this.minScroll) * getThumbTravel() / getRange();
    }

    public Rectangle getBounds() {
        return new Rectangle(this.left, this.top, this.width, this.height);
    }

    public Rectangle getThumbBounds() {
        int thumbOffset = getThumbOffset();
        int thumbLeft = this.left + (this.horizontal ? thumbOffset : 0);
        int thumbTop = this.top + (this.horizontal ? 0 : thumbOffset);
        return new Rectangle(thumbLeft, thumbTop, this.thumbWidth, this.thumbHeight);
    }

    public boolean isMouseOverThumb(int x, int y) {
        Rectangle thumb = getThumbBounds();
        return x >= thumb.x && x < thumb.x + thumb.width && y >= thumb.y && y < thumb.y + thumb.height;
    }

    public boolean click(int x, int y) {
        if (getRange() == 0 || !isMouseOver(x, y)) {
            return false;
        }
        int previous = this.currentScroll;
        if (isMouseOverThumb(x, y)) {
            this.pressed = true;
            this.dragOffset = this.horizontal ? x - (this.left + getThumbOffset()) : y - (this.top + getThumbOffset());
        } else {
            this.pressed = false;
            this.dragOffset = (this.horizontal ? this.thumbWidth : this.thumbHeight) / 2;
            int available = getThumbTravel();
            int mousePosition = this.horizontal ? x - this.left : y - this.top;
            int thumbPosition = Math.max(0, Math.min(available, mousePosition - this.dragOffset));
            this.currentScroll = this.minScroll + Math.round((thumbPosition * getRange()) / (float) available);
            applyRange();
            this.dragOffset = 0;
        }
        return this.currentScroll != previous || isMouseOver(x, y);
    }

    public void release() {
        this.pressed = false;
        this.dragOffset = 0;
    }

    public boolean dragTo(int mouseX, int mouseY) {
        if (!this.pressed || getRange() <= 0) {
            return false;
        }
        int available = getThumbTravel();
        int mousePosition = this.horizontal ? mouseX - this.left : mouseY - this.top;
        int thumbPosition = Math.max(0, Math.min(available, mousePosition - this.dragOffset));
        int previous = this.currentScroll;
        this.currentScroll = this.minScroll + Math.round((thumbPosition * getRange()) / (float) available);
        applyRange();
        return this.currentScroll != previous;
    }

    public boolean wheel(int delta) {
        int normalized = Math.max(Math.min(-delta, 1), -1);
        int previous = this.currentScroll;
        this.currentScroll += normalized * this.pageSize;
        applyRange();
        return this.currentScroll != previous;
    }

    public void draw(Gui gui, Minecraft mc, int mouseX, int mouseY) {
        if (getRange() == 0) {
            ResourceLocation tex = this.disabledTexture != null
                ? this.disabledTexture
                : this.texture == null ? DEFAULT_SCROLLBAR_TEXTURE : this.texture;
            mc.getTextureManager().bindTexture(tex);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            Gui.drawModalRectWithCustomSizedTexture(
                this.left,
                this.top,
                this.disabledU,
                this.disabledV,
                this.thumbWidth,
                this.thumbHeight,
                this.textureWidth,
                this.textureHeight
            );
            return;
        }

        int offset = getThumbOffset();
        ResourceLocation tex = this.texture == null ? DEFAULT_SCROLLBAR_TEXTURE : this.texture;
        int drawU = this.u;
        int drawV = this.v;
        if (this.pressed) {
            if (this.pressedTexture != null) {
                tex = this.pressedTexture;
            }
            drawU = this.pressedU;
            drawV = this.pressedV;
        } else if (isMouseOverThumb(mouseX, mouseY)) {
            if (this.hoverTexture != null) {
                tex = this.hoverTexture;
            }
            drawU = this.hoverU;
            drawV = this.hoverV;
        }
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        Gui.drawModalRectWithCustomSizedTexture(
            this.left + (this.horizontal ? offset : 0),
            this.top + (this.horizontal ? 0 : offset),
            drawU,
            drawV,
            this.thumbWidth,
            this.thumbHeight,
            this.textureWidth,
            this.textureHeight
        );
    }

    private void applyRange() {
        this.currentScroll = Math.max(Math.min(this.currentScroll, this.maxScroll), this.minScroll);
    }

    public static final class Config {
        public boolean horizontal;
        public int left;
        public int top;
        public int width = 12;
        public int height = 16;
        public int thumbWidth = 12;
        public int thumbHeight = 15;
        public int pageSize = 1;
        public int minScroll = 0;
        public int maxScroll = 0;
        @Nullable
        public ResourceLocation texture;
        @Nullable
        public ResourceLocation hoverTexture;
        @Nullable
        public ResourceLocation pressedTexture;
        @Nullable
        public ResourceLocation disabledTexture;
        public int textureWidth = 256;
        public int textureHeight = 256;
        public int u = 232;
        public int v = 0;
        public int hoverU = 232;
        public int hoverV = 0;
        public int pressedU = 232;
        public int pressedV = 0;
        public int disabledU = 244;
        public int disabledV = 0;
    }
}
