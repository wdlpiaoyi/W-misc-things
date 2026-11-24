package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.spearcore.MowzieSpearCompat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ForgeHooksClient.class})
public abstract class MowzieSpearReequipMixin {
    @Inject(
        method = {"shouldCauseReequipAnimation"},
        at = {@At("RETURN")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void wmisc$keepSpearInHand(ItemStack from, ItemStack to, int slot, CallbackInfoReturnable<Boolean> cir) {
        if ((Boolean)cir.getReturnValue() && !from.isEmpty() && !to.isEmpty()) {
            if (MowzieSpearCompat.isSpear(from) || MowzieSpearCompat.isSpear(to)) {
                cir.setReturnValue(false);
            }

        }
    }
}
