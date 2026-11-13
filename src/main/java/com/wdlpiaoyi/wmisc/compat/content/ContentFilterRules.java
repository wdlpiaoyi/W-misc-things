package com.wdlpiaoyi.wmisc.compat.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 内容过滤规则与资源改写。
 *
 * <p>不依赖资源包顺序：在资源管理器返回资源之后、上层解析之前改写内容。这样无论目标模组的包在
 * 我们上面还是下面都生效，换实例时只要装本模组即可，不需要往实例里放资源包/数据包。</p>
 *
 * <p>能做的改写：</p>
 * <ul>
 *   <li>配方 / 进度：替换成永不满足条件的占位（Forge {@code forge:false} / 不可达进度）；</li>
 *   <li>语言文件：整个 JSON 解析后按 key 正则把内容换成中性文本，其它 key 原样保留；</li>
 *   <li>贴图：替换成 1x1 透明 PNG；</li>
 *   <li>主菜单 splash：按行过滤掉带宣传关键词的行，其余行保留
 *       （不像整文件覆盖那样把原版行弄丢）。</li>
 * </ul>
 */
public final class ContentFilterRules {
    private static final Logger LOG = LoggerFactory.getLogger("w_misc/content_filter");

    /** 1x1 全透明 PNG。 */
    private static final byte[] TRANSPARENT_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    private static final byte[] DISABLED_RECIPE = """
            {"type":"minecraft:crafting_shaped","conditions":[{"type":"forge:false"}]}
            """.trim().getBytes(StandardCharsets.UTF_8);

    private static final byte[] DISABLED_ADVANCEMENT = """
            {"criteria":{"w_misc_blocked":{"trigger":"minecraft:impossible"}}}
            """.trim().getBytes(StandardCharsets.UTF_8);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Pattern SPLASH_BANNED = Pattern.compile(
            "(?i).*\\b(gay|gays|trans|transgender|pride|prideful|lgbt|lgbtq|queer|lesbian|bisexual|nonbinary|enby)\\b.*");

    public enum Kind {
        RECIPE_DISABLE,
        ADVANCEMENT_DISABLE,
        TEXTURE_TRANSPARENT,
        LANG_NEUTRALIZE,
        SPLASH_FILTER
    }

    public record Rule(Pattern path, Kind kind, Pattern keyPattern, String replacement) {
    }

    private static final List<Rule> RULES = List.of(
            rule("^malum:recipes/.*prideweave\\.json$", Kind.RECIPE_DISABLE),
            rule("^malum:advancements/.*prideweave\\.json$", Kind.ADVANCEMENT_DISABLE),
            rule("^malum:textures/.*pride.*\\.png$", Kind.TEXTURE_TRANSPARENT),
            rule("^malum:lang/(en_us|zh_cn)\\.json$", Kind.LANG_NEUTRALIZE, "(?i).*pride.*", "织物"),
            rule("^immersiveengineering:lang/(en_us|zh_cn)\\.json$", Kind.LANG_NEUTRALIZE, "(?i).*pride.*", "自定义着色器"),
            rule("^mekanism:lang/(en_us|zh_cn)\\.json$", Kind.LANG_NEUTRALIZE, "(?i)^robit_skin\\.mekanism\\.", "Robit 皮肤"),
            rule("^minecraft:texts/splashes\\.txt$", Kind.SPLASH_FILTER)
    );

    private static final Map<String, Integer> HITS = new HashMap<>();

    /** 由 ContentFilterFeature 在配置加载后置位；未启用时所有规则都不生效。 */
    private static volatile boolean active;

    private ContentFilterRules() {
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }

    private static Rule rule(String path, Kind kind) {
        return new Rule(Pattern.compile(path), kind, null, null);
    }

    private static Rule rule(String path, Kind kind, String keyPattern, String replacement) {
        return new Rule(Pattern.compile(path), kind, Pattern.compile(keyPattern), replacement);
    }

    private static volatile boolean firstQueryLogged;

