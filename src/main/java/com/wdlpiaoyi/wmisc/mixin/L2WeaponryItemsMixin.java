package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.deadlock.DeadlockGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * l2weaponry 的物品注册入口 {@code LWItems.register()}：进入前取共享锁、返回时释放。
 *
 * <p>死锁环的另一端：{@code L2Weaponry.<init> → LWItems.<clinit> → CompatDispatch →
 * DragonCompat → IAFProxyCE → IafItems.<clinit>}。锁住 {@code register()} 就覆盖了这段危险区，
 * 而且不会采用构造函数注入（Mixin 0.8.5 不允许在构造函数里用 INVOKE 注入点）。</p>
 */
@Pseudo
@Mixin(targets = "dev.xkmc.l2weaponry.init.registrate.LWItems", remap = false)
public abstract class L2WeaponryItemsMixin {
    @Inject(method = "register", at = @At("HEAD"), remap = false, require = 0)
    private static void wmisc$acquire(CallbackInfo ci) {
        DeadlockGuard.acquire(DeadlockGuard.L2WEAPONRY);
    }

    @Inject(method = "register", at = @At("RETURN"), remap = false, require = 0)
    private static void wmisc$release(CallbackInfo ci) {
        DeadlockGuard.release(DeadlockGuard.L2WEAPONRY);
    }
}
