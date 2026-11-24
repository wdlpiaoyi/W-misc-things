package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.irons.AnvilJeiBudget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
    targets = {"io.redspace.ironsspellbooks.jei.JeiPlugin"},
    remap = false
)
public abstract class IronsAnvilJeiRegistrationMixin {
    @Inject(
        method = {"registerRecipes"},
        at = {@At("RETURN")},
        remap = false,
        require = 0
    )
    private void wmisc$logSummary(CallbackInfo ci) {
        AnvilJeiBudget.logSummary();
    }
}
