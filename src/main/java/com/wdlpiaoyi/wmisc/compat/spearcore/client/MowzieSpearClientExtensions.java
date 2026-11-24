package com.wdlpiaoyi.wmisc.compat.spearcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.spearcore.MowzieSpearCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class MowzieSpearClientExtensions implements IClientItemExtensions {
    private static final String DELEGATE_CLASS = "net.minecraft.spearcore.client.SpearClientExtensions";
    private static volatile IClientItemExtensions delegate;
    private static volatile boolean delegateFailed;

    public static IClientItemExtensions create() {
        return new MowzieSpearClientExtensions();
    }

    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
        if (!MowzieSpearCompat.isActive()) {
            return false;
        } else {
            IClientItemExtensions extension = spearExtension();
            return extension != null && extension.applyForgeHandTransform(poseStack, player, arm, itemInHand, partialTick, equipProcess, swingProcess);
        }
    }

    private static IClientItemExtensions spearExtension() {
        IClientItemExtensions cached = delegate;
        if (cached == null && !delegateFailed) {
            synchronized(MowzieSpearClientExtensions.class) {
                if (delegate == null && !delegateFailed) {
                    try {
                        Object instance = Class.forName("net.minecraft.spearcore.client.SpearClientExtensions").getDeclaredConstructor().newInstance();
                        delegate = (IClientItemExtensions)instance;
                    } catch (Throwable t) {
                        delegateFailed = true;
                        Wmisc.LOG.warn("[w_misc] 解析 SpearCore 的第一人称长矛姿势失败，改用原版手部姿势", t);
                    }

                    return delegate;
                } else {
                    return delegate;
                }
            }
        } else {
            return cached;
        }
    }
}
