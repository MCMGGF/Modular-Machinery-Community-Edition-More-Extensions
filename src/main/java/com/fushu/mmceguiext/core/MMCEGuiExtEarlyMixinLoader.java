package com.fushu.mmceguiext.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MMCEGuiExtEarlyMixinLoader implements IFMLLoadingPlugin {
    static final String LONG_FLUID_GAS_REQUIREMENTS_KEY = "enableLongFluidGasRequirements";
    static final List<String> ALWAYS_REGISTERED_MIXIN_CONFIGS = Collections.unmodifiableList(
        Collections.singletonList("mixins.mmceguiext.json")
    );

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

        if (!isLongFluidGasRequirementsEnabled(data)) {
            return;
        }

        Mixins.addConfiguration("mixins.mmceguiext.long_fluid.json");
        if (isClassPresent("mekanism.api.gas.GasStack")) {
            Mixins.addConfiguration("mixins.mmceguiext.mekanism.json");
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    private static boolean isClassPresent(final String className) {
        try {
            Class.forName(className, false, MMCEGuiExtEarlyMixinLoader.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isLongFluidGasRequirementsEnabled(final Map<String, Object> data) {
        Object location = data == null ? null : data.get("mcLocation");
        File minecraftDir = location instanceof File ? (File) location : new File(".");
        File configFile = new File(new File(minecraftDir, "config/mmceguiext"), "client.cfg");
        if (!configFile.isFile()) {
            return false;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            return isLongFluidGasRequirementsEnabled(reader);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isLongFluidGasRequirementsEnabled(final BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            Boolean parsed = parseLongFluidGasRequirementLine(line);
            if (parsed != null) {
                return parsed;
            }
        }
        return false;
    }

    static boolean isLongFluidGasRequirementsEnabled(final Iterable<String> lines) {
        if (lines == null) {
            return false;
        }

        for (String rawLine : lines) {
            Boolean parsed = parseLongFluidGasRequirementLine(rawLine);
            if (parsed != null) {
                return parsed;
            }
        }

        return false;
    }

    @Nullable
    private static Boolean parseLongFluidGasRequirementLine(final String rawLine) {
        String line = stripComment(rawLine).trim();
        if (line.isEmpty()) {
            return null;
        }

        int separator = line.indexOf('=');
        if (separator < 0) {
            return null;
        }

        String rawKey = line.substring(0, separator).trim();
        int typeSeparator = rawKey.indexOf(':');
        String key = typeSeparator >= 0 ? rawKey.substring(typeSeparator + 1).trim() : rawKey;
        if (!LONG_FLUID_GAS_REQUIREMENTS_KEY.equalsIgnoreCase(key)) {
            return null;
        }

        String value = line.substring(separator + 1).trim();
        return "true".equalsIgnoreCase(value);
    }

    private static String stripComment(final String line) {
        if (line == null) {
            return "";
        }
        int comment = line.indexOf('#');
        return comment >= 0 ? line.substring(0, comment) : line;
    }
}
