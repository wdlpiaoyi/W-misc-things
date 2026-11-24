package com.wdlpiaoyi.wmisc.compat.chloride;

import com.wdlpiaoyi.wmisc.compat.mixin.MixinDetacher;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * chloride 1.8.1 × JEI 15.70+ 运行时兼容补丁（从 chloridejeicompat 并入）。
 *
 * <p>chloride 1.8.1 的 {@code chloride.mixin.json} 无条件注册了
 * {@code jei_rei_emi.JeiOverlayMixin}，而 JEI 15.70+ 删掉了它的 {@code @Shadow} 字段目标，
 * 导致 {@code InvalidMixinException}，且该配置 {@code required:true} → 进世界直接崩。</p>
 *
 * <p>我们不动 chloride / JEI 的 jar，也不补 JEI 成员，只在 Mixin 运行时里把这一条 mixin 摘掉；
 * chloride 其余 35 条 mixin 照常工作。原模组被并入本模组后，实例里不再需要单独装
 * {@code chloridejeicompat}。</p>
 */
public final class ChlorideJeiPatch {
    private static final Logger LOG = LoggerFactory.getLogger("w_misc/chloride");

    private static final String SECTION = "features.chloride_jei_bundle";
    private static final String CONFIG_NAME = "chloride.mixin.json";
    private static final String TARGET_MIXIN = "me.srrapero720.chloride.mixins.impl.jei_rei_emi.JeiOverlayMixin";
    private static final String TARGET_CLASS = "mezz.jei.gui.overlay.IngredientListOverlay";
    private static final String RELATIVE_MIXIN = "jei_rei_emi.JeiOverlayMixin";

    private static final MixinDetacher.Spec SPEC =
            new MixinDetacher.Spec(CONFIG_NAME, TARGET_MIXIN, RELATIVE_MIXIN, TARGET_CLASS);

    private static final int MAX_ATTEMPTS = 3;

    private static int attempts;
    private static boolean finished;

    private ChlorideJeiPatch() {
    }

    private enum GateResult { PROCEED, SKIP, NOT_READY }

    /** 首个 chloride 1.8.1 的 JEI mixin 无法应用的 JEI 版本。 */
    private static final String MIN_JEI_VERSION = "15.70.0.221";

    /** 已知有坏 mixin 的 chloride 版本；升级后自动让位，不用改代码。 */
    private static final Set<String> KNOWN_BAD_CHLORIDE = Set.of("1.8.1");

    /**
     * 构造期 / common setup / client setup 各试一次，幂等。
     *
     * @param stage 日志里用的阶段名
     */
    public static synchronized void attempt(String stage) {
        if (finished) {
            return;
        }
        if (!ConfigPeek.getBoolean(SECTION, "enabled", true)) {
            finished = true;
            LOG.info("[{}] 配置里已停用 chloride 补丁，不处理", stage);
            return;
        }
        attempts++;
        GateResult gate = checkGate();
        switch (gate) {
            case SKIP -> {
                finished = true;
                LOG.info("[{}] 跳过：{}", stage, gateReason());
                return;
            }
            case NOT_READY -> {
                LOG.info("[{}] 暂不可用：{}", stage, gateReason());
                if (attempts >= MAX_ATTEMPTS) {
                    failUnfinished(stage, gateReason());
                }
                return;
            }
            case PROCEED -> LOG.info("[{}] 前置条件满足：{}", stage, gateReason());
        }

        long started = System.nanoTime();
        try {
            MixinDetacher.Status status = MixinDetacher.run(stage, SPEC, verbose());
            long millis = (System.nanoTime() - started) / 1_000_000L;
            if (status == MixinDetacher.Status.NOT_READY) {
                LOG.info("[{}] 尚未应用（{} ms，第 {}/{} 次），稍后重试", stage, millis, attempts, MAX_ATTEMPTS);
                if (attempts >= MAX_ATTEMPTS) {
                    failUnfinished(stage, "三个钩子点上 Mixin 运行时都不可用");
                }
            } else {
                finished = true;
                LOG.info("[{}] 完成，用时 {} ms；{} 已从后续 JEI 类变换中摘除", stage, millis, TARGET_MIXIN);
            }
        } catch (Throwable t) {
            LOG.error("[{}] 打补丁时出现意外错误，游戏保持原状", stage, t);
            if (attempts >= MAX_ATTEMPTS) {
                failUnfinished(stage, t.toString());
            }
        }
    }

