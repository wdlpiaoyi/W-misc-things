package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.compat.wtc.WtcBarVisibility;
import com.wdlpiaoyi.wmisc.compat.wtc.WtcHudScreen;
import com.wdlpiaoyi.wmisc.feature.impl.WtcResourceHudFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
    targets = {"com.inolia_zaicek.wizard_terra_cuiros.Mechanics.AdrenalineRageClient$ClientEvents"},
    remap = false
)
public abstract class WtcResourceBarMixin {
    @Inject(
        method = {"drawBar"},
        at = {@At("HEAD")},
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void wmisc$hideWhenNotRelevant(GuiGraphics graphics, int x, int y, double value, boolean full, ResourceLocation[] textures, int width, int height, double scale, int tick, boolean nano, CallbackInfo ci) {
        if (WtcResourceHudFeature.isActive()) {
            if (WtcHudScreen.isOpen()) {
                ci.cancel();
            } else {
                if (WtcBarVisibility.shouldHide(textures, value)) {
                    ci.cancel();
                }

            }
        }
    }
}
