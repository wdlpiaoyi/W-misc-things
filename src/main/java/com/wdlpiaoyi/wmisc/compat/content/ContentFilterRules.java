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
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 内容过滤规则与资源改写。
 *
 * <p>不依赖资源包顺序：在资源管理器返回资源之后、上层解析之前处理。这样无论目标模组的包在
 * 我们上面还是下面都生效，换实例时只要装本模组即可，不需要往实例里放资源包/数据包。</p>
 *
 * <p>处理方式：</p>
 * <ul>
 *   <li>{@link Kind#REMOVE}：整条资源删掉（配方不进 RecipeManager、数据包注册表里不存在）；</li>
 *   <li>语言文件：按 key / value 正则把内容换成中性文本，其它 key 原样保留；</li>
 *   <li>贴图：替换成 1x1 透明 PNG；</li>
 *   <li>主菜单 splash：按行过滤掉带宣传关键词的行，其余行保留。</li>
 * </ul>
 *
 * <p>此外 Malum 的 pride 物品在注册阶段就被跳过（见 {@code MalumPrideItemRemovalMixin}），
 * IE 的 6 个 pride 着色器在注册后被摘除（见 {@code ImmersiveEngineeringShaderMixin}）。</p>
 */
public final class ContentFilterRules {
    private static final Logger LOG = LoggerFactory.getLogger("w_misc/content_filter");

    /** 1x1 全透明 PNG。 */
    private static final byte[] TRANSPARENT_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public enum Kind {
        /** 直接把资源从加载列表里删掉。 */
        REMOVE,
        TEXTURE_TRANSPARENT,
        LANG_NEUTRALIZE,
        SPLASH_FILTER
    }

    /** keyPattern / valuePattern 任一命中就把这条语言条目换成 replacement。 */
    public record Rule(Pattern path, Kind kind, Pattern keyPattern, Pattern valuePattern, String replacement) {
    }

    private static final Pattern PRIDE_WORDS = Pattern.compile(
            "(?i).*\\b(gay|gays|trans|transgender|pride|prideful|lgbt|lgbtq|queer|lesbian|bisexual|nonbinary|non-binary|enby|asexual|asexuality|intersex|pansexual|pan awareness|coming out)\\b.*");

    private static final Pattern MALUM_BLOCKED_ITEM = Pattern.compile("(?i).*pride.*");

    private static final Pattern MEKANISM_PRIDE_SKIN = Pattern.compile(
            "(?i)(ace|agender|aro|bi|enby|gay|genderfluid|lesbian|pan|pride|trans)");

    /** IE 的 6 个 pride 着色器 id。 */
    private static final Set<String> IE_PRIDE_SHADERS =
            Set.of("acepride", "bipride", "enbypride", "gaypride", "lesbianpride", "transpride");

    private static final List<Rule> RULES = List.of(
            // Malum：配方 / 进度整条删除；贴图透明；名称中性
            rule("^malum:recipes/.*prideweave\\.json$", Kind.REMOVE),
            rule("^malum:advancements/.*prideweave.*\\.json$", Kind.REMOVE),
            rule("^malum:textures/.*pride.*\\.png$", Kind.TEXTURE_TRANSPARENT),
            lang("^malum:lang/(en_us|zh_cn)\\.json$", "(?i).*pride.*", "织物"),

            // Mekanism：11 个 pride robit 皮肤的数据文件整条删除，名称中性
            rule("^mekanism:mekanism/robit_skin/(ace|agender|aro|bi|enby|gay|genderfluid|lesbian|pan|pride|trans)\\.json$",
                    Kind.REMOVE),
            lang("^mekanism:lang/(en_us|zh_cn)\\.json$",
                    "(?i)^robit_skin\\.mekanism\\.(ace|agender|aro|bi|enby|gay|genderfluid|lesbian|pan|pride|trans)$",
                    "Robit 皮肤"),

            // Immersive Engineering：着色器名称 / 说明中性（着色器本体在注册后由 mixin 摘除）
            lang("^immersiveengineering:lang/(en_us|zh_cn)\\.json$", "(?i).*pride.*", "自定义着色器"),

            // Quark：节日提示里带宣传关键词的条目中性化；彩虹相关隐藏进度整条删除
            lang("^quark:lang/(en_us|zh_cn)\\.json$", "(?i)^quark\\.gui\\.celebration\\.", PRIDE_WORDS, "Quark"),
            lang("^quark:lang/(en_us|zh_cn)\\.json$", "(?i)^advancements\\.quark\\.wear_full_rainbow\\.", ""),
            rule("^quark:advancements/wear_full_rainbow\\.json$", Kind.REMOVE),

            // 原版主菜单 splash：逐行过滤
            rule("^minecraft:texts/splashes\\.txt$", Kind.SPLASH_FILTER)
    );

    private static final Map<String, Integer> HITS = new HashMap<>();
    private static final AtomicInteger SKIPPED_ITEMS = new AtomicInteger();

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
        return new Rule(Pattern.compile(path), kind, null, null, null);
    }

    private static Rule lang(String path, String keyPattern, String replacement) {
        return new Rule(Pattern.compile(path), Kind.LANG_NEUTRALIZE,
                Pattern.compile(keyPattern), null, replacement);
    }

    private static Rule lang(String path, String keyPattern, Pattern valuePattern, String replacement) {
        return new Rule(Pattern.compile(path), Kind.LANG_NEUTRALIZE,
                Pattern.compile(keyPattern), valuePattern, replacement);
    }

    /** 这个资源是否需要处理；不需要返回 null。 */
    public static Rule match(ResourceLocation id) {
        if (!active) {
            return null;
        }
        String key = id.getNamespace() + ":" + id.getPath();
        for (Rule rule : RULES) {
            if (rule.path().matcher(key).matches()) {
                return rule;
            }
        }
        return null;
    }

    /** 这个规则是否表示「整条资源删除」而不是改写内容。 */
    public static boolean isRemoval(Rule rule) {
        return rule != null && rule.kind() == Kind.REMOVE;
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
            if (out == null) {
                out = new HashMap<>(resources);
            }
            if (isRemoval(rule)) {
                out.remove(entry.getKey());
                note(rule);
                continue;
            }
            Optional<Resource> replaced = rewrite(entry.getValue(), rule);
            if (replaced.isPresent()) {
                out.put(entry.getKey(), replaced.get());
            }
        }
        return out == null ? resources : out;
    }

    private static byte[] transform(Rule rule, byte[] data) {
        return switch (rule.kind()) {
            case REMOVE -> null;
            case TEXTURE_TRANSPARENT -> TRANSPARENT_PNG.clone();
            case LANG_NEUTRALIZE -> neutralizeLang(data, rule.keyPattern(), rule.valuePattern(), rule.replacement());
            case SPLASH_FILTER -> filterSplashes(data);
        };
    }

    private static byte[] neutralizeLang(byte[] data, Pattern keyPattern, Pattern valuePattern, String replacement) {
        if (keyPattern == null && valuePattern == null) {
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
                JsonElement value = object.get(key);
                boolean hit = keyPattern != null && keyPattern.matcher(key).matches();
                if (!hit && valuePattern != null && value != null && value.isJsonPrimitive()) {
                    hit = valuePattern.matcher(value.getAsString()).matches();
                }
                if (hit) {
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
            if (!line.isBlank() && PRIDE_WORDS.matcher(line).matches()) {
                changed = true;
                continue;
            }
            kept.add(line);
        }
        return changed ? String.join("\n", kept).getBytes(StandardCharsets.UTF_8) : null;
    }

    /** Malum 注册时是否要跳过这个物品名（直接从注册表消失，而不是只屏蔽配方）。 */
    public static boolean isBlockedMalumItemName(String name) {
        return active && name != null && MALUM_BLOCKED_ITEM.matcher(name).matches();
    }

    /** IE 注册后要摘除的 pride 着色器 id。 */
    public static boolean isBlockedIeShader(String path) {
        return active && path != null && IE_PRIDE_SHADERS.contains(path.toLowerCase(java.util.Locale.ROOT));
    }

    /** 记录一个被跳过的注册项（用于启动日志/自检）。 */
    public static void noteSkippedItem(String name) {
        int count = SKIPPED_ITEMS.incrementAndGet();
        if (count == 1) {
            LOG.info("[w_misc] 已从 Malum 注册表跳过宣传物品（首个：{}）", name);
        }
    }

    public static int skippedItemCount() {
        return SKIPPED_ITEMS.get();
    }

    /** 每种规则第一次命中时打一条日志，避免刷屏。 */
    private static void note(Rule rule) {
        if (HITS.putIfAbsent(rule.kind().name() + "|" + rule.path().pattern(), 1) == null) {
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