    private static String gateReasonCache;

    private static GateResult checkGate() {
        StringBuilder reason = new StringBuilder();
        GateResult result = computeGate(reason);
        gateReasonCache = reason.toString();
        return result;
    }

    private static String gateReason() {
        return gateReasonCache == null ? "" : gateReasonCache;
    }

    private static GateResult computeGate(StringBuilder reason) {
        if (ConfigPeek.getBoolean(SECTION, "force", false)) {
            reason.append("配置里 force=true，跳过版本门控");
            return GateResult.PROCEED;
        }
        ModList modList;
        try {
            modList = ModList.get();
        } catch (Throwable t) {
            reason.append("模组列表还没准备好（").append(t).append("）");
            return GateResult.NOT_READY;
        }
        if (modList == null) {
            reason.append("模组列表尚未初始化");
            return GateResult.NOT_READY;
        }
        if (!modList.isLoaded("chloride")) {
            reason.append("没装 chloride");
            return GateResult.SKIP;
        }
        if (!modList.isLoaded("jei")) {
            reason.append("没装 jei");
            return GateResult.SKIP;
        }
        String chloride = versionString(modList, "chloride");
        String jei = versionString(modList, "jei");
        if (chloride == null || jei == null) {
            reason.append("读不到模组版本");
            return GateResult.NOT_READY;
        }
        if (!KNOWN_BAD_CHLORIDE.contains(chloride)) {
            reason.append("chloride ").append(chloride).append(" 不在已知坏版本内（")
                    .append(String.join(", ", KNOWN_BAD_CHLORIDE.stream().sorted().toList())).append("）");
            return GateResult.SKIP;
        }
        if (compare(jei, MIN_JEI_VERSION) < 0) {
            reason.append("jei ").append(jei).append(" 低于 ").append(MIN_JEI_VERSION)
                    .append("，chloride 自带的联动还能用");
            return GateResult.SKIP;
        }
        reason.append("chloride ").append(chloride).append(" × jei ").append(jei);
        return GateResult.PROCEED;
    }

    private static boolean verbose() {
        return ConfigPeek.getBoolean(SECTION, "verbose", false);
    }

    private static String versionString(ModList modList, String modId) {
        try {
            IModFileInfo info = modList.getModFileById(modId);
            return info == null ? null : info.versionString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 数字点号比较；非数字段与缺段按 0 处理。 */
    static int compare(String left, String right) {
        String[] a = left.split("[^0-9]+");
        String[] b = right.split("[^0-9]+");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            long x = numberAt(a, i);
            long y = numberAt(b, i);
            if (x != y) {
                return Long.compare(x, y);
            }
        }
        return 0;
    }

    private static long numberAt(String[] parts, int index) {
        if (index >= parts.length || parts[index].isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(parts[index]);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static void failUnfinished(String stage, String reason) {
        finished = true;
        LOG.error("[{}] 无法摘除 {}（{}）。", stage, TARGET_MIXIN, reason);
        LOG.error("[{}] 装 JEI 后大概率仍会在加载 {} 时崩溃。", stage, TARGET_CLASS);
        LOG.error("[{}] 应急方案：从 chloride.mixin.json 里删掉 jei_rei_emi.JeiOverlayMixin 再重打包。", stage);
    }

    /** 供日志/文档引用：当前补丁的 mixin 类名单。 */
    public static List<String> relevantClasses() {
        return List.of(TARGET_MIXIN, TARGET_CLASS);
    }
}
