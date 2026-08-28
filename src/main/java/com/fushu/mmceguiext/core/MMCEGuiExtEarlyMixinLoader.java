package com.fushu.mmceguiext.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class MMCEGuiExtEarlyMixinLoader implements IFMLLoadingPlugin {
    static final String CUSTOM_PARALLEL_CONTROLLER_ENABLED_KEY = "enableCustomParallelControllerTiers";
    static final String CUSTOM_PARALLEL_CONTROLLER_COUNT_KEY = "customParallelControllerTierCount";
    static final String CUSTOM_PARALLEL_CONTROLLER_DEFAULT_MAX_KEY = "customParallelControllerDefaultMaxParallelism";
    static final String CUSTOM_PARALLEL_CONTROLLER_MAX_VALUES_KEY = "customParallelControllerMaxParallelisms";
    static final int MIN_CUSTOM_PARALLEL_CONTROLLER_COUNT = 1;
    static final int MAX_CUSTOM_PARALLEL_CONTROLLER_COUNT = 16;
    static final int DEFAULT_CUSTOM_PARALLEL_CONTROLLER_COUNT = 1;
    static final int DEFAULT_CUSTOM_PARALLEL_CONTROLLER_MAX = 32;
    static final List<String> ALWAYS_REGISTERED_MIXIN_CONFIGS = Collections.unmodifiableList(
        Collections.singletonList("mixins.mmceguiext.json")
    );
    private static volatile boolean customParallelControllerTiersEnabled;
    private static volatile int customParallelControllerTierCount = DEFAULT_CUSTOM_PARALLEL_CONTROLLER_COUNT;
    private static volatile int customParallelControllerDefaultMaxParallelism = DEFAULT_CUSTOM_PARALLEL_CONTROLLER_MAX;
    private static volatile int[] customParallelControllerMaxParallelisms = new int[0];

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
        for (String config : ALWAYS_REGISTERED_MIXIN_CONFIGS) {
            Mixins.addConfiguration(config);
        }

        if (isCustomParallelControllerTiersEnabled(data)) {
            Mixins.addConfiguration("mixins.mmceguiext.custom_parallel.json");
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static boolean areCustomParallelControllerTiersEnabled() {
        return customParallelControllerTiersEnabled && customParallelControllerTierCount > 0;
    }

    public static int getCustomParallelControllerTierCount() {
        return customParallelControllerTierCount;
    }

    public static int getCustomParallelControllerMaxParallelism(final int tierIndex) {
        if (tierIndex >= 0 && tierIndex < customParallelControllerMaxParallelisms.length) {
            return customParallelControllerMaxParallelisms[tierIndex];
        }
        return customParallelControllerDefaultMaxParallelism;
    }

    static boolean isCustomParallelControllerTiersEnabled(final Map<String, Object> data) {
        Object location = data == null ? null : data.get("mcLocation");
        File minecraftDir = location instanceof File ? (File) location : new File(".");
        File configFile = new File(new File(minecraftDir, "config/mmceguiext"), "client.cfg");
        if (!configFile.isFile()) {
            resetCustomParallelControllerSettings();
            return false;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            boolean enabled = readCustomParallelControllerSettings(reader);
            if (enabled && isWhimCraftParallelExtensionEnabled(minecraftDir)) {
                customParallelControllerTiersEnabled = false;
                return false;
            }
            return enabled;
        } catch (IOException ignored) {
            resetCustomParallelControllerSettings();
            return false;
        }
    }

    static boolean isCustomParallelControllerTiersEnabled(final Iterable<String> lines) {
        if (lines == null) {
            resetCustomParallelControllerSettings();
            return false;
        }
        return readCustomParallelControllerSettings(lines);
    }

    private static void resetCustomParallelControllerSettings() {
        customParallelControllerTiersEnabled = false;
        customParallelControllerTierCount = DEFAULT_CUSTOM_PARALLEL_CONTROLLER_COUNT;
        customParallelControllerDefaultMaxParallelism = DEFAULT_CUSTOM_PARALLEL_CONTROLLER_MAX;
        customParallelControllerMaxParallelisms = new int[0];
    }

    private static boolean isWhimCraftParallelExtensionEnabled(final File minecraftDir) {
        File modsDirectory = new File(minecraftDir, "mods");
        boolean hasWhimCraftJar = false;
        File[] modFiles = modsDirectory.listFiles();
        if (modFiles != null) {
            for (File file : modFiles) {
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).contains("whimcraft")) {
                    hasWhimCraftJar = true;
                    break;
                }
            }
        }
        if (!hasWhimCraftJar
            && !isClassPresent("com.xinyihl.whimcraft.common.mixins.mmce.ParallelControllerDataMixin")) {
            return false;
        }
        return readWhimCraftParallelControllerSetting(minecraftDir);
    }

    private static boolean readWhimCraftParallelControllerSetting(final File minecraftDir) {
        File configDirectory = new File(minecraftDir, "config");
        File configFile = new File(configDirectory, "WhimCraft.cfg");
        if (!configFile.isFile()) {
            configFile = new File(configDirectory, "whimcraft.cfg");
        }
        if (!configFile.isFile()) {
            return true;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return readWhimCraftParallelControllerSetting(lines);
        } catch (IOException ignored) {
            return true;
        }
    }

    private static boolean readWhimCraftParallelControllerSetting(final Iterable<String> lines) {
        if (lines == null) {
            return true;
        }
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            int separator = line.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String rawKey = line.substring(0, separator).trim();
            int typeSeparator = rawKey.indexOf(':');
            String key = typeSeparator >= 0 ? rawKey.substring(typeSeparator + 1).trim() : rawKey;
            if (!"otherParallelController".equalsIgnoreCase(key)) {
                continue;
            }
            try {
                return Integer.parseInt(line.substring(separator + 1).trim()) > 0;
            } catch (NumberFormatException ignored) {
                return true;
            }
        }
        return true;
    }

    private static boolean readCustomParallelControllerSettings(final BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return readCustomParallelControllerSettings(lines);
    }

    private static boolean readCustomParallelControllerSettings(final Iterable<String> lines) {
        Boolean enabled = null;
        Integer count = null;
        Integer defaultMax = null;
        List<Integer> maxValues = new ArrayList<Integer>();
        boolean readingMaxValues = false;
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (readingMaxValues) {
                int end = line.indexOf('>');
                if (end >= 0) {
                    addParallelismValues(maxValues, line.substring(0, end));
                    readingMaxValues = false;
                } else {
                    addParallelismValues(maxValues, line);
                }
                continue;
            }

            int listStart = line.indexOf('<');
            if (listStart >= 0) {
                String rawKey = line.substring(0, listStart).trim();
                int typeSeparator = rawKey.indexOf(':');
                String key = typeSeparator >= 0 ? rawKey.substring(typeSeparator + 1).trim() : rawKey;
                if (CUSTOM_PARALLEL_CONTROLLER_MAX_VALUES_KEY.equalsIgnoreCase(key)) {
                    int end = line.indexOf('>', listStart + 1);
                    if (end >= 0) {
                        addParallelismValues(maxValues, line.substring(listStart + 1, end));
                    } else {
                        addParallelismValues(maxValues, line.substring(listStart + 1));
                        readingMaxValues = true;
                    }
                    continue;
                }
            }

            int separator = line.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String rawKey = line.substring(0, separator).trim();
            int typeSeparator = rawKey.indexOf(':');
            String key = typeSeparator >= 0 ? rawKey.substring(typeSeparator + 1).trim() : rawKey;
            String value = line.substring(separator + 1).trim();
            if (CUSTOM_PARALLEL_CONTROLLER_ENABLED_KEY.equalsIgnoreCase(key)) {
                enabled = Boolean.valueOf("true".equalsIgnoreCase(value));
            } else if (CUSTOM_PARALLEL_CONTROLLER_COUNT_KEY.equalsIgnoreCase(key)) {
                count = parsePositiveInt(value, DEFAULT_CUSTOM_PARALLEL_CONTROLLER_COUNT);
            } else if (CUSTOM_PARALLEL_CONTROLLER_DEFAULT_MAX_KEY.equalsIgnoreCase(key)) {
                defaultMax = parsePositiveInt(value, DEFAULT_CUSTOM_PARALLEL_CONTROLLER_MAX);
            } else if (CUSTOM_PARALLEL_CONTROLLER_MAX_VALUES_KEY.equalsIgnoreCase(key)) {
                addParallelismValues(maxValues, value);
            }
        }

        customParallelControllerTiersEnabled = Boolean.TRUE.equals(enabled);
        customParallelControllerTierCount = clamp(
            count == null ? DEFAULT_CUSTOM_PARALLEL_CONTROLLER_COUNT : count.intValue(),
            MIN_CUSTOM_PARALLEL_CONTROLLER_COUNT,
            MAX_CUSTOM_PARALLEL_CONTROLLER_COUNT
        );
        customParallelControllerDefaultMaxParallelism = defaultMax == null
            ? DEFAULT_CUSTOM_PARALLEL_CONTROLLER_MAX
            : Math.max(1, defaultMax.intValue());
        customParallelControllerMaxParallelisms = toIntArray(maxValues);
        return areCustomParallelControllerTiersEnabled();
    }

    private static void addParallelismValues(final List<Integer> values, final String rawValue) {
        if (rawValue == null) {
            return;
        }
        String normalized = rawValue
            .replace('[', ' ')
            .replace(']', ' ')
            .replace('"', ' ')
            .replace('\'', ' ');
        String[] tokens = normalized.split("[,;\\s]+");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            Integer parsed = parsePositiveIntOrNull(token);
            if (parsed != null) {
                values.add(parsed);
            }
        }
    }

    private static Integer parsePositiveIntOrNull(final String value) {
        try {
            return Integer.valueOf(Math.max(1, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int[] toIntArray(final List<Integer> values) {
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).intValue();
        }
        return result;
    }

    private static int parsePositiveInt(final String value, final int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isClassPresent(final String className) {
        try {
            Class.forName(className, false, MMCEGuiExtEarlyMixinLoader.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static String stripComment(final String line) {
        if (line == null) {
            return "";
        }
        int comment = line.indexOf('#');
        return comment >= 0 ? line.substring(0, comment) : line;
    }

}
