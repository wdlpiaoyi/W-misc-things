package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.spearcore.MowzieSpearCompat;
import net.minecraft.spearcore.item.SpearLike;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = {"net.minecraft.spearcore.init.SpearLikeSupport"},
    remap = false
)
public abstract class MowzieSpearLikeSupportMixin {
    @Inject(
        method = {"of"},
        at = {@At("RETURN")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void wmisc$treatMowzieSpearAsSpearLike(ItemStack stack, CallbackInfoReturnable<SpearLike> cir) {
        if (cir.getReturnValue() == null) {
            Object spear = MowzieSpearCompat.spearLikeFor(stack);
            if (spear != null) {
                cir.setReturnValue((SpearLike)spear);
            }

        }
    }
}
