package com.wdlpiaoyi.wmisc.compat.chloride;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 Mixin 运行时里摘掉 chloride 的 {@code jei_rei_emi.JeiOverlayMixin}。
 *
 * <p>纯反射实现：本模组不依赖 Mixin 的编译期 API 细节，也不碰 chloride / JEI 的 jar。
 * 三重摘除互相兜底：配置里还没 prepare 时删声明名 → 找到 {@code MixinInfo} 调 Mixin 自己的
 * {@code MixinConfig.removeMixin} → 直接删活动列表里的条目。任何一步失败都只记日志。</p>
 */
final class MixinDetacher {

    /** 与 {@link ChlorideJeiPatch} 对应的类名常量（避免循环引用）。 */
    private static final String CLS_ENVIRONMENT = "org.spongepowered.asm.mixin.MixinEnvironment";
    private static final String CLS_TRANSFORMER = "org.spongepowered.asm.mixin.transformer.MixinTransformer";
    private static final String CLS_MIXIN_INFO = "org.spongepowered.asm.mixin.transformer.MixinInfo";

    private static final Logger LOG = LoggerFactory.getLogger("w_misc/chloride");

    private MixinDetacher() {
    }

    /** 与 chloride 补丁共享的状态语义。 */
    enum Status { REMOVED, ALREADY_GONE, NOT_READY }

    static Status run(String stage, boolean verbose) throws Exception {
        ClassLoader loader = MixinDetacher.class.getClassLoader();
        Class<?> environmentClass = Class.forName(CLS_ENVIRONMENT, true, loader);
        Object environment = environmentClass.getMethod("getDefaultEnvironment").invoke(null);
        Object activeTransformer = environmentClass.getMethod("getActiveTransformer").invoke(environment);
        if (activeTransformer == null) {
            LOG.info("[{}] Mixin 还没有活动变换器", stage);
            return Status.NOT_READY;
        }
        LOG.info("[{}] 活动变换器：{}", stage, activeTransformer.getClass().getName());

        Object transformer = locateTransformer(activeTransformer);
        if (transformer == null) {
            LOG.warn("[{}] 无法从包装类 {} 里找到 {}", stage, activeTransformer.getClass().getName(), CLS_TRANSFORMER);
            return Status.NOT_READY;
        }
        if (transformer != activeTransformer) {
            LOG.info("[{}] Mixin 变换器被包装过：{} -> {}", stage,
                    activeTransformer.getClass().getName(), transformer.getClass().getName());
        }

        Object processor = readField(transformer, "processor");
        if (processor == null) {
            LOG.warn("[{}] {} 没有可读的 processor 字段", stage, transformer.getClass().getName());
            return Status.NOT_READY;
        }

        List<Object> activeConfigs = liveList(readField(processor, "configs"));
        List<Object> pendingConfigs = liveList(readField(processor, "pendingConfigs"));
        LOG.info("[{}] Mixin 配置：{} 个活动，{} 个待处理", stage, activeConfigs.size(), pendingConfigs.size());
        if (verbose) {
            logConfigNames(stage, "活动", activeConfigs);
            logConfigNames(stage, "待处理", pendingConfigs);
        }

        String configName = ChlorideJeiPatch.configName();
        Object config = findConfig(activeConfigs, configName);
        boolean pending = false;
        if (config == null) {
            config = findConfig(pendingConfigs, configName);
            pending = config != null;
        }
        if (config == null) {
            if (activeConfigs.isEmpty() && pendingConfigs.isEmpty()) {
                LOG.info("[{}] Mixin 还没选中配置，稍后重试", stage);
                return Status.NOT_READY;
            }
            LOG.info("[{}] 没有注册 '{}' —— 这里 chloride 没有在混入任何东西，无需处理", stage, configName);
            return Status.ALREADY_GONE;
        }

        Object prepared = readField(config, "prepared");
        int strippedNames = stripNamesFromConfig(config);
        Set<Object> mixinInfos = collectTargetMixinInfos(config);
        int detached = 0;
        for (Object mixinInfo : mixinInfos) {
            if (invokeRemoveMixin(config, mixinInfo)) {
                detached++;
            }
        }
        int dropped = dropDirectly(config, mixinInfos);

        if (strippedNames > 0 || detached > 0 || dropped > 0) {
            LOG.info("[{}] '{}' 已修改（pending={}, prepared={}）：删除 {} 个声明名，摘除 {} 个 MixinInfo，直接删 {} 条",
                    stage, configName, pending, prepared, strippedNames, detached, dropped);
            int remaining = countTargetMixins(config);
            if (remaining > 0) {
                LOG.warn("[{}] 仍有 {} 条 {} 的注册", stage, remaining, ChlorideJeiPatch.targetClass());
            } else {
                LOG.info("[{}] 复核通过：{} 不再会应用到 {}",
                        stage, ChlorideJeiPatch.relativeMixin(), ChlorideJeiPatch.targetClass());
            }
            logKeptMixins(stage, config, verbose);
            return Status.REMOVED;
        }

        if (Boolean.TRUE.equals(prepared)) {
            LOG.info("[{}] '{}' 已 prepare 但 {} 已不存在，无需处理", stage, configName, ChlorideJeiPatch.relativeMixin());
            return Status.ALREADY_GONE;
        }
        LOG.warn("[{}] 找到 '{}' 但定位不到 {}（prepared={}），稍后重试",
                stage, configName, ChlorideJeiPatch.relativeMixin(), prepared);
        return Status.NOT_READY;
    }

