package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.mowzie.SculptorTestCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
    targets = {"com.bobmowzie.mowziesmobs.server.entity.sculptor.EntitySculptor"},
    remap = false
)
public abstract class SculptorTestCheatMixin {
    @Inject(
        method = {"playerCheated"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private void wmisc$skipCheatFailure(CallbackInfo ci) {
        if (SculptorTestCompat.isActive()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"checkTestObstructedAtHeight"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private void wmisc$ignoreTestObstructions(int height, CallbackInfo ci) {
        if (SculptorTestCompat.isActive()) {
            ci.cancel();
        }

    }
}
