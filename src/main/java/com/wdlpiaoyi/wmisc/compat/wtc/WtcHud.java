package com.wdlpiaoyi.wmisc.compat.wtc;

import com.wdlpiaoyi.wmisc.Wmisc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;

public final class WtcHud {
    public static final int ADRENALINE_WIDTH = 154;
    public static final int ADRENALINE_HEIGHT = 68;
    public static final int RAGE_WIDTH = 152;
    public static final int RAGE_HEIGHT = 38;
    public static final double MIN_SCALE = 0.05;
    public static final double MAX_SCALE = 4.0;
    public static final String WTC_MODID = "wizard_terra_cuiros";
    private static boolean resolved;
    private static ForgeConfigSpec clientSpec;
    private static ForgeConfigSpec.IntValue adrenalineX;
    private static ForgeConfigSpec.IntValue adrenalineY;
    private static ForgeConfigSpec.DoubleValue adrenalineScale;
    private static ForgeConfigSpec.IntValue rageX;
    private static ForgeConfigSpec.IntValue rageY;
    private static ForgeConfigSpec.DoubleValue rageScale;
    private static Method hasDraedonHeart;
    private static boolean failureLogged;

    private WtcHud() {
    }

    public static synchronized boolean available() {
        resolve();
        return clientSpec != null;
    }

    public static int x(Bar bar) {
        ForgeConfigSpec.IntValue value = bar == WtcHud.Bar.ADRENALINE ? adrenalineX : rageX;
        return readInt(value, bar == WtcHud.Bar.ADRENALINE ? 152 : 93);
    }

    public static int y(Bar bar) {
        ForgeConfigSpec.IntValue value = bar == WtcHud.Bar.ADRENALINE ? adrenalineY : rageY;
        return readInt(value, bar == WtcHud.Bar.ADRENALINE ? -1 : 5);
    }

    public static double scale(Bar bar) {
        ForgeConfigSpec.DoubleValue value = bar == WtcHud.Bar.ADRENALINE ? adrenalineScale : rageScale;
        return readDouble(value, 0.5);
    }

    public static void setX(Bar bar, int x) {
        writeInt(bar == WtcHud.Bar.ADRENALINE ? adrenalineX : rageX, x);
    }

    public static void setY(Bar bar, int y) {
        writeInt(bar == WtcHud.Bar.ADRENALINE ? adrenalineY : rageY, y);
    }

    public static void setScale(Bar bar, double scale) {
        double clamped = Math.max(0.05, Math.min(4.0, scale));
        ForgeConfigSpec.DoubleValue value = bar == WtcHud.Bar.ADRENALINE ? adrenalineScale : rageScale;
        if (value != null) {
            try {
                value.set(clamped);
            } catch (IllegalStateException var7) {
            }

        }
    }

    public static void save() {
        if (clientSpec != null) {
            try {
                clientSpec.save();
            } catch (RuntimeException e) {
                Wmisc.LOG.warn("[w_misc] 保存 WTC 资源条位置失败（改动只在本次运行生效）", e);
            }

        }
    }

    public static void resetToDefaults() {
        setX(WtcHud.Bar.ADRENALINE, 152);
        setY(WtcHud.Bar.ADRENALINE, -1);
        setScale(WtcHud.Bar.ADRENALINE, 0.5);
        setX(WtcHud.Bar.RAGE, 93);
        setY(WtcHud.Bar.RAGE, 5);
        setScale(WtcHud.Bar.RAGE, 0.5);
    }

    public static boolean hasDraedonHeart(Player player) {
        resolve();
        if (hasDraedonHeart != null && player != null) {
            try {
                return (Boolean)hasDraedonHeart.invoke((Object)null, player);
            } catch (RuntimeException | ReflectiveOperationException var2) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static String texture(Bar bar, boolean nanomachines) {
        String key = bar == WtcHud.Bar.ADRENALINE && nanomachines ? "nanomachines" : bar.textureKey();
        return "wizard_terra_cuiros:textures/gui/" + key + "_00.png";
    }

    private static void resolve() {
        if (!resolved) {
            resolved = true;

            try {
                Class<?> config = Class.forName("com.inolia_zaicek.wizard_terra_cuiros.Config.WTCConfig");
                clientSpec = (ForgeConfigSpec)field(config, "CLIENT_SPEC");
                adrenalineX = (ForgeConfigSpec.IntValue)field(config, "adrenaline_x_client");
                adrenalineY = (ForgeConfigSpec.IntValue)field(config, "adrenaline_y_client");
                adrenalineScale = (ForgeConfigSpec.DoubleValue)field(config, "adrenaline_scale");
                rageX = (ForgeConfigSpec.IntValue)field(config, "rage_x_client");
                rageY = (ForgeConfigSpec.IntValue)field(config, "rage_y_client");
                rageScale = (ForgeConfigSpec.DoubleValue)field(config, "rage_scale");
                Class<?> rage = Class.forName("com.inolia_zaicek.wizard_terra_cuiros.Mechanics.AdrenalineRage");
                hasDraedonHeart = rage.getMethod("hasDraedonHeart", Player.class);
            } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
                if (!failureLogged) {
                    failureLogged = true;
                    Wmisc.LOG.warn("[w_misc] 接不上 Wizard Terra Curios 的资源条配置，位置编辑工具不可用", e);
                }
            }

        }
    }

    private static Object field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getField(name);
        return field.get((Object)null);
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

    private static double readDouble(ForgeConfigSpec.DoubleValue value, double fallback) {
        if (value == null) {
            return fallback;
        } else {
            try {
                return (Double)value.get();
            } catch (IllegalStateException var4) {
                return fallback;
            }
        }
    }

    private static void writeInt(ForgeConfigSpec.IntValue value, int target) {
        if (value != null) {
            try {
                value.set(target);
            } catch (IllegalStateException var3) {
            }

        }
    }

    public static enum Bar {
        ADRENALINE("adrenaline", 154, 68),
        RAGE("rage", 152, 38);

        private final String textureKey;
        private final int width;
        private final int height;

        private Bar(String textureKey, int width, int height) {
            this.textureKey = textureKey;
            this.width = width;
            this.height = height;
        }

        public String textureKey() {
            return this.textureKey;
        }

        public int width() {
            return this.width;
        }

        public int height() {
            return this.height;
        }
    }
}
