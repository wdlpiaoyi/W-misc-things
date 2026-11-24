package com.wdlpiaoyi.wmisc.compat.saintsdragons;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wdlpiaoyi.wmisc.Wmisc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Saint's Dragons 血暴残影（Blood Tempest afterimage）的渲染状态（反射桥）。
 *
 * <p>它的 {@code BloodTempestAfterimageItemRendererMixin} 原本用三条 {@code @Redirect} 做两件事：
 * 残影期间让 {@code hasFoil} 为 false、把 {@code getFoilBuffer*} 的返回值包一层变色 consumer；
 * 再加上 `getRenderTypes` 换成半透明贴图。后两条不冲突，照旧由它自己生效；只有
 * {@code hasFoil} 和 carbon / Apprentice's Codex 撞车，所以本模组只接管撞车的那几条指令，
 * 这里提供它原本的判断与包装入口。</p>
 */
public final class BloodTempestAfterimage {
    private static MethodHandle isActive;
    private static MethodHandle wrapConsumer;
    private static boolean resolved;
    private static boolean failureLogged;

    private BloodTempestAfterimage() {
    }

    public static boolean isActive() {
        if (!resolved) {
            resolve();
        }
        MethodHandle handle = isActive;
        if (handle == null) {
            return false;
        }
        try {
            return (boolean) handle.invokeExact();
        } catch (Throwable t) {
            isActive = null;
            logFailure(t);
            return false;
        }
    }

    /** 残影期间给顶点 consumer 套一层染色/透明度；平时原样返回。 */
    public static VertexConsumer wrap(VertexConsumer delegate) {
        if (!resolved) {
            resolve();
        }
        MethodHandle handle = wrapConsumer;
        if (handle == null) {
            return delegate;
        }
        try {
            return (VertexConsumer) handle.invokeExact(delegate);
        } catch (Throwable t) {
            wrapConsumer = null;
            logFailure(t);
            return delegate;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> context = Class.forName("com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext");
            isActive = MethodHandles.lookup().unreflect(context.getMethod("isActive"));

            Class<?> consumer = Class.forName("com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageVertexConsumer");
            Method wrap = consumer.getMethod("wrap", VertexConsumer.class);
            wrapConsumer = MethodHandles.lookup().unreflect(wrap);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            logFailure(e);
        }
    }

    private static void logFailure(Throwable t) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        Wmisc.LOG.warn("[w_misc] 接不上 Saint's Dragons 的血暴残影状态，相关合并退化为「不抑制/不染色」: {}", t.toString());
    }
}