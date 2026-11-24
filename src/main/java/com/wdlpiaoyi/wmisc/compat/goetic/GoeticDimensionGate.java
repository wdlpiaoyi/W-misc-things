package com.wdlpiaoyi.wmisc.compat.goetic;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.impl.GoeticGgtuDimensionFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class GoeticDimensionGate {
    public static final String GOETIC_MODID = "goeticlegacy";
    public static final String GGTU_MODID = "resolutionscaler";
    private static final ResourceLocation PERSONAL_COURT = ResourceLocation.fromNamespaceAndPath("goeticlegacy", "personal_court");
    private static final ResourceLocation MIND_COURT = ResourceLocation.fromNamespaceAndPath("goeticlegacy", "mind_court");
    private static volatile boolean frameGenSuppressed;
    private static volatile boolean upscalingSuppressed;
    private static volatile boolean errorLogged;

    private GoeticDimensionGate() {
    }

    public static boolean inGoeticDimension() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.level != null) {
                ResourceLocation dimension = minecraft.level.dimension().location();
                return dimension.equals(PERSONAL_COURT) || dimension.equals(MIND_COURT);
            } else {
                return false;
            }
        } catch (Throwable t) {
            logErrorOnce(t);
            return false;
        }
    }

    public static boolean suppressFrameGeneration() {
        boolean suppress = active() && GoeticGgtuDimensionFeature.disableFrameGen() && inGoeticDimension();
        if (suppress != frameGenSuppressed) {
            frameGenSuppressed = suppress;
            logTransition("帧生成", suppress);
        }

        return suppress;
    }

    public static boolean suppressUpscaling() {
        boolean suppress = active() && GoeticGgtuDimensionFeature.disableUpscaling() && inGoeticDimension();
        if (suppress != upscalingSuppressed) {
            upscalingSuppressed = suppress;
            logTransition("超分辨率", suppress);
        }

        return suppress;
    }

    private static boolean active() {
        try {
            return GoeticGgtuDimensionFeature.isActive();
        } catch (Throwable t) {
            logErrorOnce(t);
            return false;
        }
    }

    private static void logTransition(String what, boolean suppressed) {
        if (suppressed) {
            Wmisc.LOG.info("[w_misc] Goetic/GGTU：已暂停 {}（维度 {}）", what, dimensionName());
        } else {
            Wmisc.LOG.info("[w_misc] Goetic/GGTU：已恢复 {}（维度 {}）", what, dimensionName());
        }

    }

    private static String dimensionName() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.level != null ? minecraft.level.dimension().location().toString() : "?";
    }

    private static void logErrorOnce(Throwable t) {
        if (!errorLogged) {
            errorLogged = true;
            Wmisc.LOG.warn("[w_misc] Goetic/GGTU 维度闸门判定失败，维持 GGTU 原行为：{}", t.toString());
        }

    }
}
