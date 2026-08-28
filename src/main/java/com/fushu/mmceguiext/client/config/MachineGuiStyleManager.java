package com.fushu.mmceguiext.client.config;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class MachineGuiStyleManager {
    private static final long RELOAD_INTERVAL_MS = 5000L;
    private static final String MACHINERY_DIR = "modularmachinery/machinery";
    private static final String STYLE_DIR = "mmceguiext/styles";
    private static final Logger LOGGER = LogManager.getLogger(MMCEGuiExt.MODID);

    private static final Object LOCK = new Object();

    private static long lastLoadTime = 0L;
    private static boolean pinnedCache = false;
    private static final Map<String, ControllerStyle> MACHINE_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> FACTORY_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> MACHINE_SUBGUI_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> FACTORY_SUBGUI_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> EXTERNAL_MACHINE_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();
    private static final Map<String, ControllerStyle> EXTERNAL_FACTORY_CONTROLLER_STYLES = new HashMap<String, ControllerStyle>();

    private MachineGuiStyleManager() {
    }

    public static ControllerStyle resolveMachineController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return merge(
            merge(
                resolve(machine, MACHINE_CONTROLLER_STYLES),
                resolve(machine, EXTERNAL_MACHINE_CONTROLLER_STYLES)
            ),
            resolve(machine, MACHINE_SUBGUI_STYLES)
        );
    }

    public static ControllerStyle resolveMachineController(@Nullable ResourceLocation machineName) {
        ensureLoaded();
        return merge(
            merge(
                resolve(machineName, MACHINE_CONTROLLER_STYLES),
                resolve(machineName, EXTERNAL_MACHINE_CONTROLLER_STYLES)
            ),
            resolve(machineName, MACHINE_SUBGUI_STYLES)
        );
    }

    public static ControllerStyle resolveFactoryController(@Nullable DynamicMachine machine) {
        ensureLoaded();
        return merge(
            merge(
                resolve(machine, FACTORY_CONTROLLER_STYLES),
                resolve(machine, EXTERNAL_FACTORY_CONTROLLER_STYLES)
            ),
            resolve(machine, FACTORY_SUBGUI_STYLES)
        );
    }

    public static ControllerStyle resolveFactoryController(@Nullable ResourceLocation machineName) {
        ensureLoaded();
        return merge(
            merge(
                resolve(machineName, FACTORY_CONTROLLER_STYLES),
                resolve(machineName, EXTERNAL_FACTORY_CONTROLLER_STYLES)
            ),
            resolve(machineName, FACTORY_SUBGUI_STYLES)
        );
    }

    private static ControllerStyle resolve(@Nullable DynamicMachine machine, Map<String, ControllerStyle> source) {
        if (machine == null || machine.getRegistryName() == null) {
            return ControllerStyle.EMPTY;
        }

        return resolve(machine.getRegistryName(), source);
    }

    private static ControllerStyle resolve(@Nullable ResourceLocation machineName, Map<String, ControllerStyle> source) {
        if (machineName == null) {
            return ControllerStyle.EMPTY;
        }

        String fullKey = machineName.toString().toLowerCase(Locale.ROOT);
        ControllerStyle fullMatch = source.get(fullKey);
        if (fullMatch != null) {
            return fullMatch;
        }

        String pathKey = machineName.getPath().toLowerCase(Locale.ROOT);
        ControllerStyle pathMatch = source.get(pathKey);
        return pathMatch == null ? ControllerStyle.EMPTY : pathMatch;
    }

    private static void ensureLoaded() {
        if (pinnedCache && lastLoadTime != 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLoadTime < RELOAD_INTERVAL_MS) {
            return;
        }
        synchronized (LOCK) {
            if (now - lastLoadTime < RELOAD_INTERVAL_MS) {
                return;
            }
            reload();
            lastLoadTime = now;
        }
    }

    public static void preloadAndPinCache() {
        synchronized (LOCK) {
            reload();
            lastLoadTime = System.currentTimeMillis();
            pinnedCache = true;
        }
    }

    public static void clearPinnedCache() {
        synchronized (LOCK) {
            pinnedCache = false;
            lastLoadTime = 0L;
        }
    }

    public static void registerExternalMachineControllerStyle(ResourceLocation machineName, ControllerStyle style) {
        registerExternalStyle(EXTERNAL_MACHINE_CONTROLLER_STYLES, machineName, style, true);
    }

    public static void registerExternalFactoryControllerStyle(ResourceLocation machineName, ControllerStyle style) {
        registerExternalStyle(EXTERNAL_FACTORY_CONTROLLER_STYLES, machineName, style, true);
    }

    public static void clearExternalStyles() {
        synchronized (LOCK) {
            EXTERNAL_MACHINE_CONTROLLER_STYLES.clear();
            EXTERNAL_FACTORY_CONTROLLER_STYLES.clear();
        }
    }

    private static void registerExternalStyle(Map<String, ControllerStyle> target,
                                              ResourceLocation machineName,
                                              ControllerStyle style,
                                              boolean allowPathFallback) {
        if (machineName == null || style == null) {
            return;
        }
        synchronized (LOCK) {
            String namespacedKey = machineName.toString().toLowerCase(Locale.ROOT);
            String pathKey = machineName.getPath().toLowerCase(Locale.ROOT);
            replaceStyle(target, namespacedKey, pathKey, allowPathFallback, style);
        }
    }

    private static void reload() {
        MACHINE_CONTROLLER_STYLES.clear();
        FACTORY_CONTROLLER_STYLES.clear();
        MACHINE_SUBGUI_STYLES.clear();
        FACTORY_SUBGUI_STYLES.clear();

        Path configDir = resolveForgeConfigDir();
        if (configDir != null) {
            Path machineryDir = configDir.resolve(MACHINERY_DIR);
            if (Files.exists(machineryDir)) {
                try (Stream<Path> stream = Files.walk(machineryDir)) {
                    stream
                        .filter(Files::isRegularFile)
                        .filter(MachineGuiStyleManager::isMachineJson)
                        .forEach(MachineGuiStyleManager::loadMachineJson);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to scan MMCE GUI ext machine configs under {}: {}", machineryDir, ex.getMessage());
                }
            }

            Path styleDir = configDir.resolve(STYLE_DIR);
            if (Files.exists(styleDir)) {
                try (Stream<Path> stream = Files.walk(styleDir)) {
                    stream
                        .filter(Files::isRegularFile)
                        .filter(MachineGuiStyleManager::isMachineJson)
                        .forEach(MachineGuiStyleManager::loadStyleJson);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to scan MMCE GUI ext standalone style configs under {}: {}", styleDir, ex.getMessage());
                }
            }

            Path subGuiDir = SubGuiConfigLoader.getSubGuiRootDir();
            if (Files.exists(subGuiDir)) {
                try (Stream<Path> stream = Files.walk(subGuiDir)) {
                    stream
                        .filter(Files::isRegularFile)
                        .filter(MachineGuiStyleManager::isMachineJson)
                        .forEach(MachineGuiStyleManager::loadSubGuiJson);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to scan MMCE GUI ext subGUI configs under {}: {}", subGuiDir, ex.getMessage());
                }
            }
        }
    }

    @Nullable
    private static Path resolveForgeConfigDir() {
        try {
            if (Loader.instance() == null || Loader.instance().getConfigDir() == null) {
                return null;
            }
            return Loader.instance().getConfigDir().toPath();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean isMachineJson(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") && !name.endsWith(".var.json");
    }

    private static void loadMachineJson(Path path) {
        loadControllerStyleJson(path, "machine");
    }

    private static void loadStyleJson(Path path) {
        loadControllerStyleJson(path, "standalone style");
    }

    private static void loadControllerStyleJson(Path path, String sourceKind) {
        try {
            long fileSize = Files.size(path);
            long maxFileSize = MMCEGuiExtConfig.getMaxExtensionConfigFileBytes();
            if (!MMCEGuiExtConfig.isExtensionConfigFileSizeAllowed(fileSize)) {
                LOGGER.warn("Skipping MMCE GUI ext {} config {} because it is larger than {} bytes.",
                    sourceKind, path, maxFileSize);
                return;
            }
            String content = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            MachineGuiStyleParser.MachineFileParseResult parsed =
                MachineGuiStyleParser.parseMachineJson(path.toString(), content);

            for (String warning : parsed.warnings) {
                LOGGER.warn(warning);
            }

            if (parsed.namespacedKey == null || parsed.pathKey == null) {
                return;
            }

            if (parsed.machineNodePresent) {
                ControllerStyle machineStyle = parsed.machineStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.machineStyle;
                putStyle(MACHINE_CONTROLLER_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, machineStyle, "machine", path);
            }

            if (parsed.factoryNodePresent) {
                ControllerStyle factoryStyle = parsed.factoryStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.factoryStyle;
                putStyle(FACTORY_CONTROLLER_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, factoryStyle, "factory", path);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to read MMCE GUI ext {} config {}: {}", sourceKind, path, ex.getMessage());
        }
    }

    private static void loadSubGuiJson(Path path) {
        try {
            long fileSize = Files.size(path);
            long maxFileSize = MMCEGuiExtConfig.getMaxExtensionConfigFileBytes();
            if (!MMCEGuiExtConfig.isExtensionConfigFileSizeAllowed(fileSize)) {
                LOGGER.warn("Skipping MMCE GUI ext subGUI config {} because it is larger than {} bytes.", path, maxFileSize);
                return;
            }
            MachineGuiStyleParser.MachineFileParseResult parsed = SubGuiConfigLoader.loadSubGuiJson(path);

            for (String warning : parsed.warnings) {
                LOGGER.warn(warning);
            }

            if (parsed.namespacedKey == null || parsed.pathKey == null) {
                return;
            }

            if (parsed.machineNodePresent) {
                ControllerStyle machineStyle = parsed.machineStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.machineStyle;
                mergeStyle(MACHINE_SUBGUI_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, machineStyle);
            }

            if (parsed.factoryNodePresent) {
                ControllerStyle factoryStyle = parsed.factoryStyle == ControllerStyle.EMPTY
                    ? new ControllerStyle()
                    : parsed.factoryStyle;
                mergeStyle(FACTORY_SUBGUI_STYLES, parsed.namespacedKey, parsed.pathKey, parsed.allowPathFallback, factoryStyle);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to read MMCE GUI ext subGUI config {}: {}", path, ex.getMessage());
        }
    }

    private static ControllerStyle merge(@Nullable ControllerStyle base, @Nullable ControllerStyle overlay) {
        if (base == null || base == ControllerStyle.EMPTY || base.isEmpty()) {
            if (overlay == null || overlay == ControllerStyle.EMPTY || overlay.isEmpty()) {
                return ControllerStyle.EMPTY;
            }
            return ControllerStyle.copyOf(overlay);
        }
        if (overlay == null || overlay == ControllerStyle.EMPTY || overlay.isEmpty()) {
            return ControllerStyle.copyOf(base);
        }
        return ControllerStyle.copyOf(base).mergeFrom(overlay);
    }

    private static void putStyle(
        Map<String, ControllerStyle> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        ControllerStyle style,
        String controllerKind,
        Path sourcePath
    ) {
        ControllerStyle previousNamespaced = target.put(namespacedKey, style);
        ControllerStyle previousPath = allowPathFallback ? target.put(pathKey, style) : null;
        if (previousNamespaced != null || previousPath != null) {
            LOGGER.warn(
                "MMCE GUI ext {} style for {} was overridden by {}.",
                controllerKind,
                namespacedKey,
                sourcePath
            );
        }
    }

    private static void mergeStyle(
        Map<String, ControllerStyle> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        ControllerStyle style
    ) {
        mergeStyleForKey(target, namespacedKey, style);
        if (allowPathFallback) {
            mergeStyleForKey(target, pathKey, style);
        }
    }

    private static void replaceStyle(
        Map<String, ControllerStyle> target,
        String namespacedKey,
        String pathKey,
        boolean allowPathFallback,
        ControllerStyle style
    ) {
        replaceStyleForKey(target, namespacedKey, style);
        if (allowPathFallback) {
            replaceStyleForKey(target, pathKey, style);
        }
    }

    private static void replaceStyleForKey(Map<String, ControllerStyle> target, String key, ControllerStyle style) {
        target.put(key, ControllerStyle.copyOf(style));
    }

    private static void mergeStyleForKey(Map<String, ControllerStyle> target, String key, ControllerStyle style) {
        ControllerStyle previous = target.get(key);
        if (previous == null || previous == ControllerStyle.EMPTY || previous.isEmpty()) {
            target.put(key, ControllerStyle.copyOf(style));
            return;
        }
        target.put(key, ControllerStyle.copyOf(previous).mergeFrom(style));
    }

    public static class ControllerStyle {
        public static final ControllerStyle EMPTY = new ControllerStyle();

        @Nullable
        public String backgroundTexture;
        @Nullable
        public Integer backgroundTextureOffsetX;
        @Nullable
        public Integer backgroundTextureOffsetY;
        @Nullable
        public Boolean centerFullGui;
        @Nullable
        public Boolean hideDefaultBackground;
        @Nullable
        public Integer guiWidth;
        @Nullable
        public Integer guiHeight;
        @Nullable
        public Boolean allowOffscreenGui;
        @Nullable
        public Integer coordinateWidth;
        @Nullable
        public Integer coordinateHeight;
        @Nullable
        public Integer backgroundTextureWidth;
        @Nullable
        public Integer backgroundTextureHeight;
        @Nullable
        public Integer backgroundCorner;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Integer specialThreadBackgroundColor;
        @Nullable
        public Integer threadQueueX;
        @Nullable
        public Integer threadQueueY;
        @Nullable
        public Integer threadScrollbarX;
        @Nullable
        public Integer threadScrollbarY;
        @Nullable
        public ThreadScrollbarStyle threadScrollbar;
        @Nullable
        public Integer threadVisibleRows;
        @Nullable
        public Integer threadRowWidth;
        @Nullable
        public Integer threadRowHeight;
        @Nullable
        public Boolean disableRightExtension;
        @Nullable
        public Boolean enableSmartInterfaceEditor;
        @Nullable
        public Integer smartInterfaceEditorX;
        @Nullable
        public Integer smartInterfaceEditorY;
        @Nullable
        public Integer smartInterfaceEditorInputWidth;
        @Nullable
        public String smartInterfaceEditorVirtualKey;
        @Nullable
        public Integer smartInterfaceEditorPriority;
        @Nullable
        public Integer foregroundContentPriority;
        @Nullable
        public Float defaultCharSpacing;
        @Nullable
        public Boolean hideDefaultSmartInterfaceEditor;
        @Nullable
        public Boolean hidePlayerInventory;
        @Nullable
        public Boolean showBlueprintInfo;
        @Nullable
        public Boolean showStructureInfo;
        @Nullable
        public Boolean showStatusInfo;
        @Nullable
        public Boolean showParallelismInfo;
        @Nullable
        public Boolean showPerformanceInfo;
        @Nullable
        public String defaultPageId;
        @Nullable
        public String defaultPanelId;
        @Nullable
        public List<String> customPanels;
        @Nullable
        public List<InfoSectionStyle> infoSections;
        @Nullable
        public List<TextStyle> texts;
        @Nullable
        public List<SmartInterfaceEditorStyle> smartInterfaceEditors;
        @Nullable
        public List<ButtonStyle> buttons;
        @Nullable
        public List<TextureLayerStyle> textureLayers;
        @Nullable
        public List<ProgressBarStyle> progressBars;
        @Nullable
        public List<SliderStyle> sliders;
        @Nullable
        public List<DynamicVisualStyle> dynamicVisuals;
        @Nullable
       public List<SubGuiStyle> subGuis;
        @Nullable
        public List<SlotGroupStyle> slotGroups;
        @Nullable
        public PlayerInventoryStyle playerInventory;
        @Nullable
        public String threadQueueMode;
        @Nullable
        public Boolean threadTooltip;

       public boolean isEmpty() {
           return (backgroundTexture == null || backgroundTexture.trim().isEmpty())
                   && backgroundTextureOffsetX == null
                   && backgroundTextureOffsetY == null
                   && centerFullGui == null
                   && hideDefaultBackground == null
                   && guiWidth == null
                   && guiHeight == null
                   && allowOffscreenGui == null
                   && coordinateWidth == null
                   && coordinateHeight == null
                   && backgroundTextureWidth == null
                   && backgroundTextureHeight == null
                   && backgroundCorner == null
                   && useNineSlice == null
                   && specialThreadBackgroundColor == null
                   && threadQueueX == null
                   && threadQueueY == null
                   && threadScrollbarX == null
                   && threadScrollbarY == null
                   && threadScrollbar == null
                   && threadVisibleRows == null
                   && threadRowWidth == null
                   && threadRowHeight == null
                   && disableRightExtension == null
                   && enableSmartInterfaceEditor == null
                   && smartInterfaceEditorX == null
                   && smartInterfaceEditorY == null
                   && smartInterfaceEditorInputWidth == null
                   && (smartInterfaceEditorVirtualKey == null || smartInterfaceEditorVirtualKey.trim().isEmpty())
                   && smartInterfaceEditorPriority == null
                   && foregroundContentPriority == null
                   && defaultCharSpacing == null
                   && hideDefaultSmartInterfaceEditor == null
                   && hidePlayerInventory == null
                   && showBlueprintInfo == null
                   && showStructureInfo == null
                   && showStatusInfo == null
                   && showParallelismInfo == null
                   && showPerformanceInfo == null
                   && (defaultPageId == null || defaultPageId.trim().isEmpty())
                   && (defaultPanelId == null || defaultPanelId.trim().isEmpty())
                   && (customPanels == null || customPanels.isEmpty())
                   && (infoSections == null || infoSections.isEmpty())
                   && (texts == null || texts.isEmpty())
                   && (smartInterfaceEditors == null || smartInterfaceEditors.isEmpty())
                   && (buttons == null || buttons.isEmpty())
                   && (textureLayers == null || textureLayers.isEmpty())
                   && (progressBars == null || progressBars.isEmpty())
                   && (sliders == null || sliders.isEmpty())
                   && (dynamicVisuals == null || dynamicVisuals.isEmpty())
                   && (subGuis == null || subGuis.isEmpty())
                   && (slotGroups == null || slotGroups.isEmpty())
                   && playerInventory == null
                   && (threadQueueMode == null || threadQueueMode.trim().isEmpty())
                   && threadTooltip == null;
        }

        public static ControllerStyle copyOf(ControllerStyle source) {
            ControllerStyle copy = new ControllerStyle();
            if (source == null) {
                return copy;
            }
            copy.backgroundTexture = source.backgroundTexture;
            copy.backgroundTextureOffsetX = source.backgroundTextureOffsetX;
            copy.backgroundTextureOffsetY = source.backgroundTextureOffsetY;
            copy.centerFullGui = source.centerFullGui;
            copy.hideDefaultBackground = source.hideDefaultBackground;
            copy.guiWidth = source.guiWidth;
            copy.guiHeight = source.guiHeight;
            copy.allowOffscreenGui = source.allowOffscreenGui;
            copy.coordinateWidth = source.coordinateWidth;
            copy.coordinateHeight = source.coordinateHeight;
            copy.backgroundTextureWidth = source.backgroundTextureWidth;
            copy.backgroundTextureHeight = source.backgroundTextureHeight;
            copy.backgroundCorner = source.backgroundCorner;
            copy.useNineSlice = source.useNineSlice;
            copy.specialThreadBackgroundColor = source.specialThreadBackgroundColor;
            copy.threadQueueX = source.threadQueueX;
            copy.threadQueueY = source.threadQueueY;
            copy.threadScrollbarX = source.threadScrollbarX;
            copy.threadScrollbarY = source.threadScrollbarY;
            copy.threadScrollbar = ThreadScrollbarStyle.copyOf(source.threadScrollbar);
            copy.threadVisibleRows = source.threadVisibleRows;
            copy.threadRowWidth = source.threadRowWidth;
            copy.threadRowHeight = source.threadRowHeight;
            copy.disableRightExtension = source.disableRightExtension;
            copy.enableSmartInterfaceEditor = source.enableSmartInterfaceEditor;
            copy.smartInterfaceEditorX = source.smartInterfaceEditorX;
            copy.smartInterfaceEditorY = source.smartInterfaceEditorY;
            copy.smartInterfaceEditorInputWidth = source.smartInterfaceEditorInputWidth;
            copy.smartInterfaceEditorVirtualKey = source.smartInterfaceEditorVirtualKey;
            copy.smartInterfaceEditorPriority = source.smartInterfaceEditorPriority;
            copy.foregroundContentPriority = source.foregroundContentPriority;
            copy.defaultCharSpacing = source.defaultCharSpacing;
            copy.hideDefaultSmartInterfaceEditor = source.hideDefaultSmartInterfaceEditor;
            copy.hidePlayerInventory = source.hidePlayerInventory;
            copy.showBlueprintInfo = source.showBlueprintInfo;
            copy.showStructureInfo = source.showStructureInfo;
            copy.showStatusInfo = source.showStatusInfo;
            copy.showParallelismInfo = source.showParallelismInfo;
            copy.showPerformanceInfo = source.showPerformanceInfo;
            copy.defaultPageId = source.defaultPageId;
            copy.defaultPanelId = source.defaultPanelId;
            copy.customPanels = source.customPanels == null ? null : new ArrayList<String>(source.customPanels);
            copy.infoSections = copyInfoSectionList(source.infoSections);
            copy.texts = copyTextList(source.texts);
            copy.smartInterfaceEditors = copySmartInterfaceEditorList(source.smartInterfaceEditors);
            copy.buttons = copyButtonList(source.buttons);
            copy.textureLayers = copyTextureLayerList(source.textureLayers);
            copy.progressBars = copyProgressBarList(source.progressBars);
            copy.sliders = copySliderList(source.sliders);
            copy.dynamicVisuals = source.dynamicVisuals == null ? null : copyDynamicVisualList(source.dynamicVisuals);
            copy.subGuis = source.subGuis == null ? null : copySubGuiList(source.subGuis);
            copy.slotGroups = source.slotGroups == null ? null : copySlotGroupList(source.slotGroups);
            copy.playerInventory = PlayerInventoryStyle.copyOf(source.playerInventory);
            copy.threadQueueMode = source.threadQueueMode;
            copy.threadTooltip = source.threadTooltip;
            return copy;
        }

        public ControllerStyle mergeFrom(@Nullable ControllerStyle overlay) {
            if (overlay == null) {
                return this;
            }
            if (overlay.backgroundTexture != null) this.backgroundTexture = overlay.backgroundTexture;
            if (overlay.backgroundTextureOffsetX != null) this.backgroundTextureOffsetX = overlay.backgroundTextureOffsetX;
            if (overlay.backgroundTextureOffsetY != null) this.backgroundTextureOffsetY = overlay.backgroundTextureOffsetY;
            if (overlay.centerFullGui != null) this.centerFullGui = overlay.centerFullGui;
            if (overlay.hideDefaultBackground != null) this.hideDefaultBackground = overlay.hideDefaultBackground;
            if (overlay.guiWidth != null) this.guiWidth = overlay.guiWidth;
            if (overlay.guiHeight != null) this.guiHeight = overlay.guiHeight;
            if (overlay.allowOffscreenGui != null) this.allowOffscreenGui = overlay.allowOffscreenGui;
            if (overlay.coordinateWidth != null) this.coordinateWidth = overlay.coordinateWidth;
            if (overlay.coordinateHeight != null) this.coordinateHeight = overlay.coordinateHeight;
            if (overlay.backgroundTextureWidth != null) this.backgroundTextureWidth = overlay.backgroundTextureWidth;
            if (overlay.backgroundTextureHeight != null) this.backgroundTextureHeight = overlay.backgroundTextureHeight;
            if (overlay.backgroundCorner != null) this.backgroundCorner = overlay.backgroundCorner;
            if (overlay.useNineSlice != null) this.useNineSlice = overlay.useNineSlice;
            if (overlay.specialThreadBackgroundColor != null) this.specialThreadBackgroundColor = overlay.specialThreadBackgroundColor;
            if (overlay.threadQueueX != null) this.threadQueueX = overlay.threadQueueX;
            if (overlay.threadQueueY != null) this.threadQueueY = overlay.threadQueueY;
            if (overlay.threadScrollbarX != null) this.threadScrollbarX = overlay.threadScrollbarX;
            if (overlay.threadScrollbarY != null) this.threadScrollbarY = overlay.threadScrollbarY;
            if (overlay.threadScrollbar != null) this.threadScrollbar = ThreadScrollbarStyle.merge(this.threadScrollbar, overlay.threadScrollbar);
            if (overlay.threadVisibleRows != null) this.threadVisibleRows = overlay.threadVisibleRows;
            if (overlay.threadRowWidth != null) this.threadRowWidth = overlay.threadRowWidth;
            if (overlay.threadRowHeight != null) this.threadRowHeight = overlay.threadRowHeight;
            if (overlay.disableRightExtension != null) this.disableRightExtension = overlay.disableRightExtension;
            if (overlay.enableSmartInterfaceEditor != null) this.enableSmartInterfaceEditor = overlay.enableSmartInterfaceEditor;
            if (overlay.smartInterfaceEditorX != null) this.smartInterfaceEditorX = overlay.smartInterfaceEditorX;
            if (overlay.smartInterfaceEditorY != null) this.smartInterfaceEditorY = overlay.smartInterfaceEditorY;
            if (overlay.smartInterfaceEditorInputWidth != null) this.smartInterfaceEditorInputWidth = overlay.smartInterfaceEditorInputWidth;
            if (overlay.smartInterfaceEditorVirtualKey != null) this.smartInterfaceEditorVirtualKey = overlay.smartInterfaceEditorVirtualKey;
            if (overlay.smartInterfaceEditorPriority != null) this.smartInterfaceEditorPriority = overlay.smartInterfaceEditorPriority;
            if (overlay.foregroundContentPriority != null) this.foregroundContentPriority = overlay.foregroundContentPriority;
            if (overlay.defaultCharSpacing != null) this.defaultCharSpacing = overlay.defaultCharSpacing;
            if (overlay.hideDefaultSmartInterfaceEditor != null) this.hideDefaultSmartInterfaceEditor = overlay.hideDefaultSmartInterfaceEditor;
            if (overlay.hidePlayerInventory != null) this.hidePlayerInventory = overlay.hidePlayerInventory;
            if (overlay.showBlueprintInfo != null) this.showBlueprintInfo = overlay.showBlueprintInfo;
            if (overlay.showStructureInfo != null) this.showStructureInfo = overlay.showStructureInfo;
            if (overlay.showStatusInfo != null) this.showStatusInfo = overlay.showStatusInfo;
            if (overlay.showParallelismInfo != null) this.showParallelismInfo = overlay.showParallelismInfo;
            if (overlay.showPerformanceInfo != null) this.showPerformanceInfo = overlay.showPerformanceInfo;
            if (overlay.defaultPageId != null) this.defaultPageId = overlay.defaultPageId;
            if (overlay.defaultPanelId != null) this.defaultPanelId = overlay.defaultPanelId;
            this.customPanels = appendList(this.customPanels, overlay.customPanels);
            this.infoSections = appendInfoSectionList(this.infoSections, overlay.infoSections);
            this.texts = appendTextList(this.texts, overlay.texts);
            this.smartInterfaceEditors = appendSmartInterfaceEditorList(this.smartInterfaceEditors, overlay.smartInterfaceEditors);
            this.buttons = appendButtonList(this.buttons, overlay.buttons);
            this.textureLayers = appendTextureLayerList(this.textureLayers, overlay.textureLayers);
            this.progressBars = appendProgressBarList(this.progressBars, overlay.progressBars);
            this.sliders = appendSliderList(this.sliders, overlay.sliders);
            this.dynamicVisuals = appendDynamicVisualList(this.dynamicVisuals, overlay.dynamicVisuals);
            this.subGuis = appendSubGuiList(this.subGuis, overlay.subGuis);
            if (overlay.slotGroups != null) this.slotGroups = appendSlotGroupList(this.slotGroups, overlay.slotGroups);
            if (overlay.playerInventory != null) this.playerInventory = PlayerInventoryStyle.copyOf(overlay.playerInventory);
            if (overlay.threadQueueMode != null) this.threadQueueMode = overlay.threadQueueMode;
            if (overlay.threadTooltip != null) this.threadTooltip = overlay.threadTooltip;
            return this;
        }

        @Nullable
        private static <T> List<T> appendList(@Nullable List<T> base, @Nullable List<T> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<T> out = base == null ? new ArrayList<T>() : new ArrayList<T>(base);
            out.addAll(overlay);
            return out;
        }

        @Nullable
        private static List<InfoSectionStyle> copyInfoSectionList(@Nullable List<InfoSectionStyle> source) {
            if (source == null) {
                return null;
            }
            List<InfoSectionStyle> copy = new ArrayList<InfoSectionStyle>(source.size());
            for (InfoSectionStyle section : source) {
                copy.add(InfoSectionStyle.copyOf(section));
            }
            return copy;
        }

        @Nullable
        private static List<TextStyle> copyTextList(@Nullable List<TextStyle> source) {
            if (source == null) {
                return null;
            }
            List<TextStyle> copy = new ArrayList<TextStyle>(source.size());
            for (TextStyle text : source) {
                copy.add(TextStyle.copyOf(text));
            }
            return copy;
        }

        @Nullable
        private static List<SmartInterfaceEditorStyle> copySmartInterfaceEditorList(
            @Nullable List<SmartInterfaceEditorStyle> source
        ) {
            if (source == null) {
                return null;
            }
            List<SmartInterfaceEditorStyle> copy = new ArrayList<SmartInterfaceEditorStyle>(source.size());
            for (SmartInterfaceEditorStyle editor : source) {
                copy.add(SmartInterfaceEditorStyle.copyOf(editor));
            }
            return copy;
        }

        @Nullable
        private static List<ButtonStyle> copyButtonList(@Nullable List<ButtonStyle> source) {
            if (source == null) {
                return null;
            }
            List<ButtonStyle> copy = new ArrayList<ButtonStyle>(source.size());
            for (ButtonStyle button : source) {
                copy.add(ButtonStyle.copyOf(button));
            }
            return copy;
        }

        @Nullable
        private static List<TextureLayerStyle> copyTextureLayerList(@Nullable List<TextureLayerStyle> source) {
            if (source == null) {
                return null;
            }
            List<TextureLayerStyle> copy = new ArrayList<TextureLayerStyle>(source.size());
            for (TextureLayerStyle layer : source) {
                copy.add(TextureLayerStyle.copyOf(layer));
            }
            return copy;
        }

        @Nullable
        private static List<ProgressBarStyle> copyProgressBarList(@Nullable List<ProgressBarStyle> source) {
            if (source == null) {
                return null;
            }
            List<ProgressBarStyle> copy = new ArrayList<ProgressBarStyle>(source.size());
            for (ProgressBarStyle bar : source) {
                copy.add(ProgressBarStyle.copyOf(bar));
            }
            return copy;
        }

        @Nullable
        private static List<SliderStyle> copySliderList(@Nullable List<SliderStyle> source) {
            if (source == null) {
                return null;
            }
            List<SliderStyle> copy = new ArrayList<SliderStyle>(source.size());
            for (SliderStyle slider : source) {
                copy.add(SliderStyle.copyOf(slider));
            }
            return copy;
        }

        @Nullable
        private static List<SubGuiStyle> copySubGuiList(@Nullable List<SubGuiStyle> source) {
            if (source == null) {
                return null;
            }
            List<SubGuiStyle> copy = new ArrayList<SubGuiStyle>(source.size());
            for (SubGuiStyle subGui : source) {
                copy.add(SubGuiStyle.copyOf(subGui));
            }
            return copy;
        }

        @Nullable
        private static List<DynamicVisualStyle> copyDynamicVisualList(@Nullable List<DynamicVisualStyle> source) {
            if (source == null) {
                return null;
            }
            List<DynamicVisualStyle> copy = new ArrayList<DynamicVisualStyle>(source.size());
            for (DynamicVisualStyle visual : source) {
                copy.add(DynamicVisualStyle.copyOf(visual));
            }
            return copy;
        }

        @Nullable
        private static List<InfoSectionStyle> appendInfoSectionList(
            @Nullable List<InfoSectionStyle> base,
            @Nullable List<InfoSectionStyle> overlay
        ) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<InfoSectionStyle> out = base == null ? new ArrayList<InfoSectionStyle>() : copyInfoSectionList(base);
            for (InfoSectionStyle section : overlay) {
                out.add(InfoSectionStyle.copyOf(section));
            }
            return out;
        }

        @Nullable
        private static List<TextStyle> appendTextList(@Nullable List<TextStyle> base, @Nullable List<TextStyle> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<TextStyle> out = base == null ? new ArrayList<TextStyle>() : copyTextList(base);
            for (TextStyle text : overlay) {
                out.add(TextStyle.copyOf(text));
            }
            return out;
        }

        @Nullable
        private static List<SmartInterfaceEditorStyle> appendSmartInterfaceEditorList(
            @Nullable List<SmartInterfaceEditorStyle> base,
            @Nullable List<SmartInterfaceEditorStyle> overlay
        ) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<SmartInterfaceEditorStyle> out = base == null
                ? new ArrayList<SmartInterfaceEditorStyle>()
                : copySmartInterfaceEditorList(base);
            for (SmartInterfaceEditorStyle editor : overlay) {
                out.add(SmartInterfaceEditorStyle.copyOf(editor));
            }
            return out;
        }

        @Nullable
        private static List<ButtonStyle> appendButtonList(@Nullable List<ButtonStyle> base, @Nullable List<ButtonStyle> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<ButtonStyle> out = base == null ? new ArrayList<ButtonStyle>() : copyButtonList(base);
            for (ButtonStyle button : overlay) {
                out.add(ButtonStyle.copyOf(button));
            }
            return out;
        }

        @Nullable
        private static List<TextureLayerStyle> appendTextureLayerList(
            @Nullable List<TextureLayerStyle> base,
            @Nullable List<TextureLayerStyle> overlay
        ) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<TextureLayerStyle> out = base == null ? new ArrayList<TextureLayerStyle>() : copyTextureLayerList(base);
            for (TextureLayerStyle layer : overlay) {
                out.add(TextureLayerStyle.copyOf(layer));
            }
            return out;
        }

        @Nullable
        private static List<ProgressBarStyle> appendProgressBarList(
            @Nullable List<ProgressBarStyle> base,
            @Nullable List<ProgressBarStyle> overlay
        ) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<ProgressBarStyle> out = base == null ? new ArrayList<ProgressBarStyle>() : copyProgressBarList(base);
            for (ProgressBarStyle bar : overlay) {
                out.add(ProgressBarStyle.copyOf(bar));
            }
            return out;
        }

        @Nullable
        private static List<SliderStyle> appendSliderList(@Nullable List<SliderStyle> base, @Nullable List<SliderStyle> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<SliderStyle> out = base == null ? new ArrayList<SliderStyle>() : copySliderList(base);
            for (SliderStyle slider : overlay) {
                out.add(SliderStyle.copyOf(slider));
            }
            return out;
        }

        @Nullable
        private static List<DynamicVisualStyle> appendDynamicVisualList(
            @Nullable List<DynamicVisualStyle> base,
            @Nullable List<DynamicVisualStyle> overlay
        ) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<DynamicVisualStyle> out = base == null ? new ArrayList<DynamicVisualStyle>() : copyDynamicVisualList(base);
            for (DynamicVisualStyle visual : overlay) {
                out.add(DynamicVisualStyle.copyOf(visual));
            }
            return out;
        }

        @Nullable
        private static List<SlotGroupStyle> copySlotGroupList(@Nullable List<SlotGroupStyle> source) {
            if (source == null) {
                return null;
            }
            List<SlotGroupStyle> copy = new ArrayList<SlotGroupStyle>(source.size());
            for (SlotGroupStyle group : source) {
                copy.add(SlotGroupStyle.copyOf(group));
            }
            return copy;
        }

        @Nullable
        private static List<SlotGroupStyle> appendSlotGroupList(@Nullable List<SlotGroupStyle> base,
                                                                @Nullable List<SlotGroupStyle> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<SlotGroupStyle> out = base == null ? new ArrayList<SlotGroupStyle>() : copySlotGroupList(base);
            for (SlotGroupStyle group : overlay) {
                out.add(SlotGroupStyle.copyOf(group));
            }
            return out;
        }
      private static List<SubGuiStyle> appendSubGuiList(@Nullable List<SubGuiStyle> base, @Nullable List<SubGuiStyle> overlay) {
            if (overlay == null || overlay.isEmpty()) {
                return base;
            }
            List<SubGuiStyle> out = base == null ? new ArrayList<SubGuiStyle>() : copySubGuiList(base);
            for (SubGuiStyle subGui : overlay) {
                out.add(SubGuiStyle.copyOf(subGui));
            }
            return out;
        }
    }

    /**
     * Internal JSON style overlay. Nullable fields distinguish omitted values
     * from values that intentionally override a provider descriptor.
     */
    public static class SlotGroupStyle {
        @Nullable public String id;
        @Nullable public Integer firstSlot;
        @Nullable public Integer slotCount;
        @Nullable public int[] slotIndices;
        @Nullable public Integer x;
        @Nullable public Integer y;
        @Nullable public Integer rows;
        @Nullable public Integer columns;
        @Nullable public Integer spacingX;
        @Nullable public Integer spacingY;
        @Nullable public String shiftTarget;
        @Nullable public Boolean enabled;

        @Nullable
        public static SlotGroupStyle copyOf(@Nullable SlotGroupStyle source) {
            if (source == null) {
                return null;
            }
            SlotGroupStyle copy = new SlotGroupStyle();
            copy.id = source.id;
            copy.firstSlot = source.firstSlot;
            copy.slotCount = source.slotCount;
            copy.slotIndices = source.slotIndices == null ? null : source.slotIndices.clone();
            copy.x = source.x;
            copy.y = source.y;
            copy.rows = source.rows;
            copy.columns = source.columns;
            copy.spacingX = source.spacingX;
            copy.spacingY = source.spacingY;
            copy.shiftTarget = source.shiftTarget;
            copy.enabled = source.enabled;
            return copy;
        }
    }

    /**
     * Internal JSON style overlay for the player inventory.
     */
    public static class PlayerInventoryStyle {
        @Nullable public Integer x;
        @Nullable public Integer y;
        @Nullable public Integer hotbarX;
        @Nullable public Integer hotbarY;
        @Nullable public Integer mainStart;
        @Nullable public Integer hotbarStart;
        @Nullable public Boolean enabled;

        @Nullable
        public static PlayerInventoryStyle copyOf(@Nullable PlayerInventoryStyle source) {
            if (source == null) {
                return null;
            }
            PlayerInventoryStyle copy = new PlayerInventoryStyle();
            copy.x = source.x;
            copy.y = source.y;
            copy.hotbarX = source.hotbarX;
            copy.hotbarY = source.hotbarY;
            copy.mainStart = source.mainStart;
            copy.hotbarStart = source.hotbarStart;
            copy.enabled = source.enabled;
            return copy;
        }
    }


    public static class ThreadScrollbarStyle {
        @Nullable public Integer x;
        @Nullable public Integer y;
        @Nullable public Integer width;
        @Nullable public Integer height;
        @Nullable public String trackTexture;
        @Nullable public String thumbTexture;
        @Nullable public Integer trackColor;
        @Nullable public Integer thumbColor;
        @Nullable public Integer textureWidth;
        @Nullable public Integer textureHeight;
        @Nullable public Integer thumbTextureWidth;
        @Nullable public Integer thumbTextureHeight;
        @Nullable public Integer thumbMinHeight;
        @Nullable public Boolean visible;

        @Nullable
        public static ThreadScrollbarStyle copyOf(@Nullable ThreadScrollbarStyle source) {
            if (source == null) {
                return null;
            }
            ThreadScrollbarStyle copy = new ThreadScrollbarStyle();
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.trackTexture = source.trackTexture;
            copy.thumbTexture = source.thumbTexture;
            copy.trackColor = source.trackColor;
            copy.thumbColor = source.thumbColor;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.thumbTextureWidth = source.thumbTextureWidth;
            copy.thumbTextureHeight = source.thumbTextureHeight;
            copy.thumbMinHeight = source.thumbMinHeight;
            copy.visible = source.visible;
            return copy;
        }

        @Nullable
        public static ThreadScrollbarStyle merge(@Nullable ThreadScrollbarStyle base, @Nullable ThreadScrollbarStyle overlay) {
            if (overlay == null) {
                return base;
            }
            ThreadScrollbarStyle out = base == null ? new ThreadScrollbarStyle() : copyOf(base);
            if (overlay.x != null) out.x = overlay.x;
            if (overlay.y != null) out.y = overlay.y;
            if (overlay.width != null) out.width = overlay.width;
            if (overlay.height != null) out.height = overlay.height;
            if (overlay.trackTexture != null) out.trackTexture = overlay.trackTexture;
            if (overlay.thumbTexture != null) out.thumbTexture = overlay.thumbTexture;
            if (overlay.trackColor != null) out.trackColor = overlay.trackColor;
            if (overlay.thumbColor != null) out.thumbColor = overlay.thumbColor;
            if (overlay.textureWidth != null) out.textureWidth = overlay.textureWidth;
            if (overlay.textureHeight != null) out.textureHeight = overlay.textureHeight;
            if (overlay.thumbTextureWidth != null) out.thumbTextureWidth = overlay.thumbTextureWidth;
            if (overlay.thumbTextureHeight != null) out.thumbTextureHeight = overlay.thumbTextureHeight;
            if (overlay.thumbMinHeight != null) out.thumbMinHeight = overlay.thumbMinHeight;
            if (overlay.visible != null) out.visible = overlay.visible;
            return out;
        }
    }
    public static class InfoSectionStyle {
        @Nullable
        public String id;
        @Nullable
        public String panel;
        @Nullable
        public Boolean visible;

        public static InfoSectionStyle copyOf(@Nullable InfoSectionStyle source) {
            InfoSectionStyle copy = new InfoSectionStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.panel = source.panel;
            copy.visible = source.visible;
            return copy;
        }
    }

    public static class TextStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public String value;
        @Nullable
        public Integer color;
        @Nullable
        public Float scale;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean shadow;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public String align;
        @Nullable
        public Float charSpacing;
        @Nullable
        public TextAppearanceStyle textStyle;

        public static TextStyle copyOf(@Nullable TextStyle source) {
            TextStyle copy = new TextStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.value = source.value;
            copy.color = source.color;
            copy.scale = source.scale;
            copy.priority = source.priority;
            copy.shadow = source.shadow;
            copy.visible = source.visible;
            copy.page = source.page;
            copy.align = source.align;
            copy.charSpacing = source.charSpacing;
            copy.textStyle = TextAppearanceStyle.copyOf(source.textStyle);
            return copy;
        }
    }

    public static class SmartInterfaceEditorStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        @Nullable
        public Integer inputWidth;
        public String virtualKey;
        @Nullable
        public String title;
        @Nullable
        public Boolean showTitle;
        @Nullable
        public Boolean showInfo;
        @Nullable
        public Boolean showControls;
        @Nullable
        public Boolean inputBackground;
        @Nullable
        public Integer priority;
        @Nullable
        public String page;
        @Nullable
        public TextAppearanceStyle titleTextStyle;
        @Nullable
        public TextAppearanceStyle infoTextStyle;
        @Nullable
        public TextAppearanceStyle controlTextStyle;
        @Nullable
        public TextAppearanceStyle inputTextStyle;

        public static SmartInterfaceEditorStyle copyOf(@Nullable SmartInterfaceEditorStyle source) {
            SmartInterfaceEditorStyle copy = new SmartInterfaceEditorStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.inputWidth = source.inputWidth;
            copy.virtualKey = source.virtualKey;
            copy.title = source.title;
            copy.showTitle = source.showTitle;
            copy.showInfo = source.showInfo;
            copy.showControls = source.showControls;
            copy.inputBackground = source.inputBackground;
            copy.priority = source.priority;
            copy.page = source.page;
            copy.titleTextStyle = TextAppearanceStyle.copyOf(source.titleTextStyle);
            copy.infoTextStyle = TextAppearanceStyle.copyOf(source.infoTextStyle);
            copy.controlTextStyle = TextAppearanceStyle.copyOf(source.controlTextStyle);
            copy.inputTextStyle = TextAppearanceStyle.copyOf(source.inputTextStyle);
            return copy;
        }
    }

    public static class ButtonStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public String label;
        @Nullable
        public String action;
        @Nullable
        public String buttonId;
        @Nullable
        public String key;
        @Nullable
        public Float value;
        @Nullable
        public Float shiftValue;
        @Nullable
        public Float ctrlValue;
        @Nullable
        public Float ctrlShiftValue;
        @Nullable
        public String stringValue;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public String targetPage;
        @Nullable
        public String targetSubGui;
        @Nullable
        public String openMode;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean visible;
        @Nullable
        public List<String> hotkeys;
        @Nullable
        public Boolean consumeHotkey;
        @Nullable
        public String page;
        @Nullable
        public String texture;
        @Nullable
        public String hoverTexture;
        @Nullable
        public String pressedTexture;
        @Nullable
        public String disabledTexture;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer u;
        @Nullable
        public Integer v;
        @Nullable
        public Integer hoverU;
        @Nullable
        public Integer hoverV;
        @Nullable
        public Integer pressedU;
        @Nullable
        public Integer pressedV;
        @Nullable
        public Integer disabledU;
        @Nullable
        public Integer disabledV;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Integer corner;
        @Nullable
        public Integer textColor;
        @Nullable
        public Integer hoverTextColor;
        @Nullable
        public Integer disabledTextColor;
        @Nullable
        public Float charSpacing;
        @Nullable
        public Boolean drawLabel;
        @Nullable
        public Boolean cycleWrap;
        @Nullable
        public List<ButtonCycleStateStyle> cycleStates;
        @Nullable
        public TextAppearanceStyle textStyle;

        public static ButtonStyle copyOf(@Nullable ButtonStyle source) {
            ButtonStyle copy = new ButtonStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.label = source.label;
            copy.action = source.action;
            copy.buttonId = source.buttonId;
            copy.key = source.key;
            copy.value = source.value;
            copy.shiftValue = source.shiftValue;
            copy.ctrlValue = source.ctrlValue;
            copy.ctrlShiftValue = source.ctrlShiftValue;
            copy.stringValue = source.stringValue;
            copy.min = source.min;
            copy.max = source.max;
            copy.targetPage = source.targetPage;
            copy.targetSubGui = source.targetSubGui;
            copy.openMode = source.openMode;
            copy.priority = source.priority;
            copy.visible = source.visible;
            copy.hotkeys = source.hotkeys == null ? null : new ArrayList<String>(source.hotkeys);
            copy.consumeHotkey = source.consumeHotkey;
            copy.page = source.page;
            copy.texture = source.texture;
            copy.hoverTexture = source.hoverTexture;
            copy.pressedTexture = source.pressedTexture;
            copy.disabledTexture = source.disabledTexture;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.u = source.u;
            copy.v = source.v;
            copy.hoverU = source.hoverU;
            copy.hoverV = source.hoverV;
            copy.pressedU = source.pressedU;
            copy.pressedV = source.pressedV;
            copy.disabledU = source.disabledU;
            copy.disabledV = source.disabledV;
            copy.useNineSlice = source.useNineSlice;
            copy.corner = source.corner;
            copy.textColor = source.textColor;
            copy.hoverTextColor = source.hoverTextColor;
            copy.disabledTextColor = source.disabledTextColor;
            copy.charSpacing = source.charSpacing;
            copy.drawLabel = source.drawLabel;
            copy.cycleWrap = source.cycleWrap;
            if (source.cycleStates != null) {
                copy.cycleStates = new ArrayList<ButtonCycleStateStyle>(source.cycleStates.size());
                for (ButtonCycleStateStyle state : source.cycleStates) {
                    copy.cycleStates.add(ButtonCycleStateStyle.copyOf(state));
                }
            }
            copy.textStyle = TextAppearanceStyle.copyOf(source.textStyle);
            return copy;
        }
    }

    public static class ButtonCycleStateStyle {
        @Nullable
        public Float value;
        @Nullable
        public String label;
        @Nullable
        public String texture;
        @Nullable
        public String hoverTexture;
        @Nullable
        public String pressedTexture;
        @Nullable
        public String disabledTexture;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer u;
        @Nullable
        public Integer v;
        @Nullable
        public Integer hoverU;
        @Nullable
        public Integer hoverV;
        @Nullable
        public Integer pressedU;
        @Nullable
        public Integer pressedV;
        @Nullable
        public Integer disabledU;
        @Nullable
        public Integer disabledV;
        @Nullable
        public Integer textColor;
        @Nullable
        public Integer hoverTextColor;
        @Nullable
        public Integer disabledTextColor;
        @Nullable
        public Float charSpacing;
        @Nullable
        public Boolean drawLabel;
        @Nullable
        public TextAppearanceStyle textStyle;

        public static ButtonCycleStateStyle copyOf(@Nullable ButtonCycleStateStyle source) {
            ButtonCycleStateStyle copy = new ButtonCycleStateStyle();
            if (source == null) {
                return copy;
            }
            copy.value = source.value;
            copy.label = source.label;
            copy.texture = source.texture;
            copy.hoverTexture = source.hoverTexture;
            copy.pressedTexture = source.pressedTexture;
            copy.disabledTexture = source.disabledTexture;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.u = source.u;
            copy.v = source.v;
            copy.hoverU = source.hoverU;
            copy.hoverV = source.hoverV;
            copy.pressedU = source.pressedU;
            copy.pressedV = source.pressedV;
            copy.disabledU = source.disabledU;
            copy.disabledV = source.disabledV;
            copy.textColor = source.textColor;
            copy.hoverTextColor = source.hoverTextColor;
            copy.disabledTextColor = source.disabledTextColor;
            copy.charSpacing = source.charSpacing;
            copy.drawLabel = source.drawLabel;
            copy.textStyle = TextAppearanceStyle.copyOf(source.textStyle);
            return copy;
        }
    }

    public static class SubGuiStyle {
        @Nullable
        public String id;
        @Nullable
        public String mode;
        @Nullable
        public Boolean draggable;
        @Nullable
        public Boolean dragHandle;
        @Nullable
        public Integer dragX;
        @Nullable
        public Integer dragY;
        @Nullable
        public Integer dragWidth;
        @Nullable
        public Integer dragHeight;
        @Nullable
        public Integer x;
        @Nullable
        public Integer y;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public ControllerStyle style;

        public boolean isEmpty() {
            return (id == null || id.trim().isEmpty())
                   && (mode == null || mode.trim().isEmpty())
                   && draggable == null
                   && dragHandle == null
                   && dragX == null
                   && dragY == null
                   && dragWidth == null
                   && dragHeight == null
                   && x == null
                   && y == null
                   && width == null
                   && height == null
                   && (style == null || style == ControllerStyle.EMPTY || style.isEmpty());
        }

        public static SubGuiStyle copyOf(@Nullable SubGuiStyle source) {
            SubGuiStyle copy = new SubGuiStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.mode = source.mode;
            copy.draggable = source.draggable;
            copy.dragHandle = source.dragHandle;
            copy.dragX = source.dragX;
            copy.dragY = source.dragY;
            copy.dragWidth = source.dragWidth;
            copy.dragHeight = source.dragHeight;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.style = source.style == null ? null : ControllerStyle.copyOf(source.style);
            return copy;
        }
    }

    public static class TextureLayerStyle {
        @Nullable
        public String id;
        public String texture;
        @Nullable
        public Integer offsetX;
        @Nullable
        public Integer offsetY;
        @Nullable
        public Integer width;
        @Nullable
        public Integer height;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer corner;
        @Nullable
        public Boolean useNineSlice;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Integer priority;
        @Nullable
        public Float alpha;
        @Nullable
        public String page;

        public static TextureLayerStyle copyOf(@Nullable TextureLayerStyle source) {
            TextureLayerStyle copy = new TextureLayerStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.texture = source.texture;
            copy.offsetX = source.offsetX;
            copy.offsetY = source.offsetY;
            copy.width = source.width;
            copy.height = source.height;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.corner = source.corner;
            copy.useNineSlice = source.useNineSlice;
            copy.foreground = source.foreground;
            copy.priority = source.priority;
            copy.alpha = source.alpha;
            copy.page = source.page;
            return copy;
        }
    }

    public static class ProgressBarStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public int width;
        public int height;
        @Nullable
        public Integer backgroundColor;
        @Nullable
        public Integer fillColor;
        @Nullable
        public Integer borderColor;
        @Nullable
        public String texture;
        @Nullable
        public String backgroundTexture;
        @Nullable
        public String fillTexture;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public String direction;
        @Nullable
        public String source;
        @Nullable
        public Integer threadIndex;
        @Nullable
        public String coreThreadId;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public Boolean showText;
        @Nullable
        public Integer textColor;

        public static ProgressBarStyle copyOf(@Nullable ProgressBarStyle source) {
            ProgressBarStyle copy = new ProgressBarStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.backgroundColor = source.backgroundColor;
            copy.fillColor = source.fillColor;
            copy.borderColor = source.borderColor;
            copy.texture = source.texture;
            copy.backgroundTexture = source.backgroundTexture;
            copy.fillTexture = source.fillTexture;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.direction = source.direction;
            copy.source = source.source;
            copy.threadIndex = source.threadIndex;
            copy.coreThreadId = source.coreThreadId;
            copy.min = source.min;
            copy.max = source.max;
            copy.priority = source.priority;
            copy.foreground = source.foreground;
            copy.visible = source.visible;
            copy.page = source.page;
            copy.showText = source.showText;
            copy.textColor = source.textColor;
            return copy;
        }
    }

    public static class SliderStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public int width;
        public int height;
        @Nullable
        public String key;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float step;
        @Nullable
        public Float initialValue;
        @Nullable
        public String direction;
        @Nullable
        public Integer trackColor;
        @Nullable
        public Integer fillColor;
        @Nullable
        public Integer thumbColor;
        @Nullable
        public Integer borderColor;
        @Nullable
        public Integer thumbWidth;
        @Nullable
        public Integer thumbHeight;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public Boolean showText;
        @Nullable
        public Integer textColor;
        @Nullable
        public TextAppearanceStyle labelTextStyle;
        @Nullable
        public TextAppearanceStyle valueTextStyle;

        public static SliderStyle copyOf(@Nullable SliderStyle source) {
            SliderStyle copy = new SliderStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.key = source.key;
            copy.min = source.min;
            copy.max = source.max;
            copy.step = source.step;
            copy.initialValue = source.initialValue;
            copy.direction = source.direction;
            copy.trackColor = source.trackColor;
            copy.fillColor = source.fillColor;
            copy.thumbColor = source.thumbColor;
            copy.borderColor = source.borderColor;
            copy.thumbWidth = source.thumbWidth;
            copy.thumbHeight = source.thumbHeight;
            copy.priority = source.priority;
            copy.foreground = source.foreground;
            copy.visible = source.visible;
            copy.page = source.page;
            copy.showText = source.showText;
            copy.textColor = source.textColor;
            copy.labelTextStyle = TextAppearanceStyle.copyOf(source.labelTextStyle);
            copy.valueTextStyle = TextAppearanceStyle.copyOf(source.valueTextStyle);
            return copy;
        }
    }

    public static class DynamicVisualStyle {
        @Nullable
        public String id;
        public int x;
        public int y;
        public int width;
        public int height;
        @Nullable
        public Integer priority;
        @Nullable
        public Boolean foreground;
        @Nullable
        public Boolean visible;
        @Nullable
        public String page;
        @Nullable
        public DynamicVisualTransformStyle transform;
        @Nullable
        public DynamicVisualTransformByValueStyle transformByValue;
        @Nullable
        public DynamicVisualVisibilityByValueStyle visibleByValue;
        @Nullable
        public DynamicVisualSourceStyle source;
        @Nullable
        public DynamicVisualHistoryStyle history;
        @Nullable
        public DynamicVisualRendererStyle renderer;
        @Nullable
        public List<DynamicVisualRendererRuleStyle> rendererSwitch;
        @Nullable
        public DynamicVisualRendererByValueStyle rendererByValue;

        public static DynamicVisualStyle copyOf(@Nullable DynamicVisualStyle source) {
            DynamicVisualStyle copy = new DynamicVisualStyle();
            if (source == null) {
                return copy;
            }
            copy.id = source.id;
            copy.x = source.x;
            copy.y = source.y;
            copy.width = source.width;
            copy.height = source.height;
            copy.priority = source.priority;
            copy.foreground = source.foreground;
            copy.visible = source.visible;
            copy.page = source.page;
            copy.transform = DynamicVisualTransformStyle.copyOf(source.transform);
            copy.transformByValue = DynamicVisualTransformByValueStyle.copyOf(source.transformByValue);
            copy.visibleByValue = DynamicVisualVisibilityByValueStyle.copyOf(source.visibleByValue);
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            copy.history = DynamicVisualHistoryStyle.copyOf(source.history);
            copy.renderer = DynamicVisualRendererStyle.copyOf(source.renderer);
            if (source.rendererSwitch != null) {
                copy.rendererSwitch = new ArrayList<DynamicVisualRendererRuleStyle>(source.rendererSwitch.size());
                for (DynamicVisualRendererRuleStyle rule : source.rendererSwitch) {
                    copy.rendererSwitch.add(DynamicVisualRendererRuleStyle.copyOf(rule));
                }
            }
            copy.rendererByValue = DynamicVisualRendererByValueStyle.copyOf(source.rendererByValue);
            return copy;
        }
    }

    public static class DynamicVisualTransformStyle {
        @Nullable
        public Float offsetX;
        @Nullable
        public Float offsetY;
        @Nullable
        public Float scale;
        @Nullable
        public Float scaleX;
        @Nullable
        public Float scaleY;
        @Nullable
        public Float rotation;
        @Nullable
        public Float alpha;
        @Nullable
        public String origin;
        @Nullable
        public Float pivotX;
        @Nullable
        public Float pivotY;
        @Nullable
        public String pivotUnit;

        @Nullable
        public static DynamicVisualTransformStyle copyOf(@Nullable DynamicVisualTransformStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualTransformStyle copy = new DynamicVisualTransformStyle();
            copy.offsetX = source.offsetX;
            copy.offsetY = source.offsetY;
            copy.scale = source.scale;
            copy.scaleX = source.scaleX;
            copy.scaleY = source.scaleY;
            copy.rotation = source.rotation;
            copy.alpha = source.alpha;
            copy.origin = source.origin;
            copy.pivotX = source.pivotX;
            copy.pivotY = source.pivotY;
            copy.pivotUnit = source.pivotUnit;
            return copy;
        }
    }

    public static class DynamicVisualTransformByValueStyle {
        @Nullable
        public DynamicVisualDrivenValueStyle offsetX;
        @Nullable
        public DynamicVisualDrivenValueStyle offsetY;
        @Nullable
        public DynamicVisualDrivenValueStyle scale;
        @Nullable
        public DynamicVisualDrivenValueStyle scaleX;
        @Nullable
        public DynamicVisualDrivenValueStyle scaleY;
        @Nullable
        public DynamicVisualDrivenValueStyle rotation;
        @Nullable
        public DynamicVisualDrivenValueStyle alpha;
        @Nullable
        public DynamicVisualDrivenValueStyle pivotX;
        @Nullable
        public DynamicVisualDrivenValueStyle pivotY;

        @Nullable
        public static DynamicVisualTransformByValueStyle copyOf(@Nullable DynamicVisualTransformByValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualTransformByValueStyle copy = new DynamicVisualTransformByValueStyle();
            copy.offsetX = DynamicVisualDrivenValueStyle.copyOf(source.offsetX);
            copy.offsetY = DynamicVisualDrivenValueStyle.copyOf(source.offsetY);
            copy.scale = DynamicVisualDrivenValueStyle.copyOf(source.scale);
            copy.scaleX = DynamicVisualDrivenValueStyle.copyOf(source.scaleX);
            copy.scaleY = DynamicVisualDrivenValueStyle.copyOf(source.scaleY);
            copy.rotation = DynamicVisualDrivenValueStyle.copyOf(source.rotation);
            copy.alpha = DynamicVisualDrivenValueStyle.copyOf(source.alpha);
            copy.pivotX = DynamicVisualDrivenValueStyle.copyOf(source.pivotX);
            copy.pivotY = DynamicVisualDrivenValueStyle.copyOf(source.pivotY);
            return copy;
        }
    }

    public static class DynamicVisualDrivenValueStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public DynamicVisualSourceStyle source;

        @Nullable
        public static DynamicVisualDrivenValueStyle copyOf(@Nullable DynamicVisualDrivenValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualDrivenValueStyle copy = new DynamicVisualDrivenValueStyle();
            copy.min = source.min;
            copy.max = source.max;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            return copy;
        }
    }

    public static class DynamicVisualVisibilityByValueStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float equals;
        @Nullable
        public Boolean invert;
        @Nullable
        public DynamicVisualSourceStyle source;

        @Nullable
        public static DynamicVisualVisibilityByValueStyle copyOf(@Nullable DynamicVisualVisibilityByValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualVisibilityByValueStyle copy = new DynamicVisualVisibilityByValueStyle();
            copy.min = source.min;
            copy.max = source.max;
            copy.equals = source.equals;
            copy.invert = source.invert;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            return copy;
        }
    }

    public static class DynamicVisualSourceStyle {
        @Nullable
        public String type;
        @Nullable
        public String combine;
        @Nullable
        public Float weight;
        @Nullable
        public List<DynamicVisualSourceStyle> sources;
        @Nullable
        public String key;
        @Nullable
        public String metric;
        @Nullable
        public Float defaultValue;
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public DynamicVisualSourceStyle minSource;
        @Nullable
        public DynamicVisualSourceStyle maxSource;
        @Nullable
        public Boolean clamp;
        @Nullable
        public Boolean invert;

        @Nullable
        public static DynamicVisualSourceStyle copyOf(@Nullable DynamicVisualSourceStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualSourceStyle copy = new DynamicVisualSourceStyle();
            copy.type = source.type;
            copy.combine = source.combine;
            copy.weight = source.weight;
            if (source.sources != null) {
                copy.sources = new ArrayList<DynamicVisualSourceStyle>(source.sources.size());
                for (DynamicVisualSourceStyle child : source.sources) {
                    copy.sources.add(DynamicVisualSourceStyle.copyOf(child));
                }
            }
            copy.key = source.key;
            copy.metric = source.metric;
            copy.defaultValue = source.defaultValue;
            copy.min = source.min;
            copy.max = source.max;
            copy.minSource = DynamicVisualSourceStyle.copyOf(source.minSource);
            copy.maxSource = DynamicVisualSourceStyle.copyOf(source.maxSource);
            copy.clamp = source.clamp;
            copy.invert = source.invert;
            return copy;
        }
    }

    public static class DynamicVisualHistoryStyle {
        @Nullable
        public Boolean enabled;
        @Nullable
        public Integer samples;
        @Nullable
        public Integer intervalTicks;

        @Nullable
        public static DynamicVisualHistoryStyle copyOf(@Nullable DynamicVisualHistoryStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualHistoryStyle copy = new DynamicVisualHistoryStyle();
            copy.enabled = source.enabled;
            copy.samples = source.samples;
            copy.intervalTicks = source.intervalTicks;
            return copy;
        }
    }

    public static class DynamicVisualRendererStyle {
        @Nullable
        public String type;
        @Nullable
        public String direction;
        @Nullable
        public String backgroundTexture;
        @Nullable
        public String fillTexture;
        @Nullable
        public String fallbackTexture;
        @Nullable
        public String texture;
        @Nullable
        public Integer backgroundColor;
        @Nullable
        public Integer fillColor;
        @Nullable
        public Integer borderColor;
        @Nullable
        public Integer color;
        @Nullable
        public Integer lineColor;
        @Nullable
        public Integer gridColor;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;
        @Nullable
        public Integer frameWidth;
        @Nullable
        public Integer frameHeight;
        @Nullable
        public Integer frameCount;
        @Nullable
        public Integer ticksPerFrame;
        @Nullable
        public Integer startFrame;
        @Nullable
        public Integer u;
        @Nullable
        public Integer v;
        @Nullable
        public Integer columns;
        @Nullable
        public Boolean loop;
        @Nullable
        public Boolean reverse;
        @Nullable
        public Boolean pingPong;
        @Nullable
        public String mode;
        @Nullable
        public Float startAngle;
        @Nullable
        public Integer innerRadius;
        @Nullable
        public Integer segments;
        @Nullable
        public Integer lineWidth;
        @Nullable
        public Boolean showGrid;
        @Nullable
        public List<DynamicVisualFrameStyle> frames;

        @Nullable
        public static DynamicVisualRendererStyle copyOf(@Nullable DynamicVisualRendererStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualRendererStyle copy = new DynamicVisualRendererStyle();
            copy.type = source.type;
            copy.direction = source.direction;
            copy.backgroundTexture = source.backgroundTexture;
            copy.fillTexture = source.fillTexture;
            copy.fallbackTexture = source.fallbackTexture;
            copy.texture = source.texture;
            copy.backgroundColor = source.backgroundColor;
            copy.fillColor = source.fillColor;
            copy.borderColor = source.borderColor;
            copy.color = source.color;
            copy.lineColor = source.lineColor;
            copy.gridColor = source.gridColor;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            copy.frameWidth = source.frameWidth;
            copy.frameHeight = source.frameHeight;
            copy.frameCount = source.frameCount;
            copy.ticksPerFrame = source.ticksPerFrame;
            copy.startFrame = source.startFrame;
            copy.u = source.u;
            copy.v = source.v;
            copy.columns = source.columns;
            copy.loop = source.loop;
            copy.reverse = source.reverse;
            copy.pingPong = source.pingPong;
            copy.mode = source.mode;
            copy.startAngle = source.startAngle;
            copy.innerRadius = source.innerRadius;
            copy.segments = source.segments;
            copy.lineWidth = source.lineWidth;
            copy.showGrid = source.showGrid;
            if (source.frames != null) {
                copy.frames = new ArrayList<DynamicVisualFrameStyle>(source.frames.size());
                for (DynamicVisualFrameStyle frame : source.frames) {
                    copy.frames.add(DynamicVisualFrameStyle.copyOf(frame));
                }
            }
            return copy;
        }
    }

    public static class DynamicVisualRendererByValueStyle {
        @Nullable
        public DynamicVisualDrivenColorStyle backgroundColor;
        @Nullable
        public DynamicVisualDrivenColorStyle fillColor;
        @Nullable
        public DynamicVisualDrivenColorStyle borderColor;
        @Nullable
        public DynamicVisualDrivenColorStyle color;
        @Nullable
        public DynamicVisualDrivenColorStyle lineColor;
        @Nullable
        public DynamicVisualDrivenColorStyle gridColor;

        @Nullable
        public static DynamicVisualRendererByValueStyle copyOf(@Nullable DynamicVisualRendererByValueStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualRendererByValueStyle copy = new DynamicVisualRendererByValueStyle();
            copy.backgroundColor = DynamicVisualDrivenColorStyle.copyOf(source.backgroundColor);
            copy.fillColor = DynamicVisualDrivenColorStyle.copyOf(source.fillColor);
            copy.borderColor = DynamicVisualDrivenColorStyle.copyOf(source.borderColor);
            copy.color = DynamicVisualDrivenColorStyle.copyOf(source.color);
            copy.lineColor = DynamicVisualDrivenColorStyle.copyOf(source.lineColor);
            copy.gridColor = DynamicVisualDrivenColorStyle.copyOf(source.gridColor);
            return copy;
        }
    }

    public static class DynamicVisualRendererRuleStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float equals;
        @Nullable
        public DynamicVisualSourceStyle source;
        @Nullable
        public DynamicVisualRendererStyle renderer;

        @Nullable
        public static DynamicVisualRendererRuleStyle copyOf(@Nullable DynamicVisualRendererRuleStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualRendererRuleStyle copy = new DynamicVisualRendererRuleStyle();
            copy.min = source.min;
            copy.max = source.max;
            copy.equals = source.equals;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            copy.renderer = DynamicVisualRendererStyle.copyOf(source.renderer);
            return copy;
        }
    }

    public static class DynamicVisualDrivenColorStyle {
        @Nullable
        public Integer fromColor;
        @Nullable
        public Integer toColor;
        @Nullable
        public DynamicVisualSourceStyle source;

        @Nullable
        public static DynamicVisualDrivenColorStyle copyOf(@Nullable DynamicVisualDrivenColorStyle source) {
            if (source == null) {
                return null;
            }
            DynamicVisualDrivenColorStyle copy = new DynamicVisualDrivenColorStyle();
            copy.fromColor = source.fromColor;
            copy.toColor = source.toColor;
            copy.source = DynamicVisualSourceStyle.copyOf(source.source);
            return copy;
        }
    }

    public static class DynamicVisualFrameStyle {
        @Nullable
        public Float min;
        @Nullable
        public Float max;
        @Nullable
        public Float equals;
        @Nullable
        public String texture;
        @Nullable
        public Integer u;
        @Nullable
        public Integer v;
        @Nullable
        public Integer textureWidth;
        @Nullable
        public Integer textureHeight;

        public static DynamicVisualFrameStyle copyOf(@Nullable DynamicVisualFrameStyle source) {
            DynamicVisualFrameStyle copy = new DynamicVisualFrameStyle();
            if (source == null) {
                return copy;
            }
            copy.min = source.min;
            copy.max = source.max;
            copy.equals = source.equals;
            copy.texture = source.texture;
            copy.u = source.u;
            copy.v = source.v;
            copy.textureWidth = source.textureWidth;
            copy.textureHeight = source.textureHeight;
            return copy;
        }
    }

}
