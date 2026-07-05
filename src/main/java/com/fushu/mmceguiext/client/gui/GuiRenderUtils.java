package com.fushu.mmceguiext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class GuiRenderUtils {
    private GuiRenderUtils() {
    }

    public static ResourceLocation parseTexture(String value, ResourceLocation fallback) {
        ResourceLocation texture = parseOptionalTexture(value);
        return texture == null ? fallback : texture;
    }

    @Nullable
    public static ResourceLocation parseOptionalTexture(String value) {
        String raw = normalizeRawTexturePath(value);
        if (raw == null) {
            return null;
        }
        try {
            ResourceLocation original = new ResourceLocation(raw);
            if (resourceExists(original)) {
                return original;
            }

            String normalized = normalizeGuiTexturePath(raw);
            if (normalized != null && !normalized.equals(raw)) {
                ResourceLocation guiTexture = new ResourceLocation(normalized);
                if (resourceExists(guiTexture)) {
                    return guiTexture;
                }
            }

            return original;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static ResourceLocation parseLooseTexture(String value) {
        return parseOptionalTexture(value);
    }

    public static ResourceLocation parseTexture(String value) {
        return parseOptionalTexture(value);
    }

    public static boolean hasTexture(String value) {
        return parseOptionalTexture(value) != null;
    }

    @Nullable
    private static String normalizeRawTexturePath(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim().replace('\\', '/');
        if (text.isEmpty() || text.contains("..") || text.startsWith("/") || text.matches("^[A-Za-z]:.*")) {
            return null;
        }
        return text;
    }

    @Nullable
    private static String normalizeGuiTexturePath(String value) {
        String text = normalizeRawTexturePath(value);
        if (text == null) {
            return null;
        }
        int separator = text.indexOf(':');
        if (separator < 0) {
            return text;
        }

        String domain = text.substring(0, separator).trim();
        String path = text.substring(separator + 1).trim();
        if (domain.isEmpty() || path.isEmpty()) {
            return null;
        }
        if (!path.startsWith("textures/")) {
            path = path.startsWith("gui/") ? "textures/" + path : "textures/gui/" + path;
        }
        return domain + ":" + path;
    }

    private static boolean resourceExists(ResourceLocation texture) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null) {
                return false;
            }
            IResourceManager manager = minecraft.getResourceManager();
            if (manager == null) {
                return false;
            }
            manager.getResource(texture);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void enableScissor(Minecraft mc, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        int scale = resolution.getScaleFactor();
        int scissorX = x * scale;
        int scissorY = mc.displayHeight - (y + height) * scale;
        int scissorWidth = width * scale;
        int scissorHeight = height * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static boolean isMouseInPanel(int mouseX, int mouseY, Rectangle panel, int guiLeft, int guiTop) {
        int left = guiLeft + panel.x;
        int top = guiTop + panel.y;
        int right = left + panel.width;
        int bottom = top + panel.height;
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    public static void drawNineSlice(int x, int y, int width, int height, int textureWidth, int textureHeight, int corner) {
        drawNineSlice(x, y, width, height, 0, 0, textureWidth, textureHeight, corner);
    }

    public static void drawTexturedRect(
        int x,
        int y,
        int u,
        int v,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawScaledTexturedRect(
        int x,
        int y,
        int width,
        int height,
        int u,
        int v,
        int sourceWidth,
        int sourceHeight,
        int textureWidth,
        int textureHeight
    ) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }

        double u0 = (double) u / (double) textureWidth;
        double v0 = (double) v / (double) textureHeight;
        double u1 = (double) (u + sourceWidth) / (double) textureWidth;
        double v1 = (double) (v + sourceHeight) / (double) textureHeight;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos((double) x, (double) (y + height), 0.0D).tex(u0, v1).endVertex();
        buffer.pos((double) (x + width), (double) (y + height), 0.0D).tex(u1, v1).endVertex();
        buffer.pos((double) (x + width), (double) y, 0.0D).tex(u1, v0).endVertex();
        buffer.pos((double) x, (double) y, 0.0D).tex(u0, v0).endVertex();
        tessellator.draw();
    }

    public static void drawNineSlice(
        int x,
        int y,
        int width,
        int height,
        int u,
        int v,
        int textureWidth,
        int textureHeight,
        int corner
    ) {
        if (width <= 0 || height <= 0 || textureWidth <= 0 || textureHeight <= 0) {
            return;
        }

        int maxCorner = Math.max(1, Math.min(textureWidth, textureHeight) / 2);
        int srcCorner = Math.max(1, Math.min(corner, maxCorner));
        int destCorner = Math.max(1, Math.min(srcCorner, Math.min(width, height) / 2));

        int srcMiddleWidth = Math.max(1, textureWidth - srcCorner * 2);
        int srcMiddleHeight = Math.max(1, textureHeight - srcCorner * 2);
        int destMiddleWidth = Math.max(0, width - destCorner * 2);
        int destMiddleHeight = Math.max(0, height - destCorner * 2);

        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, destCorner, destCorner, textureWidth, textureHeight);
        Gui.drawModalRectWithCustomSizedTexture(
            x + width - destCorner, y, u + textureWidth - srcCorner, v, destCorner, destCorner, textureWidth, textureHeight
        );
        Gui.drawModalRectWithCustomSizedTexture(
            x, y + height - destCorner, u, v + textureHeight - srcCorner, destCorner, destCorner, textureWidth, textureHeight
        );
        Gui.drawModalRectWithCustomSizedTexture(
            x + width - destCorner, y + height - destCorner,
            u + textureWidth - srcCorner, v + textureHeight - srcCorner,
            destCorner, destCorner, textureWidth, textureHeight
        );

        if (destMiddleWidth > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y, u + srcCorner, v, destMiddleWidth, destCorner, textureWidth, textureHeight
            );
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y + height - destCorner,
                u + srcCorner, v + textureHeight - srcCorner,
                destMiddleWidth, destCorner, textureWidth, textureHeight
            );
        }

        if (destMiddleHeight > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x, y + destCorner, u, v + srcCorner, destCorner, destMiddleHeight, textureWidth, textureHeight
            );
            Gui.drawModalRectWithCustomSizedTexture(
                x + width - destCorner, y + destCorner,
                u + textureWidth - srcCorner, v + srcCorner, destCorner, destMiddleHeight, textureWidth, textureHeight
            );
        }

        if (destMiddleWidth > 0 && destMiddleHeight > 0) {
            Gui.drawModalRectWithCustomSizedTexture(
                x + destCorner, y + destCorner,
                u + srcCorner, v + srcCorner, destMiddleWidth, destMiddleHeight, textureWidth, textureHeight
            );
        }
    }

    public static int parseColorARGBOrDefault(@Nullable String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return fallback;
        }

        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.startsWith("0x") || text.startsWith("0X")) {
            text = text.substring(2);
        }

        try {
            if (text.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(text, 16));
            }
            if (text.length() == 8) {
                return (int) Long.parseLong(text, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    public static void applyColorARGB(int color) {
        float a = ((color >>> 24) & 0xFF) / 255.0F;
        float r = ((color >>> 16) & 0xFF) / 255.0F;
        float g = ((color >>> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, a);
    }

    public static int resolveAlignedTextX(int anchorX, int textWidth, @Nullable String align) {
        if (textWidth <= 0 || align == null) {
            return anchorX;
        }
        if ("center".equalsIgnoreCase(align)) {
            return anchorX - textWidth / 2;
        }
        if ("right".equalsIgnoreCase(align)) {
            return anchorX - textWidth;
        }
        return anchorX;
    }

    public static float resolveCharSpacing(@Nullable Float override, float fallback) {
        if (override != null && Float.isFinite(override.floatValue())) {
            return override.floatValue();
        }
        return Float.isFinite(fallback) ? fallback : 0.0F;
    }

    public static float sanitizeCharSpacing(@Nullable Float value) {
        return value != null && Float.isFinite(value.floatValue()) ? value.floatValue() : 0.0F;
    }

    public static int drawString(
        FontRenderer fontRenderer,
        String text,
        float x,
        float y,
        int color,
        boolean shadow,
        float charSpacing
    ) {
        if (text == null) {
            return Math.round(x);
        }
        if (!Float.isFinite(charSpacing) || charSpacing == 0.0F || countVisibleTextChars(text) <= 1) {
            return shadow
                ? fontRenderer.drawStringWithShadow(text, x, y, color)
                : fontRenderer.drawString(text, x, y, color, false);
        }

        String activeFormatting = "";
        int visible = countVisibleTextChars(text);
        int drawn = 0;
        float cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                activeFormatting = updateFormatting(activeFormatting, text.charAt(i + 1));
                i++;
                continue;
            }
            String glyph = activeFormatting + c;
            if (shadow) {
                fontRenderer.drawStringWithShadow(glyph, cursorX, y, color);
            } else {
                fontRenderer.drawString(glyph, cursorX, y, color, false);
            }
            cursorX += fontRenderer.getStringWidth(glyph);
            drawn++;
            if (drawn < visible) {
                cursorX += charSpacing;
            }
        }
        return Math.round(cursorX);
    }

    public static float getStringWidth(FontRenderer fontRenderer, @Nullable String text, float charSpacing) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        return getStringWidthWithCharSpacing(fontRenderer.getStringWidth(text), text, charSpacing);
    }

    public static float getStringWidthWithCharSpacing(int vanillaWidth, @Nullable String text, float charSpacing) {
        if (text == null || text.isEmpty() || !Float.isFinite(charSpacing) || charSpacing == 0.0F) {
            return vanillaWidth;
        }
        int visible = countVisibleTextChars(text);
        if (visible <= 1) {
            return vanillaWidth;
        }
        return vanillaWidth + charSpacing * (visible - 1);
    }

    public static int countVisibleTextChars(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            count++;
        }
        return count;
    }

    public static List<String> listFormattedStringToWidth(
        FontRenderer fontRenderer,
        String text,
        int width,
        float charSpacing
    ) {
        if (text == null) {
            return new ArrayList<String>();
        }
        if (!Float.isFinite(charSpacing) || charSpacing == 0.0F) {
            return fontRenderer.listFormattedStringToWidth(text, width);
        }

        List<String> out = new ArrayList<String>();
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            wrapParagraph(fontRenderer, paragraph, Math.max(1, width), charSpacing, out);
        }
        return out;
    }

    private static void wrapParagraph(
        FontRenderer fontRenderer,
        String paragraph,
        int width,
        float charSpacing,
        List<String> out
    ) {
        if (paragraph == null || paragraph.isEmpty()) {
            out.add("");
            return;
        }

        StringBuilder line = new StringBuilder();
        String activeFormatting = "";
        int lastWhitespace = -1;
        String formattingAtWhitespace = "";

        for (int i = 0; i < paragraph.length(); i++) {
            char c = paragraph.charAt(i);
            if (c == '\u00A7' && i + 1 < paragraph.length()) {
                char code = paragraph.charAt(i + 1);
                line.append(c).append(code);
                activeFormatting = updateFormatting(activeFormatting, code);
                i++;
                continue;
            }

            line.append(c);
            if (Character.isWhitespace(c)) {
                lastWhitespace = line.length();
                formattingAtWhitespace = activeFormatting;
            }

            if (getStringWidth(fontRenderer, line.toString(), charSpacing) <= width || countVisibleTextChars(line.toString()) <= 1) {
                continue;
            }

            if (lastWhitespace > 0) {
                String emit = trimTrailingWhitespace(line.substring(0, lastWhitespace));
                if (!emit.isEmpty() || out.isEmpty()) {
                    out.add(emit);
                }
                String remainder = trimLeadingWhitespace(line.substring(lastWhitespace));
                line.setLength(0);
                if (!formattingAtWhitespace.isEmpty()) {
                    line.append(formattingAtWhitespace);
                }
                line.append(remainder);
                activeFormatting = activeFormattingAtEnd(line.toString());
            } else {
                String currentGlyph = String.valueOf(c);
                line.setLength(Math.max(0, line.length() - 1));
                String emit = line.toString();
                if (!emit.isEmpty()) {
                    out.add(emit);
                }
                line.setLength(0);
                if (!activeFormatting.isEmpty()) {
                    line.append(activeFormatting);
                }
                line.append(currentGlyph);
            }
            lastWhitespace = -1;
            formattingAtWhitespace = activeFormatting;
        }

        if (line.length() > 0) {
            out.add(line.toString());
        }
    }

    private static String trimTrailingWhitespace(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return end == value.length() ? value : value.substring(0, end);
    }

    private static String trimLeadingWhitespace(String value) {
        int start = 0;
        while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
            start++;
        }
        return start == 0 ? value : value.substring(start);
    }

    private static String activeFormattingAtEnd(String text) {
        String active = "";
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\u00A7' && i + 1 < text.length()) {
                active = updateFormatting(active, text.charAt(i + 1));
                i++;
            }
        }
        return active;
    }

    private static String updateFormatting(String active, char rawCode) {
        char code = Character.toLowerCase(rawCode);
        if (code == 'r') {
            return "";
        }
        if ("0123456789abcdef".indexOf(code) >= 0) {
            return "\u00A7" + String.valueOf(rawCode);
        }
        if ("klmno".indexOf(code) >= 0) {
            String marker = "\u00A7" + String.valueOf(rawCode);
            return active.indexOf(marker) >= 0 ? active : active + marker;
        }
        return active;
    }
}
