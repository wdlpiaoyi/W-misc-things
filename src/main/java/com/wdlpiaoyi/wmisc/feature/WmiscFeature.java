package com.wdlpiaoyi.wmisc.feature;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;

/**
 * 一个独立功能模块。所有模块都：可单独开关、可声明前置模组、可以往自己的配置段里加选项。
 *
 * <p>新增模块的步骤：在 {@code feature.impl} 下实现本接口，然后在 {@link Features} 里注册。
 * 配置段和状态日志会自动生成。</p>
 */
public interface WmiscFeature {
    /** 模块 id，同时是配置段名（[features.&lt;id&gt;]）和日志标签。 */
    String id();

    /** 一句话说明，出现在配置注释和启动日志里。 */
    default String summary() {
        return "";
    }

    /**
     * 运行期重读配置（{@code /wmisc reload}）后调用：传入本模块配置段的原始键值。
     *
     * <p>模块自己把值同步到内存里的字段，让改动立即生效；不要在这里重建 ForgeConfigSpec。</p>
     */
    default void onConfigReload(Map<String, String> values) {
    }

    /** 配置默认值。 */
    default boolean defaultEnabled() {
        return true;
    }

    /** 是否已经写完。骨架阶段先注册、还没实现的模块返回 false，启动日志会标「实现待补」。 */
    default boolean implemented() {
        return true;
    }

    /** 需要同时安装的模组 id；缺失时模块自动跳过。 */
    default List<String> requiredMods() {
        return List.of();
    }

    /** 往本模块的配置段里追加选项（enabled 已经由框架生成）。 */
    default void defineOptions(ForgeConfigSpec.Builder builder) {
    }

    /**
     * mod 构造期调用（Forge 配置还没加载）。
     *
     * <p>需要「比 common setup 更早」的模块用它，比如要在其它 mod 的事件之前摘 Mixin。
     * 框架此时无法判断配置开关，实现方自己用 {@code ConfigPeek} 读文件判断。</p>
     */
    default void onConstruct() {
    }

    /** 通用初始化阶段：只在模块启用且前置齐全时调用。 */
    default void onCommonSetup() {
    }

    /** 客户端初始化阶段：只在模块启用且前置齐全时调用。 */
    default void onClientSetup() {
    }
}