    private static int stripNamesFromConfig(Object config) {
        int stripped = 0;
        Object target = null;
        for (String fieldName : new String[]{"mixinClasses", "mixinClassesClient", "mixinClassesServer"}) {
            Object raw = readField(config, fieldName);
            if (raw instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> entries = (List<Object>) list;
                for (var iterator = entries.iterator(); iterator.hasNext(); ) {
                    Object entry = iterator.next();
                    if (matchesTarget(entry == null ? null : entry.toString())) {
                        iterator.remove();
                        stripped++;
                    }
                }
            }
        }
        return stripped;
    }

    private static boolean matchesTarget(String name) {
        if (name == null) {
            return false;
        }
        return name.equals(ChlorideJeiPatch.relativeMixin())
                || name.equals(ChlorideJeiPatch.targetMixin())
                || name.endsWith("." + ChlorideJeiPatch.relativeMixin());
    }

    private static Set<Object> collectTargetMixinInfos(Object config) {
        Set<Object> found = Collections.newSetFromMap(new IdentityHashMap<>());
        for (String fieldName : new String[]{"mixins", "pendingMixins"}) {
            for (Object mixinInfo : liveList(readField(config, fieldName))) {
                if (isTargetMixinInfo(mixinInfo)) {
                    found.add(mixinInfo);
                }
            }
        }
        Object mapping = readField(config, "mixinMapping");
        if (mapping instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                for (Object mixinInfo : liveList(value)) {
                    if (isTargetMixinInfo(mixinInfo)) {
                        found.add(mixinInfo);
                    }
                }
            }
        }
        return found;
    }

    private static boolean isTargetMixinInfo(Object candidate) {
        return candidate != null && ChlorideJeiPatch.targetMixin().equals(invokeString(candidate, "getClassName"));
    }

    private static boolean invokeRemoveMixin(Object config, Object mixinInfo) {
        try {
            Class<?> infoClass = Class.forName(CLS_MIXIN_INFO, false, config.getClass().getClassLoader());
            Method method = config.getClass().getDeclaredMethod("removeMixin", infoClass);
            method.setAccessible(true);
            method.invoke(config, mixinInfo);
            return true;
        } catch (Throwable t) {
            LOG.warn("无法调用 MixinConfig.removeMixin（{}），改用直接摘除", t.toString());
            return false;
        }
    }

    private static int dropDirectly(Object config, Set<Object> mixinInfos) {
        int dropped = 0;
        Object mapping = readField(config, "mixinMapping");
        if (mapping instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (value instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Object> entries = (List<Object>) list;
                    for (var iterator = entries.iterator(); iterator.hasNext(); ) {
                        if (isTargetMixinInfo(iterator.next())) {
                            iterator.remove();
                            dropped++;
                        }
                    }
                }
            }
        }
        for (String fieldName : new String[]{"mixins", "pendingMixins"}) {
            Object raw = readField(config, fieldName);
            if (raw instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> entries = (List<Object>) list;
                for (var iterator = entries.iterator(); iterator.hasNext(); ) {
                    if (isTargetMixinInfo(iterator.next())) {
                        iterator.remove();
                        dropped++;
                    }
                }
            }
        }
        return dropped;
    }

    private static int countTargetMixins(Object config) {
        int count = 0;
        for (String fieldName : new String[]{"mixins", "pendingMixins"}) {
            for (Object mixinInfo : liveList(readField(config, fieldName))) {
                if (isTargetMixinInfo(mixinInfo)) {
                    count++;
                }
            }
        }
        Object mapping = readField(config, "mixinMapping");
        if (mapping instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                for (Object mixinInfo : liveList(value)) {
                    if (isTargetMixinInfo(mixinInfo)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void logKeptMixins(String stage, Object config, boolean verbose) {
        if (!verbose) {
            return;
        }
        Object raw = readField(config, "mixins");
        if (raw instanceof List<?> list) {
            LOG.info("[{}] chloride 其余 {} 条 mixin 仍然在位", stage, list.size());
        }
    }

    private static Object findConfig(List<Object> configs, String name) {
        for (Object config : configs) {
            if (name.equals(invokeString(config, "getName"))) {
                return config;
            }
        }
        return null;
    }

    private static void logConfigNames(String stage, String kind, List<Object> configs) {
        List<String> names = new ArrayList<>(configs.size());
        for (Object config : configs) {
            names.add(String.valueOf(invokeString(config, "getName")));
        }
        LOG.info("[{}] {}配置：{}", stage, kind, names);
    }

    /**
     * Mixin 的变换器可能被包装（例如 mixin-booster）。沿字段广度优先找 MixinTransformer；绝不抛错。
     */
    private static Object locateTransformer(Object activeTransformer) {
        if (isInstanceOf(activeTransformer, CLS_TRANSFORMER)) {
            return activeTransformer;
        }
        Deque<Object> queue = new ArrayDeque<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        queue.add(activeTransformer);
        int budget = 48;
        while (!queue.isEmpty() && budget-- > 0) {
            Object current = queue.poll();
            if (current == null || !seen.add(current)) {
                continue;
            }
            Class<?> type = current.getClass();
            if (type.isPrimitive() || type.isArray() || type.getName().startsWith("java.")
                    || type.getName().startsWith("jdk.") || type.getName().startsWith("sun.")) {
                continue;
            }
            for (Class<?> klass = type; klass != null && klass != Object.class; klass = klass.getSuperclass()) {
                for (Field field : klass.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object value = field.get(current);
                        if (value == null) {
                            continue;
                        }
                        if (isInstanceOf(value, CLS_TRANSFORMER)) {
                            return value;
                        }
                        queue.add(value);
                    } catch (Throwable ignored) {
                        // 读不到的字段跳过，继续在别处找
                    }
                }
            }
        }
        return null;
    }

    private static boolean isInstanceOf(Object instance, String className) {
        for (Class<?> klass = instance.getClass(); klass != null; klass = klass.getSuperclass()) {
            if (className.equals(klass.getName())) {
                return true;
            }
        }
        return false;
    }

    private static Object readField(Object target, String name) {
        if (target == null) {
            return null;
        }
        for (Class<?> klass = target.getClass(); klass != null && klass != Object.class; klass = klass.getSuperclass()) {
            try {
                Field field = klass.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // 往父类找
            } catch (Throwable t) {
                LOG.warn("读不到 {} 的字段 '{}'：{}", target.getClass().getName(), name, t.toString());
                return null;
            }
        }
        LOG.warn("{} 上不存在字段 '{}'", target.getClass().getName(), name);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> liveList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return Collections.emptyList();
    }

    private static String invokeString(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke(target);
            return result == null ? null : result.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
