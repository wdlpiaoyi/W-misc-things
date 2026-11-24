package com.wdlpiaoyi.wmisc.compat.wtc;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class WtcHudScreen extends Screen {
    private static final int PADDING = 2;
    private static final double SCALE_STEP = 0.05;
    private static boolean open;
    private WtcHud.Bar selected;
    private WtcHud.Bar dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private final int[] snapshotX = new int[WtcHud.Bar.values().length];
    private final int[] snapshotY = new int[WtcHud.Bar.values().length];
    private final double[] snapshotScale = new double[WtcHud.Bar.values().length];
    private boolean handled;

    public WtcHudScreen() {
        super(Component.translatable("wmisc.wtc_hud.screen_title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new WtcHudScreen());
    }

    public static boolean isOpen() {
        return open;
    }

    protected void init() {
        open = true;
        this.handled = false;
        this.snapshot();
        int buttonWidth = 72;
        int buttonHeight = 20;
        int gap = 6;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int left = (this.width - totalWidth) / 2;
        int y = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("wmisc.wtc_hud.reset"), (button) -> this.reset()).bounds(left, y, buttonWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (button) -> this.cancel()).bounds(left + buttonWidth + gap, y, buttonWidth, buttonHeight).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> this.save()).bounds(left + (buttonWidth + gap) * 2, y, buttonWidth, buttonHeight).build());
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        for(WtcHud.Bar bar : WtcHud.Bar.values()) {
            this.drawBar(graphics, bar, bar == this.hoveredBar((double)mouseX, (double)mouseY) || bar == this.selected);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
        graphics.drawCenteredString(this.font, Component.translatable("wmisc.wtc_hud.hint").getString(), this.width / 2, this.height - 44, 16755200);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            WtcHud.Bar hit = this.hitTest(mouseX, mouseY);
            if (hit != null) {
                this.selected = hit;
                this.dragging = hit;
                this.dragOffsetX = mouseX - (double)WtcHud.x(hit);
                this.dragOffsetY = mouseY - (double)WtcHud.y(hit);
                return true;
            }

            this.selected = null;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging != null) {
            this.moveTo(this.dragging, mouseX - this.dragOffsetX, mouseY - this.dragOffsetY);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging != null) {
            this.dragging = null;
            return true;
        } else {
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        WtcHud.Bar bar = this.hoveredBar(mouseX, mouseY);
        if (bar == null) {
            bar = this.selected;
        }

        if (bar == null) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        } else {
            WtcHud.setScale(bar, WtcHud.scale(bar) + (delta > 0.0 ? 0.05 : -0.05));
            return true;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.selected != null) {
            int step = (modifiers & 1) != 0 ? 10 : 1;
            switch (keyCode) {
                case 262:
                    WtcHud.setX(this.selected, WtcHud.x(this.selected) + step);
                    return true;
                case 263:
                    WtcHud.setX(this.selected, WtcHud.x(this.selected) - step);
                    return true;
                case 264:
                    WtcHud.setY(this.selected, WtcHud.y(this.selected) + step);
                    return true;
                case 265:
                    WtcHud.setY(this.selected, WtcHud.y(this.selected) - step);
                    return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void onClose() {
        open = false;
        if (!this.handled) {
            this.restore();
        }

        super.onClose();
    }

    public void removed() {
        open = false;
        super.removed();
    }

    public boolean isPauseScreen() {
        return true;
    }

    private void save() {
        this.handled = true;
        WtcHud.save();
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.translatable("wmisc.wtc_hud.saved").withStyle(ChatFormatting.GREEN), false);
        }

        this.onClose();
    }

    private void cancel() {
        this.handled = true;
        this.restore();
        this.onClose();
    }

    private void reset() {
        WtcHud.resetToDefaults();
    }

    private void moveTo(WtcHud.Bar bar, double x, double y) {
        int maxX = Math.max(this.width, bar.width());
        int maxY = Math.max(this.height, bar.height());
        WtcHud.setX(bar, (int)Math.round(clamp(x, (double)(-bar.width()) * 0.5, (double)maxX)));
        WtcHud.setY(bar, (int)Math.round(clamp(y, (double)(-bar.height()) * 0.5, (double)maxY)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private WtcHud.Bar hoveredBar(double mouseX, double mouseY) {
        return this.hitTest(mouseX, mouseY);
    }

    private WtcHud.Bar hitTest(double mouseX, double mouseY) {
        for(WtcHud.Bar bar : new WtcHud.Bar[]{WtcHud.Bar.RAGE, WtcHud.Bar.ADRENALINE}) {
            double scale = WtcHud.scale(bar);
            double x = (double)WtcHud.x(bar);
            double y = (double)WtcHud.y(bar);
            double w = (double)bar.width() * scale;
            double h = (double)bar.height() * scale;
            if (mouseX >= x - 2.0 && mouseX <= x + w + 2.0 && mouseY >= y - 2.0 && mouseY <= y + h + 2.0) {
                return bar;
            }
        }

        return null;
    }

    private void drawBar(GuiGraphics graphics, WtcHud.Bar bar, boolean highlight) {
        int x = WtcHud.x(bar);
        int y = WtcHud.y(bar);
        double scale = WtcHud.scale(bar);
        int width = bar.width();
        int height = bar.height();
        boolean nano = this.minecraft != null && WtcHud.hasDraedonHeart(this.minecraft.player);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((double)x, (double)y, 0.0);
        pose.scale((float)scale, (float)scale, 1.0F);
        graphics.blit(new ResourceLocation(WtcHud.texture(bar, nano)), 0, 0, 0.0F, 0.0F, width, height, width, height);
        pose.popPose();
        int scaledWidth = (int)Math.round((double)width * scale);
        int scaledHeight = (int)Math.round((double)height * scale);
        int color = bar == this.selected ? -10496 : (highlight ? -8585294 : -12976278);
        outline(graphics, x, y, scaledWidth, scaledHeight, color);
        String label = Component.translatable(bar == WtcHud.Bar.ADRENALINE ? "wmisc.wtc_hud.adrenaline" : "wmisc.wtc_hud.rage").getString() + "  x=" + x + "  y=" + y + "  x" + String.format(Locale.ROOT, "%.2f", scale);
        graphics.drawString(this.font, label, x, Math.max(0, y - 10), color, true);
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x - 1, y - 1, x + width + 1, y, color);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        graphics.fill(x - 1, y, x, y + height, color);
        graphics.fill(x + width, y, x + width + 1, y + height, color);
    }

    private void snapshot() {
        for(WtcHud.Bar bar : WtcHud.Bar.values()) {
            this.snapshotX[bar.ordinal()] = WtcHud.x(bar);
            this.snapshotY[bar.ordinal()] = WtcHud.y(bar);
            this.snapshotScale[bar.ordinal()] = WtcHud.scale(bar);
        }

    }

    private void restore() {
        for(WtcHud.Bar bar : WtcHud.Bar.values()) {
            WtcHud.setX(bar, this.snapshotX[bar.ordinal()]);
            WtcHud.setY(bar, this.snapshotY[bar.ordinal()]);
            WtcHud.setScale(bar, this.snapshotScale[bar.ordinal()]);
        }

    }
}
