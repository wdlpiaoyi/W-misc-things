package com.wdlpiaoyi.wmisc.mixin;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.compat.content.ContentFilterRules;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 摘掉 Immersive Engineering 的 6 个 pride 着色器（acepride / bipride / enbypride / gaypride /
 * lesbianpride / transpride）。
 *
 * <p>IE 的着色器是代码注册的（{@code IEShaders.commonConstruction()} → {@code ShaderRegistry.registerShader}），
 * 没有数据文件可删，所以在 {@code commonConstruction()} 返回后从 {@code ShaderRegistry.shaderRegistry}
 * 和 {@code chestLootShaders} 里把它们的 id 摘掉：物品上不再出现这些着色器，战利品也不会抽到。</p>
 *
 * <p>用反射访问 {@code ShaderRegistry} 的静态字段，避免编译期依赖 IE。</p>
 */
@Pseudo
@Mixin(targets = "blusunrize.immersiveengineering.common.util.IEShaders", remap = false)
public abstract class ImmersiveEngineeringShaderMixin {
    private static final String SHADER_REGISTRY = "blusunrize.immersiveengineering.api.shader.ShaderRegistry";

    @Inject(method = "commonConstruction", at = @At("TAIL"), remap = false, require = 0)
    private static void wmisc$dropPrideShaders(CallbackInfo ci) {
        if (!ContentFilterRules.isActive()) {
            return;
        }
        try {
            Class<?> registryClass = Class.forName(SHADER_REGISTRY);
            int removed = 0;
            Object map = registryClass.getField("shaderRegistry").get(null);
            if (map instanceof Map<?, ?> shaderMap) {
                for (Object key : new ArrayList<>(shaderMap.keySet())) {
                    if (key instanceof ResourceLocation id && ContentFilterRules.isBlockedIeShader(id.getPath())) {
                        shaderMap.remove(key);
                        removed++;
                    }
                }
            }
            Object loot = registryClass.getField("chestLootShaders").get(null);
            if (loot instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> entries = (List<Object>) list;
                entries.removeIf(entry -> entry instanceof ResourceLocation id
                        && ContentFilterRules.isBlockedIeShader(id.getPath()));
            }
            Wmisc.LOG.info("[w_misc] 已摘除 IE 的 {} 个 pride 着色器", removed);
        } catch (Throwable t) {
            Wmisc.LOG.warn("[w_misc] 摘除 IE pride 着色器失败（IE 版本变化？）：{}", t.toString());
        }
    }
}
