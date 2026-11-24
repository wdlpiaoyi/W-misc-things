package com.wdlpiaoyi.wmisc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * fantasy_ending 2.7.x × geckolib 4.8+ 的加载期占位字段。
 *
 * <p>fantasy_ending（内部包名 {@code com.mega.uom}）的
 * {@code com.mega.uom.mixin.geckolib.GeoModelAccessor} 用 {@code @Accessor("animTime")}
 * 读写 geckolib {@code GeoModel#animTime}（double）。geckolib 4.8 删掉了这个字段，
 * 而 uom 的 mixin 配置是 {@code required=true}，accessor 在 GeoModel 类加载时找不到字段会直接
 * 抛 InvalidMixinException，把启动打断。</p>
 *
 * <p>这里只做一件事：把同名同类型的字段塞回 GeoModel，让 uom 的 accessor 能解析成功。
 * 这个字段没有任何写入点（本模组不读也不写），所以 fantasy_ending 里依赖「动画时间」推进的
 * 逻辑会静默失效 —— 这是版本兼容的代价，换来两边都能正常加载。</p>
 *
 * <p>顺序：本模组 mixin 配置用默认优先级 1000，uom 的配置是 {@code Integer.MAX_VALUE}，
 * 配置按优先级升序 prepare，所以本字段会先合入 GeoModel，之后 uom 的 accessor 才去找它。</p>
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.model.GeoModel", remap = false)
public abstract class FantasyGeckolibAnimTimeMixin {

    /** 仅供 fantasy_ending 的 @Accessor("animTime") 解析，本模组不读也不写。 */
    @SuppressWarnings("unused")
    private double animTime;
}
