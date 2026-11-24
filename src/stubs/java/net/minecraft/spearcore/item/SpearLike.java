package net.minecraft.spearcore.item;

/**
 * spear-core 的 {@code SpearLike} 编译期空壳。
 *
 * <p>只有 {@code MowzieSpearLikeSupportMixin} 需要这个类型出现在编译期类路径上（作为
 * {@code CallbackInfoReturnable<SpearLike>} 的类型参数和转型目标），运行期的真实接口由
 * spear-core 模组提供。spear-core 不在 maven 上，所以按本仓库惯例放进 {@code src/stubs}
 * （只进 compileOnly，不进 jar）。</p>
 *
 * <p>本接口刻意不声明任何方法：代码里只用到它的类型，方法调用一律走
 * {@code SpearCoreBridge} 的反射。</p>
 */
public interface SpearLike {
}
