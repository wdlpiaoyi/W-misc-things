package com.wdlpiaoyi.wmisc.feature.impl;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.WmiscFeature;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 内容过滤：屏蔽模组里与玩法无关的政治宣传内容（目前针对整合包里确认存在的几处）。
 *
 * <p>分层实现，能改的尽量用数据/资源层，不硬改别人的 jar：</p>
 * <ol>
 *   <li><b>Malum 的 prideweave</b>：本模组自带同名 recipe / advancement 覆盖文件，
 *       用 {@code forge:false} 条件让它们全部失效（不可合成、不显示解锁提示）。
 *       物品本体仍存在（代码注册，删物品风险太高）。</li>
 *   <li><b>主菜单 splash</b>：本模组提供原版 {@code splashes.txt}，去掉第三方模组追加的宣传句。</li>
 *   <li><b>Quark 彩虹 Q 按钮</b>：{@link com.wdlpiaoyi.wmisc.mixin.QuarkQButtonMixin} 固定前景色。</li>
 *   <li><b>其余（Mekanism robit 皮肤、IE 着色器名称、Malum 贴图/名称、Quark LGBT 蜜蜂/日期提示）</b>：
 *       这些要改 JSON/贴图/语言文件，跨模组资源包顺序不可控，改为在实例侧用
 *       {@code moonlight-global-datapacks} 的 filter 数据包 + 资源包处理；
 *       详见 README 的“内容过滤”一节。</li>
 * </ol>
 *
 * <p>开关只控制启动日志与自检；随包提供的资源/数据文件是静态的，无法在运行时单独关掉单个文件。</p>
 */
public final class ContentFilterFeature implements WmiscFeature {
    @Override
    public String id() {
        return "content_filter";
    }

    @Override
    public String summary() {
        return "屏蔽模组注入的政治宣传内容（Malum prideweave、splash、Quark 彩虹按钮等）";
    }

    @Override
    public boolean implemented() {
        return true;
    }

    @Override
    public void onCommonSetup() {
        boolean malum = com.wdlpiaoyi.wmisc.compat.Compat.isModLoaded("malum");
        boolean quark = com.wdlpiaoyi.wmisc.compat.Compat.isModLoaded("quark");
        Wmisc.LOG.info("[w_misc] 内容过滤已启用（Malum 覆盖：{}，Quark 按钮：{}）",
                malum ? "已随包提供" : "未安装 Malum", quark ? "已生效" : "未安装 Quark");
    }
}
