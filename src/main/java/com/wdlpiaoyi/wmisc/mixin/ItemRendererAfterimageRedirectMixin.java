package com.wdlpiaoyi.wmisc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wdlpiaoyi.wmisc.compat.foil.ItemRendererOverrides;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * 复刻 Saint's Dragons 血暴残影在 {@code ItemRenderer#render} 里除 {@code hasFoil} 之外的三条
 * {@code @Redirect}，并合并 Apprentice's Codex 的绑定武器 glint。
 *
 * <p>前置：{@code WmiscMixinPlugin#acceptTargets} 已经把圣龙的
 * {@code client.BloodTempestAfterimageItemRendererMixin} 从 {@code saintsdragons.mixins.json} 里
 * 摘掉，所以这里不会再和它撞车；carbon 的 {@code hasFoil} 那条则原样留着，由 carbon 自己独占
 * （它 {@code require = 1}，被抢就会让游戏起不来）。</p>
 *
 * <p>{@code priority = 1500}：只为压过 Apprentice's Codex 那两条（900）和万一残留的同指令注入，
 * 并把它们的逻辑合并进来（Codex 的 {@code @Redirect} 自带 {@code require = 0}，被跳过不会崩）。</p>
 */
@Mixin(value = ItemRenderer.class, priority = 1500)
public abstract class ItemRendererAfterimageRedirectMixin {

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;"),
            require = 0)
    private List<RenderType> wmisc$afterimageRenderTypes(BakedModel model, ItemStack stack, boolean fabulous) {
        return ItemRendererOverrides.renderTypes(model, stack, fabulous);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;hasAnimatedTexture(Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private boolean wmisc$afterimageAnimatedTexture(ItemStack stack) {
        return ItemRendererOverrides.hasAnimatedTexture(stack);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 0)
    private VertexConsumer wmisc$mergeFoilBuffer(
            MultiBufferSource buffers, RenderType renderType, boolean isItem, boolean glint,
            ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
            MultiBufferSource modelBuffers, int combinedLight, int combinedOverlay, BakedModel model) {
        return ItemRendererOverrides.foilBuffer(false, buffers, renderType, isItem, glint, stack);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 0)
    private VertexConsumer wmisc$mergeFoilBufferDirect(
            MultiBufferSource buffers, RenderType renderType, boolean isItem, boolean glint,
            ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
            MultiBufferSource modelBuffers, int combinedLight, int combinedOverlay, BakedModel model) {
        return ItemRendererOverrides.foilBuffer(true, buffers, renderType, isItem, glint, stack);
    }
}