package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.mowzie.SculptorTestCompat;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = {"com.bobmowzie.mowziesmobs.client.gui.GuiSculptorTrade"},
    remap = false
)
public abstract class SculptorTradeHoverMixin {
    @Inject(
        method = {"getHoverText"},
        at = {@At("RETURN")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private void wmisc$replaceBeginTestHint(CallbackInfoReturnable<Style> cir) {
        if (SculptorTestCompat.isActive()) {
            cir.setReturnValue(SculptorTestCompat.hoverText());
        }

    }
}
