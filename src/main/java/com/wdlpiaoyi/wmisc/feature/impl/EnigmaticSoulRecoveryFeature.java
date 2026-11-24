package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.enigmatic.SoulCrystalRecovery;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;

public final class EnigmaticSoulRecoveryFeature implements WmiscFeature {
    private static final String SECTION = "features.enigmatic_soul_recovery";
    private static final int DEFAULT_INTERVAL_TICKS = 6000;
    private static final int DEFAULT_AMOUNT = 1;
    private static ForgeConfigSpec.IntValue intervalTicks;
    private static ForgeConfigSpec.IntValue amount;
    private static volatile boolean active;

    public String id() {
        return "enigmatic_soul_recovery";
    }

    public String summary() {
        return "每 N tick（默认 6000 = 5 分钟）自动回复一点丢失的灵魂水晶";
    }

    public List<String> requiredMods() {
        return List.of("enigmaticlegacy");
    }

    public void defineOptions(ForgeConfigSpec.Builder builder) {
        intervalTicks = builder.comment(new String[]{"回复间隔，单位 tick（20 tick = 1 秒，6000 tick = 5 分钟；0 = 关掉自动回复）", "计时按世界 gameTime 走，暂停游戏/下线不会继续累计", "旧的 interval_minutes 已改成这个键；之前填 5 的，这里填 6000"}).defineInRange("interval_ticks", 6000, 0, 1728000);
        amount = builder.comment("每次回复几片（不会超过当前丢失数）").defineInRange("amount", 1, 1, 10);
    }

    public void onCommonSetup() {
        MinecraftForge.EVENT_BUS.addListener(SoulCrystalRecovery::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(SoulCrystalRecovery::onPlayerLogout);
        active = true;
        int ticks = intervalTicks();
        Wmisc.LOG.info("[w_misc] 灵魂水晶自动回复已启用：每 {} tick（约 {} 分钟）回复 {} 片", new Object[]{ticks, String.format(Locale.ROOT, "%.1f", (double)ticks / 1200.0), amount()});
    }

    public void onConfigReload(Map<String, String> values) {
        ConfigPeek.applyInt(values, "interval_ticks", intervalTicks);
        ConfigPeek.applyInt(values, "amount", amount);
    }

    public static boolean isActive() {
        return active && FeatureRegistry.liveActive("enigmatic_soul_recovery");
    }

    public static int intervalTicks() {
        return read(intervalTicks, 6000);
    }

    public static int amount() {
        return read(amount, 1);
    }

    public static String section() {
        return "features.enigmatic_soul_recovery";
    }

    private static int read(ForgeConfigSpec.IntValue value, int fallback) {
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
}
