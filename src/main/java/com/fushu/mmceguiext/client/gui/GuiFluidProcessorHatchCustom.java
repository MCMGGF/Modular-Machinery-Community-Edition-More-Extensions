package com.fushu.mmceguiext.client.gui;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.GlobalGuiStyleManager;
import com.fushu.mmceguiext.common.container.ContainerFluidProcessorHatchCustom;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.util.EnergyAccessHelper;
import com.fushu.mmceguiext.common.util.UnitFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.input.Mouse;

public class GuiFluidProcessorHatchCustom extends GuiContainer {
    private static final ResourceLocation DEFAULT_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/guibar.png");
    private static final ResourceLocation MMCE_FLUID_HATCH_TEXTURE =
        new ResourceLocation("modularmachinery", "textures/gui/guibar.png");
    private static final int PLAYER_INV_TOP = 84;
    private static final int PLAYER_HOTBAR_TOP = 142;

    private final TileEntity owner;
    private final CustomHatchRegistry.CustomHatchDef definition;
    private final java.util.List<CustomHatchRegistry.ComponentDef> components;
    private GlobalGuiStyleManager.StyleFile styleFile = GlobalGuiStyleManager.StyleFile.EMPTY;
    @Nullable
    private ResourceLocation backgroundTexture;
    private List<GlobalTextureLayerConfig.LayerDef> textureLayers = Collections.emptyList();
    private int backgroundOffsetX;
    private int backgroundOffsetY;
    private int backgroundTextureWidth = 176;
    private int backgroundTextureHeight = 166;
    private int coordinateWidth = 176;
    private int coordinateHeight = 166;
    private final Map<String, ScrollableSlotGrid> slotGridStates = new HashMap<String, ScrollableSlotGrid>();

