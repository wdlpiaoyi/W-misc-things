package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.deadlock.DeadlockGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * iceandfire 的初始化入口 {@code IceAndFire.init()}：进入前取共享锁、返回时释放。
 *
 * <p>为什么锁在这里而不是构造函数：Mixin 0.8.5 对构造函数只允许 TAIL 注入
 * （INVOKE 注入点在构造函数里被限制为 METHODS_ONLY，会直接报
 * "Found @Inject targetting a constructor"）。而真正触发死锁环的调用
 * {@code IceAndFireForge.<init> → IceAndFire.init → IafBlocks.<clinit>} 就在这个方法里，
 * 所以锁住 {@code init()} 已经覆盖了危险区。</p>
 */
@Pseudo
@Mixin(targets = "com.iafenvoy.iceandfire.IceAndFire", remap = false)
public abstract class IceAndFireInitMixin {
    @Inject(method = "init", at = @At("HEAD"), remap = false, require = 0)
    private static void wmisc$acquire(CallbackInfo ci) {
        DeadlockGuard.acquire(DeadlockGuard.ICEANDFIRE);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = false, require = 0)
    private static void wmisc$release(CallbackInfo ci) {
        DeadlockGuard.release(DeadlockGuard.ICEANDFIRE);
    }
}
