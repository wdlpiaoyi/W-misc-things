package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.feature.impl.NoNarratorFeature;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({KeyboardHandler.class})
public abstract class NoNarratorHotkeyMixin {
    @Redirect(
        method = {"keyPress"},
        at = @At(
    value = "INVOKE",
    target = "Lnet/minecraft/client/GameNarrator;isActive()Z"
)
    )
    private boolean wmisc$blockNarratorHotkey(GameNarrator narrator) {
        return NoNarratorFeature.isActive() && NoNarratorFeature.blockHotkey() ? false : narrator.isActive();
    }
}
