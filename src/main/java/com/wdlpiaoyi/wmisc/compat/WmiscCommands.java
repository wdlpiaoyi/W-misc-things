package com.wdlpiaoyi.wmisc.compat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.wtc.WtcHud;
import com.wdlpiaoyi.wmisc.compat.wtc.WtcHudScreen;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.impl.EnigmaticSoulRecoveryFeature;
import com.wdlpiaoyi.wmisc.feature.impl.WtcResourceHudFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class WmiscCommands {
    private WmiscCommands() {
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("wmisc").then(Commands.literal("reload").executes(WmiscCommands::reload))).then(Commands.literal("wtc_resource_hud").executes(WmiscCommands::openWtcHudEditor)));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();

        try {
            FeatureRegistry.reloadOptionsFromFile();
        } catch (RuntimeException e) {
            Wmisc.LOG.warn("[w_misc] 重读配置失败，沿用内存里的旧值", e);
            feedback(minecraft, Component.translatable("wmisc.command.reload_failed").withStyle(ChatFormatting.RED));
            return 0;
        }

        feedback(minecraft, Component.translatable("wmisc.command.reloaded").withStyle(ChatFormatting.GREEN));
        Wmisc.LOG.info("[w_misc] 重载后生效值：灵魂水晶回复间隔 {} tick；WTC 静默 {} tick", EnigmaticSoulRecoveryFeature.intervalTicks(), WtcResourceHudFeature.hideAfterTicks());
        return 1;
    }

    private static int openWtcHudEditor(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        } else if (!WtcResourceHudFeature.isActive()) {
            feedback(minecraft, Component.translatable("wmisc.wtc_hud.feature_off").withStyle(ChatFormatting.RED));
            return 0;
        } else if (!WtcHud.available()) {
            feedback(minecraft, Component.translatable("wmisc.wtc_hud.unavailable").withStyle(ChatFormatting.RED));
            return 0;
        } else {
            WtcHudScreen.open();
            return 1;
        }
    }

    private static void feedback(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }

    }
}
