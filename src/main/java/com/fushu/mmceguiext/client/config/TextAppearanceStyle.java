package com.fushu.mmceguiext.client.config;

import javax.annotation.Nullable;

/**
 * Shared text appearance configuration. Used for texts, buttons, labels,
 * slider values, Smart Interface titles/info/controls/input, and more.
 */
public final class TextAppearanceStyle {
    @Nullable
    public Float scale;
    @Nullable
    public Integer color;
    @Nullable
    public Boolean shadow;
    @Nullable
    public String align;
    @Nullable
    public Float charSpacing;
    @Nullable
    public Float lineSpacing;
    @Nullable
    public Integer maxWidth;
    @Nullable
    public Boolean wrap;

    public TextAppearanceStyle() {
    }

    @Nullable
    public static TextAppearanceStyle copyOf(@Nullable TextAppearanceStyle source) {
        if (source == null) {
            return null;
        }
        TextAppearanceStyle copy = new TextAppearanceStyle();
        copy.scale = source.scale;
        copy.color = source.color;
        copy.shadow = source.shadow;
        copy.align = source.align;
        copy.charSpacing = source.charSpacing;
        copy.lineSpacing = source.lineSpacing;
        copy.maxWidth = source.maxWidth;
        copy.wrap = source.wrap;
        return copy;
    }

    /**
     * Merge explicit values from overlay into this instance. Returns this for chaining.
     */
    public TextAppearanceStyle mergeFrom(@Nullable TextAppearanceStyle overlay) {
        if (overlay == null) {
            return this;
        }
        if (overlay.scale != null) this.scale = overlay.scale;
        if (overlay.color != null) this.color = overlay.color;
        if (overlay.shadow != null) this.shadow = overlay.shadow;
        if (overlay.align != null) this.align = overlay.align;
        if (overlay.charSpacing != null) this.charSpacing = overlay.charSpacing;
        if (overlay.lineSpacing != null) this.lineSpacing = overlay.lineSpacing;
        if (overlay.maxWidth != null) this.maxWidth = overlay.maxWidth;
        if (overlay.wrap != null) this.wrap = overlay.wrap;
        return this;
    }

    /**
     * Build a flattened instance with fallback flat fields and nested textStyle override.
     * Flat fields act as defaults; nested textStyle fields override them.
     */
    @Nullable
    public static TextAppearanceStyle fromFlatAndNested(
        @Nullable Float flatScale,
        @Nullable Integer flatColor,
        @Nullable Boolean flatShadow,
        @Nullable String flatAlign,
        @Nullable Float flatCharSpacing,
        @Nullable TextAppearanceStyle nestedStyle
    ) {
        if (flatScale == null && flatColor == null && flatShadow == null
            && flatAlign == null && flatCharSpacing == null && nestedStyle == null) {
            return null;
        }
        TextAppearanceStyle result = new TextAppearanceStyle();
        result.scale = flatScale;
        result.color = flatColor;
        result.shadow = flatShadow;
        result.align = flatAlign;
        result.charSpacing = flatCharSpacing;
        if (nestedStyle != null) {
            result.mergeFrom(nestedStyle);
        }
        return result;
    }
}
