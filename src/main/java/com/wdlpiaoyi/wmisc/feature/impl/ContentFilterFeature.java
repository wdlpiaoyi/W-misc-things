package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.Compat;
import com.wdlpiaoyi.wmisc.compat.content.ContentFilterRules;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Field;

/**
 * 内容过滤：屏蔽模组注入的政治宣传内容（目前整合包里确认存在的几处）。
 *
 * <p>全部改动都在模组内部完成，换实例、换整合包不需要再往实例里放资源包/数据包：</p>
 * <ol>
 *   <li>配方 / 进度 / 语言 / 贴图 / splash 在 {@code MultiPackResourceManager} 返回资源之后
 *       按规则改写（见 {@link ContentFilterRules}），与模组之间资源包顺序无关；</li>
 *   <li>Quark 的彩虹 Q 按钮：{@code QuarkQButtonMixin} 固定前景色；</li>
 *   <li>Quark 的 13 种 LGBT 蜜蜂：客户端每 20 tick 把
 *       {@code VariantAnimalTexturesModule.enableLGBTBees / everyBeeIsLGBT} 置回 false，
 *       防止它自己的配置或界面把开关改回来。</li>
 * </ol>
 *
 * <p>规则覆盖：Malum 的 prideweave（配方/进度/贴图/名称）、ImmersiveEngineering 着色器名称说明、
 * Mekanism robit 皮肤名称、原版 splash 中的宣传句。</p>
 */
public final class ContentFilterFeature implements WmiscFeature {
    private static final String QUARK_MODULE = "org.violetmoon.quark.content.client.module.VariantAnimalTexturesModule";

    private static volatile boolean active;
    private static boolean quarkFlagsForced;
    private static int clientTicks;

    @Override
    public String id() {
        return "content_filter";
    }

    @Override
    public String summary() {
        return "屏蔽模组注入的政治宣传内容（Malum / IE / Mekanism / Quark / splash）";
    }

    @Override
    public boolean implemented() {
        return true;
    }

    @Override
    public void onConstruct() {
        // 客户端资源（语言/贴图/splash）在 common setup 之前就可能被读，所以构造期就置位。
        // 此时 Forge 配置还没加载，直接读配置文件。
        boolean enabled = ConfigPeek.getBoolean("features.content_filter", "enabled", true);
        active = enabled;
        ContentFilterRules.setActive(enabled);
    }

    @Override
    public void onCommonSetup() {
        active = true;
        ContentFilterRules.setActive(true);
        Wmisc.LOG.info("[w_misc] 内容过滤已启用：{} 条资源规则（与资源包顺序无关）；Malum 已跳过 {} 个宣传物品",
                ContentFilterRules.ruleCount(), ContentFilterRules.skippedItemCount());
    }

    @Override
    public void onClientSetup() {
        forceQuarkBeeFlags();
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    public static boolean isActive() {
        return active;
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (++clientTicks % 20 != 0) {
            return;
        }
        forceQuarkBeeFlags();
    }

    /** Quark 把这两个开关做成 public static 字段，直接反射置 false；配置界面改回来也会被压回去。 */
    private static void forceQuarkBeeFlags() {
        if (!Compat.isModLoaded("quark")) {
            return;
        }
        try {
            Class<?> module = Class.forName(QUARK_MODULE);
            Field lgbt = module.getField("enableLGBTBees");
            Field every = module.getField("everyBeeIsLGBT");
            boolean changed = lgbt.getBoolean(null) || every.getBoolean(null);
            lgbt.setBoolean(null, false);
            every.setBoolean(null, false);
            if (!quarkFlagsForced) {
                quarkFlagsForced = true;
                Wmisc.LOG.info("[w_misc] 已关闭 Quark 的 LGBT 蜜蜂变体（enableLGBTBees=false）");
            } else if (changed) {
                Wmisc.LOG.debug("[w_misc] Quark 的 LGBT 蜜蜂开关被改回 true，已重新关闭");
            }
        } catch (Throwable t) {
            if (!quarkFlagsForced) {
                quarkFlagsForced = true;
                Wmisc.LOG.warn("[w_misc] 无法关闭 Quark 的 LGBT 蜜蜂开关：{}", t.toString());
            }
        }
    }
}

