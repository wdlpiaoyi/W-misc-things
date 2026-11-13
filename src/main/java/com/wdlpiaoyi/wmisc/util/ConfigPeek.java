package com.wdlpiaoyi.wmisc.util;

import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 在 Forge 配置尚未加载的阶段（mod 构造 / Mixin 注入）读取本模组配置文件的极简解析器。
 *
 * <p>Forge 的 COMMON 配置要等所有 mod 构造完之后才加载，所以「构造期就要知道开关」的功能
 * （死锁修复、chloride 补丁）必须自己读一遍 TOML。只解析本模组自己写的、结构固定的 key = value，
 * 不支持数组、内联表、多行字符串；解析失败一律按默认值处理，绝不抛错。</p>
 */
public final class ConfigPeek {
    public static final String FILE_NAME = "w_misc-common.toml";

    private static final Logger LOG = LoggerFactory.getLogger("w_misc/config");
    private static Map<String, Map<String, String>> cache;

    private ConfigPeek() {
    }

    public static synchronized Map<String, String> section(String name) {
        if (cache == null) {
            cache = parse();
        }
        Map<String, String> values = cache.get(name);
        return values == null ? Map.of() : values;
    }

    public static boolean getBoolean(String section, String key, boolean fallback) {
        String raw = section(section).get(key);
        if (raw == null) {
            return fallback;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> fallback;
        };
    }

    public static String getString(String section, String key, String fallback) {
        String raw = section(section).get(key);
        return raw == null ? fallback : raw;
    }

    private static Map<String, Map<String, String>> parse() {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
            if (!Files.exists(file)) {
                LOG.debug("[w_misc] {} 不存在（首次启动），按默认值处理", FILE_NAME);
                return result;
            }
            String section = "";
            for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1).trim();
                    continue;
                }
                int cut = line.indexOf('=');
                if (cut <= 0) {
                    continue;
                }
                String key = line.substring(0, cut).trim();
                String value = line.substring(cut + 1).trim();
                int comment = value.indexOf('#');
                if (comment >= 0) {
                    value = value.substring(0, comment).trim();
                }
                result.computeIfAbsent(section, ignored -> new HashMap<>()).put(key, unquote(value));
            }
        } catch (IOException | RuntimeException e) {
            LOG.debug("[w_misc] 读取 {} 失败，按默认值处理: {}", FILE_NAME, e.toString());
        }
        return result;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
