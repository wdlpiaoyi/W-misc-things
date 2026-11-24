package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.deadlock.DeadlockGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
    targets = {"dev.xkmc.l2weaponry.compat.CompatDispatch"},
    remap = false
)
public abstract class L2WeaponryCompatDispatchMixin {
    @Inject(
        method = {"register"},
        at = {@At("HEAD")},
        remap = false,
        require = 0
    )
    private static void wmisc$acquire(CallbackInfo ci) {
        DeadlockGuard.acquire("l2weaponry");
    }

    @Inject(
        method = {"register"},
        at = {@At("RETURN")},
        remap = false,
        require = 0
    )
    private static void wmisc$release(CallbackInfo ci) {
        DeadlockGuard.release("l2weaponry");
    }
}
