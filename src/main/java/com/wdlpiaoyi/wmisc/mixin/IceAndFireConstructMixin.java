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
 * iceandfire 主类的构造函数：在 super() 之后、方法体之前取共享锁，返回前释放。
 *
 * <p>注意不能用 {@code @At("HEAD")}：构造函数在 super() 之前注入会被 Mixin 拒绝
 * （"targetting a constructor before super()"）。这里用「super 调用之后」作为注入点，
 * 仍在任何 Iaf* 类被触碰之前。</p>
 *
 * <p>用 {@code targets} 字符串指向目标类，避免本模组在编译期/初始化期把 iceandfire 的类加载进来
 * （那本身就是死锁环的一部分）。构造失败时不会走到释放点，锁会泄漏；此时 Forge 直接终止加载，
 * 没有后续影响。</p>
 */
@Pseudo
@Mixin(targets = "com.iafenvoy.iceandfire.forge.IceAndFireForge", remap = false)
public abstract class IceAndFireConstructMixin {
    @Unique
    private ReentrantLock wmisc$constructLock;

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER),
            remap = false)
    private void wmisc$acquire(CallbackInfo ci) {
        this.wmisc$constructLock = DeadlockGuard.acquire(DeadlockGuard.ICEANDFIRE);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void wmisc$release(CallbackInfo ci) {
        ReentrantLock lock = this.wmisc$constructLock;
        this.wmisc$constructLock = null;
        DeadlockGuard.release(lock, DeadlockGuard.ICEANDFIRE);
    }
}
