package com.wdlpiaoyi.wmisc.feature;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.Compat;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块登记表 + 启用判定。
 *
 * <p>启用 = 配置开关打开 且 前置模组齐全。配置在构造期还没加载，读值失败时退回到模块默认值，
 * 避免在配置加载前误判。</p>
 */
public final class FeatureRegistry {
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean activated;

    private FeatureRegistry() {
    }

    private static final class Entry {
        final WmiscFeature feature;
        ForgeConfigSpec.BooleanValue toggle;
        boolean active;

        Entry(WmiscFeature feature) {
            this.feature = feature;
        }
    }

    public static void register(WmiscFeature feature) {
        String id = feature.id();
        if (ENTRIES.putIfAbsent(id, new Entry(feature)) != null) {
            throw new IllegalStateException("重复注册的功能模块 id: " + id);
        }
    }

    public static Collection<WmiscFeature> all() {
        List<WmiscFeature> features = new ArrayList<>(ENTRIES.size());
        for (Entry entry : ENTRIES.values()) {
            features.add(entry.feature);
        }
        return features;
    }

    public static void bindToggle(String id, ForgeConfigSpec.BooleanValue toggle) {
        Entry entry = ENTRIES.get(id);
        if (entry != null) {
            entry.toggle = toggle;
        }
    }

    /** 只考虑配置开关，不考虑前置模组。 */
    public static boolean configEnabled(WmiscFeature feature) {
        Entry entry = ENTRIES.get(feature.id());
        if (entry == null || entry.toggle == null) {
            return feature.defaultEnabled();
        }
        try {
            return entry.toggle.get();
        } catch (IllegalStateException configNotLoadedYet) {
            return feature.defaultEnabled();
        }
    }

    public static List<String> missingMods(WmiscFeature feature) {
        List<String> missing = new ArrayList<>();
        for (String modId : feature.requiredMods()) {
            if (!Compat.isModLoaded(modId)) {
                missing.add(modId);
            }
        }
        return missing;
    }

    public static boolean isActive(WmiscFeature feature) {
        Entry entry = ENTRIES.get(feature.id());
        if (activated && entry != null) {
            return entry.active;
        }
        return configEnabled(feature) && missingMods(feature).isEmpty();
    }

    /** 在 common setup 阶段统一判定并初始化，避免每个模块各自判断。 */
    public static void activateAll() {
        for (Entry entry : ENTRIES.values()) {
            entry.active = configEnabled(entry.feature) && missingMods(entry.feature).isEmpty();
            if (!entry.active) {
                continue;
            }
            try {
                entry.feature.onCommonSetup();
            } catch (Throwable t) {
                entry.active = false;
                Wmisc.LOG.error("[w_misc] 模块 {} 初始化失败，已停用", entry.feature.id(), t);
            }
        }
        activated = true;
    }

    public static void clientSetupAll() {
        for (Entry entry : ENTRIES.values()) {
            if (!entry.active) {
                continue;
            }
            try {
                entry.feature.onClientSetup();
            } catch (Throwable t) {
                entry.active = false;
                Wmisc.LOG.error("[w_misc] 模块 {} 客户端初始化失败，已停用", entry.feature.id(), t);
            }
        }
    }

    /** 启动日志用的一览：每个模块一行，写明启用 / 停用 / 缺前置 / 实现待补。 */
    public static void logSummary() {
        StringBuilder log = new StringBuilder("[w_misc] 功能模块：");
        for (Entry entry : ENTRIES.values()) {
            log.append("\n  ").append(entry.feature.id());
            List<String> missing = missingMods(entry.feature);
            if (!configEnabled(entry.feature)) {
                log.append("  [配置停用]");
            } else if (!missing.isEmpty()) {
                log.append("  [缺少前置 ").append(String.join(", ", missing)).append("]");
            } else {
                log.append("  [启用]");
                if (!entry.feature.implemented()) {
                    log.append("（实现待补）");
                }
                if (!entry.feature.summary().isEmpty()) {
                    log.append(" — ").append(entry.feature.summary());
                }
            }
        }
        Wmisc.LOG.info(log.toString());
    }
}
