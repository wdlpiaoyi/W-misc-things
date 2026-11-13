package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.deadlock.DeadlockGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReentrantLock;

/**
 * l2weaponry 主类的构造函数：HEAD 取共享锁，RETURN 释放。
 *
 * <p>死锁环的另一端：{@code L2Weaponry.<init> → LWItems.<clinit> → CompatDispatch → DragonCompat →
 * IAFProxyCE → IafItems.<clinit>}。在 HEAD 处挡住它，就不会在 iceandfire 尚未初始化完 IafBlocks 时
 * 去抢 IafItems 的类初始化锁。</p>
 */
@Mixin(targets = "dev.xkmc.l2weaponry.init.L2Weaponry", remap = false)
public abstract class L2WeaponryConstructMixin {
    @Unique
    private ReentrantLock wmisc$constructLock;

    @Inject(method = "<init>", at = @At("HEAD"), remap = false)
    private void wmisc$acquire(CallbackInfo ci) {
        this.wmisc$constructLock = DeadlockGuard.acquire(DeadlockGuard.L2WEAPONRY);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void wmisc$release(CallbackInfo ci) {
        ReentrantLock lock = this.wmisc$constructLock;
        this.wmisc$constructLock = null;
        DeadlockGuard.release(lock, DeadlockGuard.L2WEAPONRY);
    }
}