    /** 这个资源是否需要过滤；不需要返回 null。 */
    public static Rule match(ResourceLocation id) {
        if (!active) {
            return null;
        }
        if (!firstQueryLogged) {
            firstQueryLogged = true;
            LOG.info("[w_misc] 资源过滤器已接管（首次查询：{}）", id);
        }
        String key = id.getNamespace() + ":" + id.getPath();
        for (Rule rule : RULES) {
            if (rule.path().matcher(key).matches()) {
                return rule;
            }
        }
        return null;
    }

    /** 对单个资源执行改写；不需要改写或失败时返回 empty。 */
    public static Optional<Resource> rewrite(Resource original, Rule rule) {
        try (InputStream in = original.open()) {
            byte[] data = in.readAllBytes();
            byte[] replaced = transform(rule, data);
            if (replaced == null) {
                return Optional.empty();
            }
            note(rule);
            return Optional.of(new Resource(original.source(), () -> new ByteArrayInputStream(replaced)));
        } catch (IOException | RuntimeException e) {
            LOG.warn("[w_misc] 过滤资源失败，保留原样：{}", e.toString());
            return Optional.empty();
        }
    }

    /** 批量过滤（listResources 的返回值）；没有命中时原样返回，避免无谓拷贝。 */
    public static Map<ResourceLocation, Resource> rewriteAll(Map<ResourceLocation, Resource> resources) {
        Map<ResourceLocation, Resource> out = null;
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            Rule rule = match(entry.getKey());
            if (rule == null) {
                continue;
            }
            Optional<Resource> replaced = rewrite(entry.getValue(), rule);
            if (replaced.isPresent()) {
                if (out == null) {
                    out = new HashMap<>(resources);
                }
                out.put(entry.getKey(), replaced.get());
            }
        }
        return out == null ? resources : out;
    }

    private static byte[] transform(Rule rule, byte[] data) {
        return switch (rule.kind()) {
            case RECIPE_DISABLE -> DISABLED_RECIPE.clone();
            case ADVANCEMENT_DISABLE -> DISABLED_ADVANCEMENT.clone();
            case TEXTURE_TRANSPARENT -> TRANSPARENT_PNG.clone();
            case LANG_NEUTRALIZE -> neutralizeLang(data, rule.keyPattern(), rule.replacement());
            case SPLASH_FILTER -> filterSplashes(data);
        };
    }

    private static byte[] neutralizeLang(byte[] data, Pattern keyPattern, String replacement) {
        if (keyPattern == null) {
            return null;
        }
        try {
            JsonElement parsed = GSON.fromJson(new String(data, StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                return null;
            }
            JsonObject object = parsed.getAsJsonObject();
            boolean changed = false;
            for (String key : new ArrayList<>(object.keySet())) {
                if (keyPattern.matcher(key).matches()) {
                    object.addProperty(key, replacement);
                    changed = true;
                }
            }
            return changed ? GSON.toJson(object).getBytes(StandardCharsets.UTF_8) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static byte[] filterSplashes(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        List<String> kept = new ArrayList<>();
        boolean changed = false;
        for (String line : text.split("\r?\n", -1)) {
            if (!line.isBlank() && SPLASH_BANNED.matcher(line).matches()) {
                changed = true;
                continue;
            }
            kept.add(line);
        }
        return changed ? String.join("\n", kept).getBytes(StandardCharsets.UTF_8) : null;
    }

    /** 每种规则第一次命中时打一条日志，避免刷屏。 */
    private static void note(Rule rule) {
        if (HITS.putIfAbsent(rule.kind().name(), 1) == null) {
            LOG.info("[w_misc] 内容过滤命中：{}（规则 {}）", rule.kind(), rule.path().pattern());
        }
    }

    public static int ruleCount() {
        return RULES.size();
    }

    /** 供启动日志自检用。 */
    public static List<String> ruleSummary() {
        List<String> lines = new ArrayList<>(RULES.size());
        for (Rule rule : RULES) {
            lines.add(rule.kind() + " " + rule.path().pattern());
        }
        return lines;
    }

    /** 测试/自检用：对一段 splash 文本跑过滤规则。 */
    public static String filterSplashTextForTest(String text) {
        byte[] result = filterSplashes(text.getBytes(StandardCharsets.UTF_8));
        return result == null ? text : new String(result, StandardCharsets.UTF_8);
    }
}
