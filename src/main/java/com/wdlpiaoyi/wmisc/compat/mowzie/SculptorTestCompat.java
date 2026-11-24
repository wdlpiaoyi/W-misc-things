package com.wdlpiaoyi.wmisc.compat.mowzie;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;

public final class SculptorTestCompat {
    public static final String FEATURE_ID = "mowzie_sculptor_no_anticheat";
    public static final String MOWZIE_MODID = "mowziesmobs";
    public static final String HOVER_KEY = "wmisc.mowzie.sculptor_hover";
    private static volatile boolean active;

    private SculptorTestCompat() {
    }

    public static void setActive(boolean on) {
        if (on != active) {
            active = on;
            Wmisc.LOG.info("[w_misc] 雕塑家试炼反作弊：{}", on ? "已关闭（放/破方块、传送、飞行、游泳、加速不再判失败）" : "已恢复");
        }
    }

    public static boolean isActive() {
        return active && FeatureRegistry.liveActive("mowzie_sculptor_no_anticheat");
    }

    public static Style hoverText() {
        MutableComponent text = Component.translatable("wmisc.mowzie.sculptor_hover");
        return Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, text));
    }
}
