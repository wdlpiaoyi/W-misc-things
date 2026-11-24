package com.wdlpiaoyi.wmisc.compat.apprenticecodex;

import com.wdlpiaoyi.wmisc.Wmisc;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Apprentice's Codex 绑定武器专用附魔光的判定与贴图类型（反射桥）。
 *
 * <p>它的 {@code ItemRendererBoundWeaponGlintMixin}（priority 900）用两条 {@code @Redirect}
 * 把 {@code getFoilBuffer} / {@code getFoilBufferDirect} 换成
 * {@code VertexMultiConsumer.create(getBuffer(boundSpellWeaponGlint), getBuffer(renderType))}，
 * 也就是绑定武器额外写进一张自定义 glint 贴图缓冲。这两条指令同时被 Saint's Dragons 的残影
 * {@code @Redirect}（priority 1000）占据，所以它原本是被整个跳过的。现在由本模组接管并合并
 * （见 {@code ItemRendererOverrides#foilBuffer}），这里保留它需要的 5 个入口。</p>
 */
public final class BoundWeaponGlint {
    private static MethodHandle isBoundBow;
    private static MethodHandle isBoundSword;
    private static MethodHandle buffersRegistered;
    private static MethodHandle glint;
    private static MethodHandle glintDirect;
    private static boolean resolved;
    private static boolean failureLogged;

    private BoundWeaponGlint() {
    }

    public static boolean isBoundWeapon(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (!resolved) {
            resolve();
        }
        return invokeBool(isBoundBow, stack) || invokeBool(isBoundSword, stack);
    }

    public static boolean buffersRegistered() {
        if (!resolved) {
            resolve();
        }
        return invokeBool(buffersRegistered, null);
    }

    /** {@code direct = true} 对应 {@code getFoilBufferDirect} 用的那张（带 direct 后缀）。 */
    public static RenderType glintType(boolean direct) {
        if (!resolved) {
            resolve();
        }
        MethodHandle handle = direct ? glintDirect : glint;
        if (handle == null) {
            return null;
        }
        try {
            return (RenderType) handle.invokeExact();
        } catch (Throwable t) {
            if (direct) {
                glintDirect = null;
            } else {
                glint = null;
            }
            logFailure(t);
            return null;
        }
    }

    private static boolean invokeBool(MethodHandle handle, ItemStack stack) {
        if (handle == null) {
            return false;
        }
        try {
            return stack == null ? (boolean) handle.invokeExact() : (boolean) handle.invokeExact(stack);
        } catch (Throwable t) {
            logFailure(t);
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> bow = Class.forName("jp.aquafactory.apprenticecodex.item.boundweapon.BoundBowItem");
            isBoundBow = MethodHandles.lookup().unreflect(bow.getMethod("isBoundBow", ItemStack.class));

            Class<?> sword = Class.forName("jp.aquafactory.apprenticecodex.item.boundweapon.BoundSwordItem");
            isBoundSword = MethodHandles.lookup().unreflect(sword.getMethod("isBoundSword", ItemStack.class));

            Class<?> types = Class.forName("jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes");
            Method registered = types.getMethod("areBoundSpellWeaponGlintBuffersRegistered");
            buffersRegistered = MethodHandles.lookup().unreflect(registered);
            glint = MethodHandles.lookup().unreflect(types.getMethod("boundSpellWeaponGlint"));
            glintDirect = MethodHandles.lookup().unreflect(types.getMethod("boundSpellWeaponGlintDirect"));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            logFailure(e);
        }
    }

    private static void logFailure(Throwable t) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        Wmisc.LOG.warn("[w_misc] 接不上 Apprentice's Codex 的绑定武器附魔光，合并退化为原版 getFoilBuffer: {}", t.toString());
    }
}