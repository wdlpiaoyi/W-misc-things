package com.wdlpiaoyi.wmisc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
    targets = {"dev.xkmc.l2modularblock.DelegateBlockImpl", "dev.xkmc.l2modularblock.DelegateEntityBlockImpl"},
    remap = false
)
public abstract class L2ModularBlockFinalMixin {
}
