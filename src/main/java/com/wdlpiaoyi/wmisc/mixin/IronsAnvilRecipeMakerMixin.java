package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.irons.AnvilJeiBudget;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = {"io.redspace.ironsspellbooks.jei.ArcaneAnvilRecipeMaker"},
    remap = false
)
public abstract class IronsAnvilRecipeMakerMixin {
    @Inject(
        method = {"getRecipes"},
        at = {@At("RETURN")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void wmisc$capRecipes(CallbackInfoReturnable<List<?>> cir) {
        List<?> original = (List)cir.getReturnValue();
        if (original != null) {
            List<?> capped = AnvilJeiBudget.capRecipes(original);
            if (capped != original) {
                cir.setReturnValue(capped);
            }

        }
    }
}
