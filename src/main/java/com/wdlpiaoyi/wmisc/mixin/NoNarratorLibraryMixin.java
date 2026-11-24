package com.wdlpiaoyi.wmisc.mixin;

import com.mojang.text2speech.Narrator;
import com.wdlpiaoyi.wmisc.feature.impl.NoNarratorFeature;
import net.minecraft.client.GameNarrator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
    value = {GameNarrator.class},
    remap = false
)
public abstract class NoNarratorLibraryMixin {
    @Redirect(
        method = {"<init>"},
        at = @At(
    value = "INVOKE",
    target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;"
)
    )
    private Narrator wmisc$skipNarratorLibrary() {
        if (NoNarratorFeature.earlyEnabled() && NoNarratorFeature.earlyBlockLibrary()) {
            NoNarratorFeature.noteLibraryBlocked();
            return Narrator.EMPTY;
        } else {
            return Narrator.getNarrator();
        }
    }
}
