package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.goetic.GoeticDimensionGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = {"com.maxim.resolutionscaler.ResolutionScalerConfig"},
    remap = false
)
public abstract class GgtuGoeticDimensionMixin {
    @Inject(
        method = {"isFrameGenerationEnabled"},
        at = {@At("RETURN")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private void wmisc$gateFrameGeneration(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && GoeticDimensionGate.suppressFrameGeneration()) {
            cir.setReturnValue(false);
        }

    }

    @Inject(
        method = {"isUpscalingEnabled"},
        at = {@At("RETURN")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private void wmisc$gateUpscaling(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && GoeticDimensionGate.suppressUpscaling()) {
            cir.setReturnValue(false);
        }

    }
}
