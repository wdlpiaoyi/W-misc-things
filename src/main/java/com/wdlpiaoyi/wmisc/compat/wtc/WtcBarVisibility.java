package com.wdlpiaoyi.wmisc.compat.wtc;

import com.wdlpiaoyi.wmisc.feature.impl.WtcResourceHudFeature;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public final class WtcBarVisibility {
    private static final double EMPTY_EPSILON = 0.001;
    private static final double VALUE_EPSILON = 1.0E-6;
    private static final Map<String, long[]> ACTIVITY = new ConcurrentHashMap();
    private static UUID trackedPlayer;

    private WtcBarVisibility() {
    }

    public static boolean shouldHide(ResourceLocation[] textures, double value) {
        if (!WtcResourceHudFeature.isActive()) {
            return false;
        } else if (WtcResourceHudFeature.forceHide()) {
            return true;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                reset();
                return true;
            } else {
                if (!player.getUUID().equals(trackedPlayer)) {
                    reset();
                    trackedPlayer = player.getUUID();
                }

                if (WtcResourceHudFeature.hideWhenDead() && player.isDeadOrDying()) {
                    return true;
                } else if (WtcResourceHudFeature.hideInSpectator() && player.isSpectator()) {
                    return true;
                } else if (WtcResourceHudFeature.hideInCreative() && player.isCreative()) {
                    return true;
                } else {
                    return WtcResourceHudFeature.hideWhenEmpty() && value <= 0.001 ? true : stagnant(keyOf(textures), value, player.level().getGameTime());
                }
            }
        }
    }

    private static boolean stagnant(String key, double value, long gameTime) {
        int window = WtcResourceHudFeature.hideAfterTicks();
        if (window <= 0) {
            return false;
        } else {
            long[] slot = (long[])ACTIVITY.computeIfAbsent(key, (ignored) -> new long[]{gameTime, Double.doubleToRawLongBits(value)});
            double previous = Double.longBitsToDouble(slot[1]);
            if (Math.abs(previous - value) > 1.0E-6) {
                slot[0] = gameTime;
                slot[1] = Double.doubleToRawLongBits(value);
            }

            return gameTime - slot[0] > (long)window;
        }
    }

    private static String keyOf(ResourceLocation[] textures) {
        if (textures != null && textures.length > 0 && textures[0] != null) {
            String path = textures[0].getPath();
            return path.startsWith("textures/gui/rage") ? "rage" : "adrenaline";
        } else {
            return "unknown";
        }
    }

    public static void reset() {
        ACTIVITY.clear();
        trackedPlayer = null;
    }
}
