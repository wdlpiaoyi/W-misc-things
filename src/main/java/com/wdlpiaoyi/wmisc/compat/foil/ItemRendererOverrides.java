package com.wdlpiaoyi.wmisc.compat.foil;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.apprenticecodex.BoundWeaponGlint;
import com.wdlpiaoyi.wmisc.compat.saintsdragons.BloodTempestAfterimage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 复刻 Saint's Dragons 血暴残影在 {@code ItemRenderer#render} 里的三条注入，并与
 * Apprentice's Codex 的绑定武器 glint 合并。
 *
 * <p>为什么不直接抢 {@code hasFoil}：carbon 的 {@code imo$foil} 是 {@code require = 1} 的
 * {@code @Redirect}，只要它那条指令被任何人抢走，carbon 就会抛
 * {@code InjectionError: (0/1) succeeded} 让游戏起不来（整合包里实测就是这个崩法）。所以正确
 * 做法是反过来——<b>让 carbon 独占那条指令</b>，把圣龙那条 ItemRenderer mixin 从它的配置里摘掉
 * （见 {@code WmiscMixinPlugin#acceptTargets}），再由本类复刻圣龙剩下的三条注入：</p>
 * <ul>
 *   <li>{@code BakedModel#getRenderTypes}：残影期间换成半透明物品贴图；</li>
 *   <li>{@code hasAnimatedTexture}：残影期间当作「不是动态贴图」，绕开罗盘/时钟那套放大 + 附魔光；</li>
 *   <li>{@code getFoilBuffer}/{@code getFoilBufferDirect}：残影期间传 {@code glint = false}（等效于
 *       圣龙把 hasFoil 压成 false 的效果），并套上它的染色 consumer。</li>
 * </ul>
 *
 * <p>{@code getFoilBuffer*} 这两条指令原本同时被 Apprentice's Codex（900）和圣龙（1000）抢；
 * 现在由本模组统一持有，Codex 的部分按模块开关 {@code apprenticecodex_saintsdragons_glint}
 * 在运行期决定是否合并。</p>
 */
public final class ItemRendererOverrides {
    /** 模块 id，同时是配置段名 {@code [features.carbon_saintsdragons_foil]}。 */
    public static final String MODULE_FOIL = "carbon_saintsdragons_foil";
    /** 模块 id，同时是配置段名 {@code [features.apprenticecodex_saintsdragons_glint]}。 */
    public static final String MODULE_GLINT = "apprenticecodex_saintsdragons_glint";

    private static final AtomicBoolean GLINT_REPORTED = new AtomicBoolean();

    /** Codex 合并开关：由 {@code ApprenticeCodexSaintsdragonsGlintFeature} 在 setup/reload 时写入。 */
    private static volatile boolean glintModule;

    private ItemRendererOverrides() {
    }

    public static void setGlintModule(boolean enabled) {
        glintModule = enabled;
    }

    public static boolean glintModule() {
        return glintModule;
    }

    /** {@code BakedModel#getRenderTypes(ItemStack, boolean)} 的合并结果。 */
    @SuppressWarnings("deprecation")
    public static List<RenderType> renderTypes(BakedModel model, ItemStack stack, boolean fabulous) {
        return BloodTempestAfterimage.isActive()
                ? List.of(Sheets.translucentItemSheet())
                : model.getRenderTypes(stack, fabulous);
    }

    /** {@code ItemRenderer#hasAnimatedTexture(ItemStack)} 的合并结果：残影期间按「动态贴图」走会多出放大和附魔光。 */
    public static boolean hasAnimatedTexture(ItemStack stack) {
        return !BloodTempestAfterimage.isActive()
                && (stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK));
    }

    /** {@code getFoilBuffer}/{@code getFoilBufferDirect} 调用的合并结果。 */
    public static VertexConsumer foilBuffer(boolean direct, MultiBufferSource buffers, RenderType renderType,
                                            boolean isItem, boolean glint, ItemStack stack) {
        boolean effectiveGlint = glint && !BloodTempestAfterimage.isActive();
        if (glintModule && effectiveGlint && GLINT_REPORTED.compareAndSet(false, true)) {
            Wmisc.LOG.info("[{}] 已接管 ItemRenderer#render 的 getFoilBuffer/getFoilBufferDirect"
                    + "（Apprentice's Codex 绑定武器 glint + 圣龙血暴残影合并）", MODULE_GLINT);
        }

        VertexConsumer base = direct
                ? ItemRenderer.getFoilBufferDirect(buffers, renderType, isItem, effectiveGlint)
                : ItemRenderer.getFoilBuffer(buffers, renderType, isItem, effectiveGlint);

        // Apprentice's Codex 原始逻辑：glint && 绑定武器 && 缓冲已注册 时额外写一张 glint 贴图。
        if (glintModule && effectiveGlint
                && BoundWeaponGlint.isBoundWeapon(stack) && BoundWeaponGlint.buffersRegistered()) {
            RenderType glintType = BoundWeaponGlint.glintType(direct);
            if (glintType != null) {
                base = VertexMultiConsumer.create(buffers.getBuffer(glintType), buffers.getBuffer(renderType));
            }
        }

        // Saint's Dragons 原始逻辑：残影期间把最终 consumer 包一层染色；平时原样返回。
        return BloodTempestAfterimage.wrap(base);
    }
}