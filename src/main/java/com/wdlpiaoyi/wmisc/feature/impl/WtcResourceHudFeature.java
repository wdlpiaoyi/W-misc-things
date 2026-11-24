package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.wtc.WtcHud;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

public final class WtcResourceHudFeature implements WmiscFeature {
    private static final String SECTION = "features.wtc_resource_hud";
    private static ForgeConfigSpec.BooleanValue hideWhenEmpty;
    private static ForgeConfigSpec.BooleanValue hideWhenDead;
    private static ForgeConfigSpec.BooleanValue hideInSpectator;
    private static ForgeConfigSpec.BooleanValue hideInCreative;
    private static ForgeConfigSpec.BooleanValue forceHide;
    private static ForgeConfigSpec.IntValue hideAfterTicks;
    private static volatile boolean active;

    public String id() {
        return "wtc_resource_hud";
    }

    public String summary() {
        return "WTC 肾上腺素/怒气条：/wmisc wtc_resource_hud 调位置，空值/死亡/旁观/久无变化不显示";
    }

    public List<String> requiredMods() {
        return List.of("wizard_terra_cuiros");
    }

    public void defineOptions(ForgeConfigSpec.Builder builder) {
        builder.comment("位置编辑：游戏内输入 /wmisc wtc_resource_hud打开界面，界面里拖拽/滚轮/方向键调整");
        hideWhenEmpty = builder.comment("条里没有任何积累（值为 0）时不显示").define("hide_when_empty", true);
        hideWhenDead = builder.comment("玩家死亡/濒死时不显示").define("hide_when_dead", true);
        hideInSpectator = builder.comment("旁观模式不显示").define("hide_in_spectator", true);
        hideInCreative = builder.comment("创造模式不显示（默认关：创造里常用来测数值）").define("hide_in_creative", false);
        forceHide = builder.comment("硬关：两条一律不显示（只想用编辑工具调位置时别开这个）").define("force_hide", false);
        hideAfterTicks = builder.comment("静默时长（tick）：数值这么久没变化就不显示，200 = 10 秒；0 = 关闭").defineInRange("hide_after_ticks", 200, 0, 72000);
    }

    public void onClientSetup() {
        if (!WtcHud.available()) {
            Wmisc.LOG.warn("[w_misc] 没接上 Wizard Terra Curios 的资源条配置，位置编辑工具不可用（显隐条件仍然生效）");
        }

        active = true;
        Wmisc.LOG.info("[w_misc] WTC 资源条：位置编辑 = /wmisc wtc_resource_hud；显示条件 = 空值 {}、死亡 {}、旁观 {}、创造 {}、硬关 {}、静默 {} tick", new Object[]{hideWhenEmpty(), hideWhenDead(), hideInSpectator(), hideInCreative(), forceHide(), hideAfterTicks()});
    }

    public void onConfigReload(Map<String, String> values) {
        ConfigPeek.applyBoolean(values, "hide_when_empty", hideWhenEmpty);
        ConfigPeek.applyBoolean(values, "hide_when_dead", hideWhenDead);
        ConfigPeek.applyBoolean(values, "hide_in_spectator", hideInSpectator);
        ConfigPeek.applyBoolean(values, "hide_in_creative", hideInCreative);
        ConfigPeek.applyBoolean(values, "force_hide", forceHide);
        ConfigPeek.applyInt(values, "hide_after_ticks", hideAfterTicks);
    }

    public static boolean isActive() {
        return active && FeatureRegistry.liveActive("wtc_resource_hud");
    }

    public static boolean hideWhenEmpty() {
        return read(hideWhenEmpty, true);
    }

    public static boolean hideWhenDead() {
        return read(hideWhenDead, true);
    }

    public static boolean hideInSpectator() {
        return read(hideInSpectator, true);
    }

    public static boolean hideInCreative() {
        return read(hideInCreative, false);
    }

    public static boolean forceHide() {
        return read(forceHide, false);
    }

    public static int hideAfterTicks() {
        return readInt(hideAfterTicks, 200);
    }

    public static String section() {
        return "features.wtc_resource_hud";
    }

    private static int readInt(ForgeConfigSpec.IntValue value, int fallback) {
        if (value == null) {
            return fallback;
        } else {
            try {
                return (Integer)value.get();
            } catch (IllegalStateException var3) {
                return fallback;
            }
        }
    }

    private static boolean read(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        if (value == null) {
            return fallback;
        } else {
            try {
                return (Boolean)value.get();
            } catch (IllegalStateException var3) {
                return fallback;
            }
        }
    }
}
