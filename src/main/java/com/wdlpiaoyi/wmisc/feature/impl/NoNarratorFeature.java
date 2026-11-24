package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import com.wdlpiaoyi.wmisc.util.ConfigPeek;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

public final class NoNarratorFeature implements WmiscFeature {
    public static final String SECTION = "features.no_narrator";
    private static final boolean DEFAULT_BLOCK_LIBRARY = true;
    private static final boolean DEFAULT_DISABLE_SPEECH = true;
    private static final boolean DEFAULT_BLOCK_HOTKEY = true;
    private static final boolean DEFAULT_FORCE_OPTION_OFF = true;
    private static ForgeConfigSpec.BooleanValue blockLibrary;
    private static ForgeConfigSpec.BooleanValue disableSpeech;
    private static ForgeConfigSpec.BooleanValue blockHotkey;
    private static ForgeConfigSpec.BooleanValue forceOptionOff;
    private static volatile boolean active;
    private static volatile boolean shutdownLogged;
    private static volatile boolean libraryBlockedLogged;

    public String id() {
        return "no_narrator";
    }

    public String summary() {
        return "彻底关掉旁白：不加载 TTS 底层库、所有发声入口吞掉、Ctrl+B 失效";
    }

    public List<String> requiredMods() {
        return List.of();
    }

    public void defineOptions(ForgeConfigSpec.Builder builder) {
        blockLibrary = builder.comment("启动时不加载旁白底层库（text2speech 原生库），这项在游戏构造期生效").define("block_library", true);
        disableSpeech = builder.comment("所有发声入口（say/sayChat/sayNow/clear）与切换吐司都不执行").define("disable_speech", true);
        blockHotkey = builder.comment("禁用 Ctrl+B（循环 OFF/ALL/CHAT/SYSTEM）").define("block_hotkey", true);
        forceOptionOff = builder.comment("进游戏时把「旁白」选项压成 OFF（避免界面显示开启、实际没声音）").define("force_option_off", true);
    }

    public void onClientSetup() {
        active = true;
        if (forceOptionOff()) {
            forceOptionOffNow();
        }

        Wmisc.LOG.info("[w_misc] 旁白已禁用：不加载底层库 {}、吞掉发声 {}、屏蔽 Ctrl+B {}、选项压 OFF {}", new Object[]{blockLibrary(), disableSpeech(), blockHotkey(), forceOptionOff()});
    }

    public void onConfigReload(Map<String, String> values) {
        ConfigPeek.applyBoolean(values, "block_library", blockLibrary);
        ConfigPeek.applyBoolean(values, "disable_speech", disableSpeech);
        ConfigPeek.applyBoolean(values, "block_hotkey", blockHotkey);
        ConfigPeek.applyBoolean(values, "force_option_off", forceOptionOff);
    }

    public static boolean isActive() {
        return active && FeatureRegistry.liveActive("no_narrator");
    }

    public static boolean earlyEnabled() {
        return ConfigPeek.getBoolean("features.no_narrator", "enabled", true);
    }

    public static boolean earlyBlockLibrary() {
        return ConfigPeek.getBoolean("features.no_narrator", "block_library", true);
    }

    public static boolean blockLibrary() {
        return read(blockLibrary, true);
    }

    public static boolean disableSpeech() {
        return read(disableSpeech, true);
    }

    public static boolean blockHotkey() {
        return read(blockHotkey, true);
    }

    public static boolean forceOptionOff() {
        return read(forceOptionOff, true);
    }

    public static void noteShutdown() {
        if (active && !shutdownLogged) {
            shutdownLogged = true;
            Wmisc.LOG.info("[w_misc] 旁白：底层库从未加载，destroy 直接跳过");
        }

    }

    public static void noteLibraryBlocked() {
        if (!libraryBlockedLogged) {
            libraryBlockedLogged = true;
            Wmisc.LOG.info("[w_misc] 旁白：已拦下 Narrator.getNarrator()，text2speech 原生库不会加载");
        }

    }

    @OnlyIn(Dist.CLIENT)
    private static void forceOptionOffNow() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.options == null) {
                return;
            }

            NarratorStatus current = (NarratorStatus)minecraft.options.narrator().get();
            if (current != NarratorStatus.OFF) {
                minecraft.options.narrator().set(NarratorStatus.OFF);
                Wmisc.LOG.info("[w_misc] 旁白选项已从 {} 压回 OFF", current);
            }
        } catch (RuntimeException e) {
            Wmisc.LOG.debug("[w_misc] 压旁白选项失败：{}", e.toString());
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
