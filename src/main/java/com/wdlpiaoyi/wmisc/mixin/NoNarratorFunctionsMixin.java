package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.feature.impl.NoNarratorFeature;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.NarratorStatus;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameNarrator.class})
public abstract class NoNarratorFunctionsMixin {
    @Inject(
        method = {"checkStatus"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$skipInitDialog(boolean requested, CallbackInfo ci) {
        if (NoNarratorFeature.isActive()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"updateNarratorStatus"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$skipToggleToast(NarratorStatus status, CallbackInfo ci) {
        if (NoNarratorFeature.isActive() && NoNarratorFeature.disableSpeech()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"sayChat"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$silenceChat(Component message, CallbackInfo ci) {
        if (NoNarratorFeature.isActive() && NoNarratorFeature.disableSpeech()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"say"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$silenceSystem(Component message, CallbackInfo ci) {
        if (NoNarratorFeature.isActive() && NoNarratorFeature.disableSpeech()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"sayNow(Lnet/minecraft/network/chat/Component;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$silenceNow(Component message, CallbackInfo ci) {
        if (NoNarratorFeature.isActive() && NoNarratorFeature.disableSpeech()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"sayNow(Ljava/lang/String;)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$silenceNowRaw(String message, CallbackInfo ci) {
        if (NoNarratorFeature.isActive() && NoNarratorFeature.disableSpeech()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"clear"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void wmisc$silenceClear(CallbackInfo ci) {
        if (NoNarratorFeature.isActive() && NoNarratorFeature.disableSpeech()) {
            ci.cancel();
        }

    }

    @Inject(
        method = {"destroy"},
        at = {@At("HEAD")}
    )
    private void wmisc$noteDestroy(CallbackInfo ci) {
        NoNarratorFeature.noteShutdown();
    }
}
