package com.fushu.mmceguiext.common.util;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VirtualSmartInterfaceStore {
    private static final String KEY_VIRTUAL_SMART_INTERFACES = "virtualSmartInterfaces";

    private final Map<String, SmartInterfaceData> dataByType = new LinkedHashMap<String, SmartInterfaceData>();

    public VirtualSmartInterfaceStore() {
    }

    public synchronized void sync(@Nullable DynamicMachine machine, @Nullable BlockPos pos) {
        List<SmartInterfaceType> declaredTypes = collectDeclaredTypes(machine);
        if (machine == null || pos == null || declaredTypes.isEmpty()) {
            this.dataByType.clear();
            return;
        }

        ResourceLocation parent = machine.getRegistryName();
        if (parent == null) {
            this.dataByType.clear();
            return;
        }

        Map<String, SmartInterfaceData> next = new LinkedHashMap<String, SmartInterfaceData>();
        for (SmartInterfaceType type : declaredTypes) {
            String normalizedType = normalizeType(type == null ? null : type.getType());
            if (normalizedType == null) {
                continue;
            }

            SmartInterfaceData existing = this.dataByType.get(normalizedType);
            float value = existing != null && Float.isFinite(existing.getValue())
                ? existing.getValue()
                : Float.isFinite(type.getDefaultValue()) ? type.getDefaultValue() : 0.0F;
            next.put(normalizedType, new SmartInterfaceData(pos, parent, normalizedType, value));
        }

        this.dataByType.clear();
        this.dataByType.putAll(next);
    }

    @Nullable
    public synchronized SmartInterfaceData get(@Nullable DynamicMachine machine, @Nullable BlockPos pos, @Nullable String type) {
        sync(machine, pos);
        String normalizedType = normalizeType(type);
        if (normalizedType == null) {
            return null;
        }

        SmartInterfaceData existing = this.dataByType.get(normalizedType);
        return existing == null ? null : copyOf(existing);
    }

    @Nullable
    public synchronized SmartInterfaceData set(@Nullable DynamicMachine machine, @Nullable BlockPos pos, @Nullable String type, float value) {
        String normalizedType = normalizeType(type);
        if (machine == null || pos == null || normalizedType == null || !Float.isFinite(value)) {
            return null;
        }
        sync(machine, pos);
        if (!machine.hasSmartInterfaceType(normalizedType)) {
            return null;
        }

        ResourceLocation parent = machine.getRegistryName();
        if (parent == null) {
            return null;
        }

        SmartInterfaceData data = new SmartInterfaceData(pos, parent, normalizedType, value);
        this.dataByType.put(normalizedType, data);
        return copyOf(data);
    }

    public synchronized SmartInterfaceData[] list(@Nullable DynamicMachine machine, @Nullable BlockPos pos) {
        sync(machine, pos);
        List<SmartInterfaceType> declaredTypes = collectDeclaredTypes(machine);
        List<SmartInterfaceData> result = new ArrayList<SmartInterfaceData>(Math.max(this.dataByType.size(), declaredTypes.size()));
        List<String> declaredKeys = new ArrayList<String>(declaredTypes.size());

        for (SmartInterfaceType declared : declaredTypes) {
            String normalizedType = normalizeType(declared == null ? null : declared.getType());
            if (normalizedType == null) {
                continue;
            }
            declaredKeys.add(normalizedType);
            SmartInterfaceData existing = this.dataByType.get(normalizedType);
            if (existing != null) {
                result.add(copyOf(existing));
            }
        }

        List<String> leftovers = new ArrayList<String>();
        for (String storedType : this.dataByType.keySet()) {
            if (!declaredKeys.contains(storedType)) {
                leftovers.add(storedType);
            }
        }
        Collections.sort(leftovers);
        for (String leftover : leftovers) {
            SmartInterfaceData existing = this.dataByType.get(leftover);
            if (existing != null) {
                result.add(copyOf(existing));
            }
        }

        return result.toArray(new SmartInterfaceData[result.size()]);
    }

    public synchronized void writeTo(@Nullable NBTTagCompound compound) {
        if (compound == null) {
            return;
        }

        compound.removeTag(KEY_VIRTUAL_SMART_INTERFACES);
        NBTTagList list = new NBTTagList();
        for (SmartInterfaceData data : this.dataByType.values()) {
            if (data == null) {
                continue;
            }
            NBTTagCompound serialized = data.serialize();
            if (serialized != null) {
                list.appendTag(serialized);
            }
        }
        if (list.tagCount() > 0) {
            compound.setTag(KEY_VIRTUAL_SMART_INTERFACES, list);
        }
    }

    public synchronized void readFrom(@Nullable NBTTagCompound compound) {
        this.dataByType.clear();
        if (compound == null || !compound.hasKey(KEY_VIRTUAL_SMART_INTERFACES, Constants.NBT.TAG_LIST)) {
            return;
        }

        NBTTagList list = compound.getTagList(KEY_VIRTUAL_SMART_INTERFACES, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                SmartInterfaceData data = SmartInterfaceData.deserialize(list.getCompoundTagAt(i));
                if (data == null) {
                    continue;
                }
                String normalizedType = normalizeType(data.getType());
                ResourceLocation parent = data.getParent();
                if (normalizedType == null || parent == null || !Float.isFinite(data.getValue())) {
                    continue;
                }
                this.dataByType.put(normalizedType, new SmartInterfaceData(data.getPos(), parent, normalizedType, data.getValue()));
            } catch (RuntimeException ignored) {
                // Ignore malformed entries and keep the rest of the store intact.
            }
        }
    }

    private static List<SmartInterfaceType> collectDeclaredTypes(@Nullable DynamicMachine machine) {
        if (machine == null || machine.getSmartInterfaceTypes().isEmpty()) {
            return Collections.emptyList();
        }

        List<SmartInterfaceType> declaredTypes = new ArrayList<SmartInterfaceType>(machine.getSmartInterfaceTypes().values());
        Collections.sort(declaredTypes);
        return declaredTypes;
    }

    @Nullable
    private static String normalizeType(@Nullable String type) {
        if (type == null) {
            return null;
        }
        String normalized = type.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static SmartInterfaceData copyOf(SmartInterfaceData data) {
        return new SmartInterfaceData(data.getPos(), data.getParent(), data.getType(), data.getValue());
    }
}
