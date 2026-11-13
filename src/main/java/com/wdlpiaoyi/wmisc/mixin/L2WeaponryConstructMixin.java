package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.deadlock.DeadlockGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReentrantLock;

/**
 * l2weaponry 主类的构造函数：在 super() 之后、方法体之前取共享锁，返回前释放。
 *
 * <p>死锁环的另一端：{@code L2Weaponry.<init> → LWItems.<clinit> → CompatDispatch → DragonCompat →
 * IAFProxyCE → IafItems.<clinit>}。在进入方法体之前挡住它，就不会在 iceandfire 尚未初始化完
 * IafBlocks 时去抢 IafItems 的类初始化锁。</p>
 *
 * <p>不能用 {@code @At("HEAD")}：构造函数在 super() 之前注入会被 Mixin 拒绝。</p>
 */
@Pseudo
@Mixin(targets = "dev.xkmc.l2weaponry.init.L2Weaponry", remap = false)
public abstract class L2WeaponryConstructMixin {
    @Unique
    private ReentrantLock wmisc$constructLock;

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER),
            remap = false)
    private void wmisc$acquire(CallbackInfo ci) {
        this.wmisc$constructLock = DeadlockGuard.acquire(DeadlockGuard.L2WEAPONRY);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void wmisc$release(CallbackInfo ci) {
        ReentrantLock lock = this.wmisc$constructLock;
        this.wmisc$constructLock = null;
        DeadlockGuard.release(lock, DeadlockGuard.L2WEAPONRY);
    }
}