    public GuiFluidProcessorHatchCustom(TileEntity owner, EntityPlayer opening, CustomHatchRegistry.CustomHatchDef definition) {
        super(new ContainerFluidProcessorHatchCustom(owner, opening, definition));
        this.owner = owner;
        this.definition = definition;
        this.components = definition.gui == null ? Collections.emptyList() : definition.gui.components;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        this.styleFile = GlobalGuiStyleManager.load(definition.guiStyleFile);
        this.backgroundTexture = resolveBackgroundTexture();
        this.textureLayers = !this.styleFile.layers.isEmpty()
            ? this.styleFile.layers
            : Collections.<GlobalTextureLayerConfig.LayerDef>emptyList();
        updateBackgroundMetrics();
        if (this.definition.gui != null) {
            this.xSize = this.definition.gui.width > 0 ? this.definition.gui.width : this.backgroundTextureWidth;
            this.ySize = this.definition.gui.height > 0 ? this.definition.gui.height : this.backgroundTextureHeight;
        }
        updateCoordinateMetrics();
        super.initGui();
        updateSlotGridScrollbars();
        applyConfiguredSlotPositions();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        int offsetX = this.backgroundOffsetX;
        int offsetY = this.backgroundOffsetY;
        int texW = this.backgroundTextureWidth;
        int texH = this.backgroundTextureHeight;
        int corner = this.styleFile.background != null && this.styleFile.background.corner != null
            ? this.styleFile.background.corner.intValue()
            : 8;
        boolean useNineSlice = this.styleFile.background != null
            && this.styleFile.background.useNineSlice != null
            && this.styleFile.background.useNineSlice.booleanValue();

        this.mc.getTextureManager().bindTexture(this.backgroundTexture == null ? DEFAULT_TEXTURE : this.backgroundTexture);
        if (useNineSlice) {
            GuiRenderUtils.drawNineSlice(left - offsetX, top - offsetY, this.xSize + offsetX, this.ySize + offsetY, texW, texH, corner);
        } else {
            Gui.drawModalRectWithCustomSizedTexture(left - offsetX, top - offsetY, 0, 0, this.xSize + offsetX, this.ySize + offsetY, texW, texH);
        }
        GlobalTextureLayerConfig.drawLayers(this.textureLayers, false, left, top, offsetX, offsetY);
        drawTankOverlays(left, top);
        drawNegativePriorityForegroundContent(left, top);
        drawSlotGridScrollbars(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        java.util.SortedSet<Integer> priorities = new java.util.TreeSet<Integer>();
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null) {
                continue;
            }
            if ("text".equalsIgnoreCase(component.type)) {
                priorities.add(Integer.valueOf(component.priority));
            }
            if ("slot".equalsIgnoreCase(component.type) && component.itemOverlay != null && component.itemOverlay.booleanValue()) {
                priorities.add(Integer.valueOf(component.priority));
            }
        }
        for (GlobalTextureLayerConfig.LayerDef layer : this.textureLayers) {
            if (layer != null && layer.foreground) {
                priorities.add(Integer.valueOf(layer.priority));
            }
        }
        if (priorities.isEmpty()) {
            drawConfiguredTexts(null);
            drawItemSlotOverlays(null);
            GlobalTextureLayerConfig.drawLayers(this.textureLayers, true, 0, 0, 0, 0);
            return;
        }
        for (Integer priority : priorities) {
            if (priority.intValue() < 0) {
                continue;
            }
            drawConfiguredTexts(priority);
            drawItemSlotOverlays(priority);
            drawConfiguredForegroundLayers(priority);
        }
    }

    private void drawNegativePriorityForegroundContent(int guiLeft, int guiTop) {
        java.util.SortedSet<Integer> priorities = new java.util.TreeSet<Integer>();
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null) {
                continue;
            }
            if ("slot".equalsIgnoreCase(component.type) && component.itemOverlay != null && component.itemOverlay.booleanValue() && component.priority < 0) {
                priorities.add(Integer.valueOf(component.priority));
            }
        }
        for (GlobalTextureLayerConfig.LayerDef layer : this.textureLayers) {
            if (layer != null && layer.foreground && layer.priority < 0) {
                priorities.add(Integer.valueOf(layer.priority));
            }
        }
        for (Integer priority : priorities) {
            drawItemSlotOverlays(priority);
            GlobalTextureLayerConfig.drawLayers(this.textureLayers, true, guiLeft, guiTop, this.backgroundOffsetX, this.backgroundOffsetY, priority);
        }
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        handleSlotGridWheel(mouseX, mouseY, wheel);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 0) {
            clickSlotGridScrollbars(mouseX, mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        releaseSlotGridScrollbars();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0) {
            dragSlotGridScrollbars(mouseX, mouseY);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        super.renderHoveredToolTip(mouseX, mouseY);
        CustomHatchRegistry.ComponentDef tankComponent = findHoveredTankComponent(mouseX, mouseY);
        if (tankComponent == null) {
            return;
        }
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useEnergy = shouldUseEnergy(tankComponent);
        boolean useGas = !useEnergy && shouldUseGas(tankComponent, fluid, gas);
        long amount = useEnergy ? getEnergyStored() : useGas ? getGasAmount() : getFluidAmount();
        long capacity = Math.max(1L, getTankCapacity(tankComponent, useGas, useEnergy));
        List<String> tooltip = new ArrayList<String>();
        List<String> configuredTips = resolveComponentTips(tankComponent);
        if (configuredTips.isEmpty()) {
            tooltip.add(useEnergy
                ? "Energy"
                : useGas
                ? gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName()
                : fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName());
            tooltip.add(useEnergy
                ? UnitFormat.amountWithUnit(amount, "FE") + " / " + UnitFormat.amountWithUnit(capacity, "FE")
                : I18n.format("tooltip.fluidhatch.tank", UnitFormat.compact(amount), UnitFormat.compact(capacity)));
        } else {
            for (String tip : configuredTips) {
                String resolved = resolveTemplate(tip, fluid, gas, useGas, useEnergy, amount, capacity);
                if (resolved != null && !resolved.isEmpty()) {
                    tooltip.add(resolved);
                }
            }
        }
        if (tooltip.isEmpty()) {
            return;
        }
        drawHoveringText(tooltip, mouseX, mouseY, this.fontRenderer);
    }

    private void drawConfiguredTexts(@Nullable Integer priorityFilter) {
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useGas = shouldUseGas(null, fluid, gas);
        long amount = useGas ? getGasAmount() : getFluidAmount();
        long capacity = getTankCapacity(null, useGas, false);
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || component.value == null || !("text".equalsIgnoreCase(component.type))) {
                continue;
            }
            if (priorityFilter != null && component.priority != priorityFilter.intValue()) {
                continue;
            }
            boolean componentUseEnergy = shouldUseEnergy(component);
            boolean componentUseGas = !componentUseEnergy && shouldUseGas(component, fluid, gas);
            long componentAmount = componentUseEnergy ? getEnergyStored() : componentUseGas ? getGasAmount() : getFluidAmount();
            long componentCapacity = getTankCapacity(component, componentUseGas, componentUseEnergy);
            String value = resolveTextValue(component.value, fluid, gas, componentUseGas, componentUseEnergy, componentAmount, componentCapacity);
            if (value == null || value.isEmpty()) {
                continue;
            }
            int color = GuiRenderUtils.parseColorARGBOrDefault(component.color, 0xFFFFFF);
            float scale = component.scale == null ? 1.0F : component.scale.floatValue();
            int alignedX = GuiRenderUtils.resolveAlignedTextX(
                componentGuiX(component),
                Math.round(this.fontRenderer.getStringWidth(value) * scale),
                component.align
            );
            if (scale != 1.0F) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, 1.0F);
                this.fontRenderer.drawStringWithShadow(value, Math.round(alignedX / scale), Math.round(componentGuiY(component) / scale), color);
                GlStateManager.popMatrix();
            } else {
                this.fontRenderer.drawStringWithShadow(value, alignedX, componentGuiY(component), color);
            }
        }
    }

    private void drawItemSlotOverlays(@Nullable Integer priorityFilter) {
        if (this.inventorySlots == null || this.inventorySlots.inventorySlots == null) {
            return;
        }
        for (int i = 36; i < this.inventorySlots.inventorySlots.size(); i++) {
            net.minecraft.inventory.Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (slot == null || !slot.getHasStack()) {
                continue;
            }
            CustomHatchRegistry.ComponentDef component = findSlotComponent(i - 36);
            if (component == null || component.itemOverlay == null || !component.itemOverlay.booleanValue()) {
                continue;
            }
            if (priorityFilter != null && component.priority != priorityFilter.intValue()) {
                continue;
            }
            ResourceLocation overlayTexture = GuiRenderUtils.parseOptionalTexture(component.itemOverlayTexture);
            if (overlayTexture == null) {
                continue;
            }
            if (slot.xPos <= -1000 || slot.yPos <= -1000) {
                continue;
            }
            this.mc.getTextureManager().bindTexture(overlayTexture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Gui.drawModalRectWithCustomSizedTexture(
                slot.xPos,
                slot.yPos,
                component.itemOverlayU,
                component.itemOverlayV,
                16,
                16,
                Math.max(1, component.itemOverlayTextureWidth),
                Math.max(1, component.itemOverlayTextureHeight)
            );
        }
    }

    private void drawConfiguredForegroundLayers(@Nullable Integer priorityFilter) {
        for (GlobalTextureLayerConfig.LayerDef layer : this.textureLayers) {
            if (layer == null || !layer.foreground) {
                continue;
            }
            if (priorityFilter != null && layer.priority != priorityFilter.intValue()) {
                continue;
            }
            java.util.List<GlobalTextureLayerConfig.LayerDef> single = java.util.Collections.singletonList(layer);
            GlobalTextureLayerConfig.drawLayers(single, true, 0, 0, 0, 0);
        }
    }

    private void drawTankOverlays(int left, int top) {
        boolean drewConfiguredTank = false;
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"tank".equalsIgnoreCase(component.type)) {
                continue;
            }
            drawTankOverlay(left, top, component);
            drewConfiguredTank = true;
        }
        if (!drewConfiguredTank && definition.tank.width > 0 && definition.tank.height > 0) {
            drawTankOverlay(left, top, null);
        }
    }

    private void drawTankOverlay(int left, int top, @Nullable CustomHatchRegistry.ComponentDef component) {
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useEnergy = shouldUseEnergy(component);
        boolean useGas = !useEnergy && shouldUseGas(component, fluid, gas);
        long amount = useEnergy ? getEnergyStored() : useGas ? getGasAmount() : getFluidAmount();
        long capacity = Math.max(1L, getTankCapacity(component, useGas, useEnergy));
        int rawX = component == null ? definition.tank.x : component.x;
        int rawY = component == null ? definition.tank.y : component.y;
        int rawWidth = component == null ? definition.tank.width : component.width;
        int rawHeight = component == null ? definition.tank.height : component.height;
        if (rawWidth <= 0 || rawHeight <= 0) {
            return;
        }
        int tankX = left + scaledX(rawX) - this.backgroundOffsetX;
        int tankY = top + scaledY(rawY) - this.backgroundOffsetY;
        int tankWidth = scaledWidth(rawWidth);
        int tankHeight = scaledHeight(rawHeight);
        if (useEnergy && amount > 0L) {
            int energyColor = GuiRenderUtils.parseColorARGBOrDefault(component == null ? null : component.color, 0xFF3DDC84);
            float red = (energyColor >> 16 & 0xFF) / 255F;
            float green = (energyColor >> 8 & 0xFF) / 255F;
            float blue = (energyColor & 0xFF) / 255F;
            float filledPercent = (float) MathHelper.clamp((double) amount / (double) capacity, 0.0D, 1.0D);
            int filled = MathHelper.ceil(filledPercent * tankHeight);
            drawSolidTankFill(tankX, tankY + tankHeight - filled, tankWidth, filled, red, green, blue, resolveTankAlpha(component));
        } else if (useGas && gas != null && amount > 0) {
            int gasColor = gas.getGas().getTint();
            float red = (gasColor >> 16 & 0xFF) / 255F;
            float green = (gasColor >> 8 & 0xFF) / 255F;
            float blue = (gasColor & 0xFF) / 255F;
            float filledPercent = (float) MathHelper.clamp((double) amount / (double) capacity, 0.0D, 1.0D);
            int filled = MathHelper.ceil(filledPercent * tankHeight);
            float alpha = resolveTankAlpha(component);
            if (usesTexturedTankRender(resolveTankRenderMode(component))) {
                TextureAtlasSprite sprite = gas.getGas().getSprite();
                if (sprite == null) {
                    sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
                }

                GlStateManager.color(red, green, blue, alpha);
                this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                drawTiledFluidSprite(tankX, tankY + tankHeight - filled, tankWidth, filled, sprite);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                drawSolidTankFill(tankX, tankY + tankHeight - filled, tankWidth, filled, red, green, blue, alpha);
            }
        } else if (fluid != null && amount > 0) {
            int fluidColor = fluid.getFluid().getColor(fluid);
            float red = (fluidColor >> 16 & 0xFF) / 255F;
            float green = (fluidColor >> 8 & 0xFF) / 255F;
            float blue = (fluidColor & 0xFF) / 255F;

            float filledPercent = (float) MathHelper.clamp((double) amount / (double) capacity, 0.0D, 1.0D);
            int filled = MathHelper.ceil(filledPercent * tankHeight);
            float alpha = resolveTankAlpha(component);
            if (usesSolidTankRender(resolveTankRenderMode(component))) {
                drawSolidTankFill(tankX, tankY + tankHeight - filled, tankWidth, filled, red, green, blue, alpha);
            } else {
                ResourceLocation still = fluid.getFluid().getStill(fluid);
                TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(still.toString());
                if (sprite == null) {
                    sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
                }

                GlStateManager.color(red, green, blue, alpha);
                this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                drawTiledFluidSprite(tankX, tankY + tankHeight - filled, tankWidth, filled, sprite);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        boolean overlay = component == null ? true : Boolean.TRUE.equals(component.overlay);
        if (overlay) {
            this.mc.getTextureManager().bindTexture(MMCE_FLUID_HATCH_TEXTURE);
            this.drawTexturedModalRect(tankX, tankY, 176, 0, tankWidth, tankHeight);
        }
    }

    private void drawTiledFluidSprite(int x, int y, int width, int height, TextureAtlasSprite sprite) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int remainingHeight = height;
        int drawY = y;
        while (remainingHeight > 0) {
            int tileHeight = Math.min(16, remainingHeight);
            int remainingWidth = width;
            int drawX = x;
            while (remainingWidth > 0) {
                int tileWidth = Math.min(16, remainingWidth);
                drawTexturedModalRect(drawX, drawY, sprite, tileWidth, tileHeight);
                drawX += tileWidth;
                remainingWidth -= tileWidth;
            }
            drawY += tileHeight;
            remainingHeight -= tileHeight;
        }
    }

    private void drawSolidTankFill(int x, int y, int width, int height, float red, float green, float blue, float alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int a = MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255);
        int r = MathHelper.clamp(Math.round(red * 255.0F), 0, 255);
        int g = MathHelper.clamp(Math.round(green * 255.0F), 0, 255);
        int b = MathHelper.clamp(Math.round(blue * 255.0F), 0, 255);
        Gui.drawRect(x, y, x + width, y + height, (a << 24) | (r << 16) | (g << 8) | b);
    }

    @Nullable
    private CustomHatchRegistry.ComponentDef findHoveredTankComponent(int mouseX, int mouseY) {
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"tank".equalsIgnoreCase(component.type)) {
                continue;
            }
            if (isMouseOverTank(mouseX, mouseY, component.x, component.y, component.width, component.height)) {
                return component;
            }
        }
        return isMouseOverTank(mouseX, mouseY, definition.tank.x, definition.tank.y, definition.tank.width, definition.tank.height) ? null : null;
    }

    private boolean isMouseOverTank(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        int left = this.guiLeft + scaledX(x) - this.backgroundOffsetX;
        int top = this.guiTop + scaledY(y) - this.backgroundOffsetY;
        int scaledWidth = scaledWidth(width);
        int scaledHeight = scaledHeight(height);
        return mouseX >= left && mouseX < left + scaledWidth
            && mouseY >= top && mouseY < top + scaledHeight;
    }

    @Nullable
    private String resolveTextValue(@Nullable String key, @Nullable FluidStack fluid, @Nullable GasStack gas, boolean useGas, boolean useEnergy, long amount, long capacity) {
        if (key == null) {
            return null;
        }
        if ("fluid.name".equalsIgnoreCase(key)) {
            return useGas
                ? gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName()
                : fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName();
        }
        if ("gas.name".equalsIgnoreCase(key)) {
            return gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName();
        }
        if ("tank.name".equalsIgnoreCase(key)) {
            return useEnergy
                ? "Energy"
                : useGas
                ? gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName()
                : fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName();
        }
        if ("tank.amount".equalsIgnoreCase(key)) {
            return Long.toString(amount);
        }
        if ("tank.amount_formatted".equalsIgnoreCase(key) || "tank.amount.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(amount);
        }
        if ("gas.amount".equalsIgnoreCase(key)) {
            return Long.toString(getGasAmount());
        }
        if ("gas.amount_formatted".equalsIgnoreCase(key) || "gas.amount.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getGasAmount());
        }
        if ("tank.capacity".equalsIgnoreCase(key)) {
            return Long.toString(capacity);
        }
        if ("tank.capacity_formatted".equalsIgnoreCase(key) || "tank.capacity.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(capacity);
        }
        if ("gas.capacity".equalsIgnoreCase(key)) {
            return Long.toString(getGasCapacity());
        }
        if ("gas.capacity_formatted".equalsIgnoreCase(key) || "gas.capacity.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getGasCapacity());
        }
        if ("tank.amount_capacity".equalsIgnoreCase(key)) {
            return amount + " / " + capacity;
        }
        if ("tank.amount_capacity_formatted".equalsIgnoreCase(key) || "tank.amount_capacity.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(amount) + " / " + UnitFormat.compact(capacity);
        }
        if ("gas.amount_capacity".equalsIgnoreCase(key)) {
            return getGasAmount() + " / " + getGasCapacity();
        }
        if ("gas.amount_capacity_formatted".equalsIgnoreCase(key) || "gas.amount_capacity.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getGasAmount()) + " / " + UnitFormat.compact(getGasCapacity());
        }
        if ("energy.name".equalsIgnoreCase(key)) {
            return "Energy";
        }
        if ("energy.amount".equalsIgnoreCase(key) || "energy.stored".equalsIgnoreCase(key)) {
            return Long.toString(getEnergyStored());
        }
        if ("energy.amount_formatted".equalsIgnoreCase(key) || "energy.amount.compact".equalsIgnoreCase(key) || "energy.stored_formatted".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getEnergyStored());
        }
        if ("energy.capacity".equalsIgnoreCase(key) || "energy.max".equalsIgnoreCase(key)) {
            return Long.toString(getEnergyCapacity());
        }
        if ("energy.capacity_formatted".equalsIgnoreCase(key) || "energy.capacity.compact".equalsIgnoreCase(key) || "energy.max_formatted".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getEnergyCapacity());
        }
        if ("energy.amount_capacity".equalsIgnoreCase(key) || "energy.stored_capacity".equalsIgnoreCase(key)) {
            return getEnergyStored() + " / " + getEnergyCapacity();
        }
        if ("energy.amount_capacity_formatted".equalsIgnoreCase(key) || "energy.amount_capacity.compact".equalsIgnoreCase(key) || "energy.stored_capacity_formatted".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getEnergyStored()) + " / " + UnitFormat.compact(getEnergyCapacity());
        }
        if ("energy.transfer".equalsIgnoreCase(key)) {
            return Long.toString(getEnergyTransfer());
        }
        if ("energy.transfer_formatted".equalsIgnoreCase(key) || "energy.transfer.compact".equalsIgnoreCase(key)) {
            return UnitFormat.compact(getEnergyTransfer());
        }
        if ("input.slot".equalsIgnoreCase(key)) {
            return "Input";
        }
        if ("output.slot".equalsIgnoreCase(key)) {
            return "Output";
        }
        return key;
    }

    private List<String> resolveComponentTips(@Nullable CustomHatchRegistry.ComponentDef component) {
        if (component != null && component.tips != null && !component.tips.isEmpty()) {
            return component.tips;
        }
        return this.definition.tips == null ? Collections.<String>emptyList() : this.definition.tips;
    }

    private String resolveTemplate(@Nullable String template,
                                   @Nullable FluidStack fluid,
                                   @Nullable GasStack gas,
                                   boolean useGas,
                                   boolean useEnergy,
                                   long amount,
                                   long capacity) {
        if (template == null) {
            return "";
        }
        String name = useEnergy
            ? "Energy"
            : useGas
            ? gas == null ? I18n.format("tooltip.fluidhatch.empty") : gas.getGas().getLocalizedName()
            : fluid == null ? I18n.format("tooltip.fluidhatch.empty") : fluid.getLocalizedName();
        return template
            .replace("{name}", name)
            .replace("{tank.name}", name)
            .replace("{amount}", Long.toString(amount))
            .replace("{tank.amount}", Long.toString(amount))
            .replace("{amount_formatted}", UnitFormat.compact(amount))
            .replace("{tank.amount_formatted}", UnitFormat.compact(amount))
            .replace("{amount.compact}", UnitFormat.compact(amount))
            .replace("{capacity}", Long.toString(capacity))
            .replace("{tank.capacity}", Long.toString(capacity))
            .replace("{capacity_formatted}", UnitFormat.compact(capacity))
            .replace("{tank.capacity_formatted}", UnitFormat.compact(capacity))
            .replace("{capacity.compact}", UnitFormat.compact(capacity))
            .replace("{amount_capacity}", amount + " / " + capacity)
            .replace("{tank.amount_capacity}", amount + " / " + capacity)
            .replace("{amount_capacity_formatted}", UnitFormat.compact(amount) + " / " + UnitFormat.compact(capacity))
            .replace("{tank.amount_capacity_formatted}", UnitFormat.compact(amount) + " / " + UnitFormat.compact(capacity))
            .replace("{energy}", Long.toString(getEnergyStored()))
            .replace("{energy.amount}", Long.toString(getEnergyStored()))
            .replace("{energy.stored}", Long.toString(getEnergyStored()))
            .replace("{energy_formatted}", UnitFormat.compact(getEnergyStored()))
            .replace("{energy.amount_formatted}", UnitFormat.compact(getEnergyStored()))
            .replace("{energy.capacity}", Long.toString(getEnergyCapacity()))
            .replace("{energy_capacity}", Long.toString(getEnergyCapacity()))
            .replace("{energy.capacity_formatted}", UnitFormat.compact(getEnergyCapacity()))
            .replace("{energy_capacity_formatted}", UnitFormat.compact(getEnergyCapacity()))
            .replace("{energy.amount_capacity}", getEnergyStored() + " / " + getEnergyCapacity())
            .replace("{energy_amount_capacity}", getEnergyStored() + " / " + getEnergyCapacity())
            .replace("{energy.amount_capacity_formatted}", UnitFormat.compact(getEnergyStored()) + " / " + UnitFormat.compact(getEnergyCapacity()))
            .replace("{energy_amount_capacity_formatted}", UnitFormat.compact(getEnergyStored()) + " / " + UnitFormat.compact(getEnergyCapacity()))
            .replace("{energy.transfer}", Long.toString(getEnergyTransfer()))
            .replace("{energy_transfer}", Long.toString(getEnergyTransfer()))
            .replace("{energy.transfer_formatted}", UnitFormat.compact(getEnergyTransfer()))
            .replace("{energy_transfer_formatted}", UnitFormat.compact(getEnergyTransfer()))
            .replace("{unit}", useEnergy ? "FE" : "mB");
    }

    private boolean shouldUseEnergy(@Nullable CustomHatchRegistry.ComponentDef component) {
        String content = component != null && component.content != null ? component.content : definition.tank.content;
        if (content != null && ("energy".equalsIgnoreCase(content) || "power".equalsIgnoreCase(content) || "fe".equalsIgnoreCase(content))) {
            return true;
        }
        return component != null
            && component.type != null
            && ("energy".equalsIgnoreCase(component.type) || "power".equalsIgnoreCase(component.type));
    }

    private boolean shouldUseGas(@Nullable CustomHatchRegistry.ComponentDef component, @Nullable FluidStack fluid, @Nullable GasStack gas) {
        String content = component != null && component.content != null ? component.content : definition.tank.content;
        content = content == null ? "fluid" : content;
        if ("gas".equalsIgnoreCase(content)) {
            return true;
        }
        return "fluid_gas".equalsIgnoreCase(content) && (fluid == null || fluid.amount <= 0) && gas != null && gas.amount > 0;
    }

    private float resolveTankAlpha(@Nullable CustomHatchRegistry.ComponentDef component) {
        Float alpha = component != null && component.alpha != null ? component.alpha : this.definition.tank.alpha;
        return alpha == null ? 1.0F : MathHelper.clamp(alpha.floatValue(), 0.0F, 1.0F);
    }

    @Nullable
    private String resolveTankRenderMode(@Nullable CustomHatchRegistry.ComponentDef component) {
        return component != null && component.renderMode != null && !component.renderMode.trim().isEmpty()
            ? component.renderMode
            : this.definition.tank.renderMode;
    }

    private boolean usesSolidTankRender(@Nullable String renderMode) {
        return "solid".equalsIgnoreCase(renderMode)
            || "flat".equalsIgnoreCase(renderMode)
            || "color".equalsIgnoreCase(renderMode);
    }

    private boolean usesTexturedTankRender(@Nullable String renderMode) {
        return "texture".equalsIgnoreCase(renderMode)
            || "textured".equalsIgnoreCase(renderMode)
            || "sprite".equalsIgnoreCase(renderMode);
    }

    private long getTankCapacity(@Nullable CustomHatchRegistry.ComponentDef component, boolean useGas, boolean useEnergy) {
        if (useEnergy) {
            return getEnergyCapacity();
        }
        String content = component != null && component.content != null ? component.content : definition.tank.content;
        content = content == null ? "fluid" : content;
        if ("gas".equalsIgnoreCase(content)) {
            return getGasCapacity();
        }
        if ("fluid_gas".equalsIgnoreCase(content)) {
            return getFluidCapacity();
        }
        return useGas ? getGasCapacity() : getFluidCapacity();
    }

    @Nullable
    private ResourceLocation resolveBackgroundTexture() {
        if (this.styleFile.background != null && this.styleFile.background.texture != null && !this.styleFile.background.texture.trim().isEmpty()) {
            ResourceLocation texture = parseCustomHatchTexture(this.styleFile.background.texture);
            if (texture != null) {
                return texture;
            }
        }
        if (MMCEGuiExtConfig.fluidHatch.backgroundTexture != null && !MMCEGuiExtConfig.fluidHatch.backgroundTexture.trim().isEmpty()) {
            return GuiRenderUtils.parseOptionalTexture(MMCEGuiExtConfig.fluidHatch.backgroundTexture);
        }
        return DEFAULT_TEXTURE;
    }

    @Nullable
    private FluidStack getFluid() {
        try {
            Method method = owner.getClass().getMethod("getFluidStack");
            Object result = method.invoke(owner);
            return result instanceof FluidStack ? ((FluidStack) result).copy() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private long getCapacity() {
        return getFluidCapacity();
    }

    private long getFluidCapacity() {
        try {
            Method method = owner.getClass().getMethod("getFluidCapacity");
            Object result = method.invoke(owner);
            return result instanceof Number ? Math.max(1L, ((Number) result).longValue()) : 1L;
        } catch (Exception ignored) {
            try {
                Method method = owner.getClass().getMethod("getCapacity");
                Object result = method.invoke(owner);
                return result instanceof Number ? Math.max(1L, ((Number) result).longValue()) : 1L;
            } catch (Exception ignoredAgain) {
                return 1L;
            }
        }
    }

    private long getGasCapacity() {
        try {
            Method method = owner.getClass().getMethod("getGasCapacity");
            Object result = method.invoke(owner);
            return result instanceof Number ? Math.max(1L, ((Number) result).longValue()) : getFluidCapacity();
        } catch (Exception ignored) {
            return getFluidCapacity();
        }
    }

    private long getFluidAmount() {
        try {
            Method method = owner.getClass().getMethod("getFluidAmountLong");
            Object result = method.invoke(owner);
            return result instanceof Number ? Math.max(0L, ((Number) result).longValue()) : 0L;
        } catch (Exception ignored) {
            FluidStack fluid = getFluid();
            return fluid == null ? 0L : Math.max(0L, (long) fluid.amount);
        }
    }

    @Optional.Method(modid = "mekanism")
    private long getGasAmount() {
        try {
            Method method = owner.getClass().getMethod("getGasAmountLong");
            Object result = method.invoke(owner);
            return result instanceof Number ? Math.max(0L, ((Number) result).longValue()) : 0L;
        } catch (Exception ignored) {
            GasStack gas = getGas();
            return gas == null ? 0L : Math.max(0L, (long) gas.amount);
        }
    }

    private long getEnergyStored() {
        return EnergyAccessHelper.getStored(owner);
    }

    private long getEnergyCapacity() {
        return EnergyAccessHelper.getCapacity(owner);
    }

    private long getEnergyTransfer() {
        try {
            Method method = owner.getClass().getMethod("getEnergyTransfer");
            Object result = method.invoke(owner);
            return result instanceof Number ? Math.max(1L, ((Number) result).longValue()) : 1L;
        } catch (Exception ignored) {
            return 1L;
        }
    }

    @Nullable
    @Optional.Method(modid = "mekanism")
    private GasStack getGas() {
        try {
            Method method = owner.getClass().getMethod("getGasStack");
            Object result = method.invoke(owner);
            return result instanceof GasStack ? ((GasStack) result).copy() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyConfiguredSlotPositions() {
        CustomHatchRegistry.ComponentDef playerInventory = findComponent("player_inventory");
        int playerInvX = 8;
        int playerInvTop = Math.max(84, this.ySize - 82);
        int playerHotbarTop = playerInvTop + 58;
        if (playerInventory != null) {
            playerInvX = componentGuiX(playerInventory);
            playerInvTop = componentGuiY(playerInventory);
            playerHotbarTop = playerInventory.hotbarY >= 0
                ? scaledY(playerInventory.hotbarY) - this.backgroundOffsetY
                : playerInvTop + 58;
        }
        for (int i = 0; i < this.inventorySlots.inventorySlots.size(); i++) {
            net.minecraft.inventory.Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (i < 27) {
                slot.xPos = playerInvX + (i % 9) * 18;
                slot.yPos = playerInvTop + ((i / 9) * 18);
            } else if (i < 36) {
                slot.xPos = playerInvX + (i - 27) * 18;
                slot.yPos = playerHotbarTop;
            } else {
                CustomHatchRegistry.ComponentDef component = findSlotComponent(i - 36);
                if (component != null) {
                    positionCustomSlot(slot, component);
                } else if (i == 36) {
                    slot.xPos = scaledX(definition.inputSlot.x) - this.backgroundOffsetX;
                    slot.yPos = scaledY(definition.inputSlot.y) - this.backgroundOffsetY;
                } else if (i == 37) {
                    slot.xPos = scaledX(definition.outputSlot.x) - this.backgroundOffsetX;
                    slot.yPos = scaledY(definition.outputSlot.y) - this.backgroundOffsetY;
                }
            }
        }
    }

    private void positionCustomSlot(net.minecraft.inventory.Slot slot, CustomHatchRegistry.ComponentDef component) {
        ScrollableSlotGrid state = getSlotGridState(component);
        if (state == null) {
            slot.xPos = componentGuiX(component);
            slot.yPos = componentGuiY(component);
            return;
        }
        if (!state.containsAbsoluteIndex(component.index)) {
            slot.xPos = componentGuiX(component);
            slot.yPos = componentGuiY(component);
            return;
        }
        ScrollableSlotGrid.SlotPosition position = state.positionForAbsoluteIndex(component.index);
        slot.xPos = position.x;
        slot.yPos = position.y;
    }

    @Nullable
    private CustomHatchRegistry.ComponentDef findSlotComponent(int guiSlotOrdinal) {
        int ordinal = 0;
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"slot".equalsIgnoreCase(component.type)) {
                continue;
            }
            if (!isRuntimeSlotRole(component.role)) {
                continue;
            }
            if (ordinal++ == guiSlotOrdinal) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private CustomHatchRegistry.ComponentDef findComponent(String type) {
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component != null && type.equalsIgnoreCase(component.type)) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private ScrollableSlotGrid getSlotGridState(@Nullable CustomHatchRegistry.ComponentDef component) {
        if (component == null || component.index < 0) {
            return null;
        }
        String key = buildSlotGridKey(component);
        return this.slotGridStates.get(key);
    }

    private void updateSlotGridScrollbars() {
        this.slotGridStates.clear();
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || !"slot".equalsIgnoreCase(component.type) || component.index < 0) {
                continue;
            }
            if (!isRuntimeSlotRole(component.role)) {
                continue;
            }
            if (component.rows <= 0 || component.columns <= 0) {
                continue;
            }
            String key = buildSlotGridKey(component);
            if (this.slotGridStates.containsKey(key)) {
                continue;
            }
            ScrollableSlotGrid.Config config = new ScrollableSlotGrid.Config();
            config.key = key;
            config.baseIndex = component.gridBaseIndex;
            config.baseX = component.gridBaseX;
            config.baseY = component.gridBaseY;
            config.rows = Math.max(1, component.rows);
            config.columns = Math.max(1, component.columns);
            config.visibleRows = component.visibleRows;
            config.visibleColumns = component.visibleColumns;
            config.spacingX = component.spacingX;
            config.spacingY = component.spacingY;
            config.slotSize = Math.max(1, component.slotSize);
            config.scrollMode = ScrollableSlotGrid.parseScrollMode(component.scrollMode);
            config.scrollAxis = component.scrollAxis;
            config.scrollbarEnabled = component.scrollbar == null || component.scrollbar.booleanValue();
            config.scrollbarX = component.scrollbarX;
            config.scrollbarY = component.scrollbarY;
            config.scrollbarLength = component.scrollbarLength;
            config.scrollbarHeight = component.scrollbarHeight;
            config.scrollbarWidth = component.scrollbarWidth;
            config.scrollbarThumbHeight = component.scrollbarThumbHeight;
            config.scrollbarThumbWidth = component.scrollbarThumbWidth;
            config.scrollbarTexture = component.scrollbarTexture == null ? null : GuiRenderUtils.parseOptionalTexture(component.scrollbarTexture);
            config.scrollbarHoverTexture = component.scrollbarHoverTexture == null ? null : GuiRenderUtils.parseOptionalTexture(component.scrollbarHoverTexture);
            config.scrollbarPressedTexture = component.scrollbarPressedTexture == null ? null : GuiRenderUtils.parseOptionalTexture(component.scrollbarPressedTexture);
            config.scrollbarDisabledTexture = component.scrollbarDisabledTexture == null ? null : GuiRenderUtils.parseOptionalTexture(component.scrollbarDisabledTexture);
            config.scrollbarTextureWidth = Math.max(1, component.scrollbarTextureWidth);
            config.scrollbarTextureHeight = Math.max(1, component.scrollbarTextureHeight);
            config.scrollbarU = component.scrollbarU;
            config.scrollbarV = component.scrollbarV;
            config.scrollbarHoverU = component.scrollbarHoverU;
            config.scrollbarHoverV = component.scrollbarHoverV;
            config.scrollbarPressedU = component.scrollbarPressedU;
            config.scrollbarPressedV = component.scrollbarPressedV;
            config.scrollbarDisabledU = component.scrollbarDisabledU;
            config.scrollbarDisabledV = component.scrollbarDisabledV;
            ScrollableSlotGrid state = ScrollableSlotGrid.create(config, slotGridMetrics());
            if (!state.canScroll()) {
                continue;
            }
            this.slotGridStates.put(key, state);
        }
    }

    private static boolean isRuntimeSlotRole(@Nullable String role) {
        return "input".equalsIgnoreCase(role) || "output".equalsIgnoreCase(role);
    }

    private void handleSlotGridWheel(int mouseX, int mouseY, int wheel) {
        for (ScrollableSlotGrid state : this.slotGridStates.values()) {
            if (!state.isMouseOverGrid(mouseX, mouseY)) {
                continue;
            }
            if (state.wheel(wheel)) {
                applyConfiguredSlotPositions();
            }
            return;
        }
    }

    private void clickSlotGridScrollbars(int mouseX, int mouseY) {
        for (ScrollableSlotGrid state : this.slotGridStates.values()) {
            if (!state.clickScrollbar(mouseX, mouseY)) {
                continue;
            }
            applyConfiguredSlotPositions();
            return;
        }
    }

    private void releaseSlotGridScrollbars() {
        for (ScrollableSlotGrid state : this.slotGridStates.values()) {
            state.releaseScrollbar();
        }
    }

    private void dragSlotGridScrollbars(int mouseX, int mouseY) {
        for (ScrollableSlotGrid state : this.slotGridStates.values()) {
            if (!state.isDraggingScrollbar()) {
                continue;
            }
            if (state.dragScrollbar(mouseX, mouseY)) {
                applyConfiguredSlotPositions();
            }
            return;
        }
    }

    private void drawSlotGridScrollbars(int mouseX, int mouseY) {
        for (ScrollableSlotGrid state : this.slotGridStates.values()) {
            state.drawScrollbar(this, this.mc, mouseX, mouseY);
        }
    }

    private ScrollableSlotGrid.Metrics slotGridMetrics() {
        return new ScrollableSlotGrid.Metrics() {
            @Override
            public int guiLeft() {
                return GuiFluidProcessorHatchCustom.this.guiLeft;
            }

            @Override
            public int guiTop() {
                return GuiFluidProcessorHatchCustom.this.guiTop;
            }

            @Override
            public int backgroundOffsetX() {
                return GuiFluidProcessorHatchCustom.this.backgroundOffsetX;
            }

            @Override
            public int backgroundOffsetY() {
                return GuiFluidProcessorHatchCustom.this.backgroundOffsetY;
            }

            @Override
            public int scaledX(int textureX) {
                return GuiFluidProcessorHatchCustom.this.scaledX(textureX);
            }

            @Override
            public int scaledY(int textureY) {
                return GuiFluidProcessorHatchCustom.this.scaledY(textureY);
            }

            @Override
            public int scaledWidth(int textureWidth) {
                return GuiFluidProcessorHatchCustom.this.scaledWidth(textureWidth);
            }

            @Override
            public int scaledHeight(int textureHeight) {
                return GuiFluidProcessorHatchCustom.this.scaledHeight(textureHeight);
            }
        };
    }

    private String buildSlotGridKey(CustomHatchRegistry.ComponentDef component) {
        return component.gridBaseIndex + "|" + component.gridBaseX + "|" + component.gridBaseY + "|" + component.rows + "|" + component.columns + "|" + component.role;
    }

    private void updateBackgroundMetrics() {
        this.backgroundOffsetX = this.styleFile.background != null && this.styleFile.background.offsetX != null
            ? this.styleFile.background.offsetX.intValue()
            : 0;
        this.backgroundOffsetY = this.styleFile.background != null && this.styleFile.background.offsetY != null
            ? this.styleFile.background.offsetY.intValue()
            : 0;
        this.backgroundTextureWidth = this.styleFile.background != null && this.styleFile.background.textureWidth != null
            ? Math.max(1, this.styleFile.background.textureWidth.intValue())
            : 176;
        this.backgroundTextureHeight = this.styleFile.background != null && this.styleFile.background.textureHeight != null
            ? Math.max(1, this.styleFile.background.textureHeight.intValue())
            : 166;
    }

    private void updateCoordinateMetrics() {
        if (this.definition.gui != null && this.definition.gui.coordinateWidth > 0) {
            this.coordinateWidth = this.definition.gui.coordinateWidth;
        } else {
            this.coordinateWidth = this.backgroundTextureWidth;
        }
        if (this.definition.gui != null && this.definition.gui.coordinateHeight > 0) {
            this.coordinateHeight = this.definition.gui.coordinateHeight;
        } else {
            this.coordinateHeight = this.backgroundTextureHeight;
        }
    }

    public List<Rectangle> getJeiExtraAreas() {
        Rectangle base = new Rectangle(this.guiLeft, this.guiTop, this.xSize, this.ySize);
        Rectangle bounds = calculateOccupiedBounds(base);
        if (bounds == null || base.contains(bounds)) {
            return Collections.emptyList();
        }
        List<Rectangle> out = new ArrayList<Rectangle>();
        int boundsRight = bounds.x + bounds.width;
        int boundsBottom = bounds.y + bounds.height;
        int baseRight = base.x + base.width;
        int baseBottom = base.y + base.height;
        if (boundsRight > baseRight) {
            addArea(out, baseRight, bounds.y, boundsRight - baseRight, bounds.height);
        }
        if (bounds.x < base.x) {
            addArea(out, bounds.x, bounds.y, base.x - bounds.x, bounds.height);
        }
        int sharedLeft = Math.max(bounds.x, base.x);
        int sharedRight = Math.min(boundsRight, baseRight);
        if (sharedRight > sharedLeft && bounds.y < base.y) {
            addArea(out, sharedLeft, bounds.y, sharedRight - sharedLeft, base.y - bounds.y);
        }
        if (sharedRight > sharedLeft && boundsBottom > baseBottom) {
            addArea(out, sharedLeft, baseBottom, sharedRight - sharedLeft, boundsBottom - baseBottom);
        }
        return out;
    }

    @Nullable
    private Rectangle calculateOccupiedBounds(Rectangle base) {
        Rectangle bounds = new Rectangle(base);
        union(bounds, this.guiLeft - this.backgroundOffsetX, this.guiTop - this.backgroundOffsetY, this.xSize + this.backgroundOffsetX, this.ySize + this.backgroundOffsetY);
        Set<String> slotGrids = new HashSet<String>();
        for (CustomHatchRegistry.ComponentDef component : this.components) {
            if (component == null || component.type == null) {
                continue;
            }
            if ("slot".equalsIgnoreCase(component.type)) {
                if (component.rows > 0 && component.columns > 0 && (component.visibleRows > 0 || component.visibleColumns > 0)) {
                    String key = buildSlotGridKey(component);
                    if (slotGrids.add(key)) {
                        unionSlotGrid(bounds, component);
                    }
                } else {
                    unionComponent(bounds, component, Math.max(16, component.slotSize), Math.max(16, component.slotSize));
                }
            } else if ("tank".equalsIgnoreCase(component.type)) {
                unionComponent(bounds, component, component.width, component.height);
            } else if ("text".equalsIgnoreCase(component.type)) {
                unionText(bounds, component);
            } else if ("player_inventory".equalsIgnoreCase(component.type)) {
                int hotbarY = component.hotbarY >= 0 ? component.hotbarY : component.y + 58;
                unionComponent(bounds, component, 162, Math.max(76, hotbarY - component.y + 18));
            }
        }
        for (GlobalTextureLayerConfig.LayerDef layer : this.textureLayers) {
            if (layer == null) {
                continue;
            }
            union(bounds, this.guiLeft - this.backgroundOffsetX + layer.x, this.guiTop - this.backgroundOffsetY + layer.y, layer.width, layer.height);
        }
        return bounds;
    }

    private void unionSlotGrid(Rectangle bounds, CustomHatchRegistry.ComponentDef component) {
        int visibleRows = component.visibleRows > 0 ? Math.min(component.visibleRows, component.rows) : component.rows;
        int visibleColumns = component.visibleColumns > 0 ? Math.min(component.visibleColumns, component.columns) : component.columns;
        int width = visibleColumns * component.slotSize + Math.max(0, visibleColumns - 1) * component.spacingX;
        int height = visibleRows * component.slotSize + Math.max(0, visibleRows - 1) * component.spacingY;
        ScrollableSlotGrid state = this.slotGridStates.get(buildSlotGridKey(component));
        if (state != null) {
            state.unionBounds(bounds);
        } else {
            union(bounds, this.guiLeft + scaledX(component.gridBaseX) - this.backgroundOffsetX, this.guiTop + scaledY(component.gridBaseY) - this.backgroundOffsetY, scaledWidth(width), scaledHeight(height));
        }
    }

    private void unionComponent(Rectangle bounds, CustomHatchRegistry.ComponentDef component, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        union(bounds, this.guiLeft + componentGuiX(component), this.guiTop + componentGuiY(component), scaledWidth(width), scaledHeight(height));
    }

    private void unionText(Rectangle bounds, CustomHatchRegistry.ComponentDef component) {
        if (component.value == null || component.value.trim().isEmpty()) {
            return;
        }
        FluidStack fluid = getFluid();
        GasStack gas = getGas();
        boolean useEnergy = shouldUseEnergy(component);
        boolean useGas = !useEnergy && shouldUseGas(component, fluid, gas);
        long amount = useEnergy ? getEnergyStored() : useGas ? getGasAmount() : getFluidAmount();
        long capacity = getTankCapacity(component, useGas, useEnergy);
        String value = resolveTextValue(component.value, fluid, gas, useGas, useEnergy, amount, capacity);
        if (value == null || value.isEmpty()) {
            return;
        }
        float scale = component.scale == null ? 1.0F : component.scale.floatValue();
        int width = Math.round(this.fontRenderer.getStringWidth(value) * scale);
        int height = Math.round(this.fontRenderer.FONT_HEIGHT * scale);
        int x = GuiRenderUtils.resolveAlignedTextX(this.guiLeft + componentGuiX(component), width, component.align);
        union(bounds, x, this.guiTop + componentGuiY(component), width, height);
    }

    private static void addArea(List<Rectangle> out, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            out.add(new Rectangle(x, y, width, height));
        }
    }

    private static void union(Rectangle bounds, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        bounds.add(new Rectangle(x, y, width, height));
    }

    private int componentGuiX(CustomHatchRegistry.ComponentDef component) {
        return scaledX(component.x) - this.backgroundOffsetX;
    }

    private int componentGuiY(CustomHatchRegistry.ComponentDef component) {
        return scaledY(component.y) - this.backgroundOffsetY;
    }

    private int scaledX(int textureX) {
        return Math.round(textureX * (this.xSize / (float) Math.max(1, this.coordinateWidth)));
    }

    private int scaledY(int textureY) {
        return Math.round(textureY * (this.ySize / (float) Math.max(1, this.coordinateHeight)));
    }

    private int scaledWidth(int textureWidth) {
        return Math.max(1, Math.round(textureWidth * (this.xSize / (float) Math.max(1, this.coordinateWidth))));
    }

    private int scaledHeight(int textureHeight) {
        return Math.max(1, Math.round(textureHeight * (this.ySize / (float) Math.max(1, this.coordinateHeight))));
    }

    @Nullable
    private static ResourceLocation parseCustomHatchTexture(String value) {
        return GuiRenderUtils.parseOptionalTexture(value);
    }
}
