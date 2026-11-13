package com.wdlpiaoyi.wmisc;

import com.wdlpiaoyi.wmisc.config.WmiscConfig;
import com.wdlpiaoyi.wmisc.feature.FeatureRegistry;
import com.wdlpiaoyi.wmisc.feature.Features;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * wdlpiaoyi's Miscellaneous Things 入口。
 *
 * <p>骨架只做三件事：注册配置、把各功能模块初始化一遍、在日志里输出模块状态表。
 * 具体功能在 {@code com.wdlpiaoyi.wmisc.feature.impl} 下，一个模块一个类。</p>
 */
@Mod(Wmisc.MODID)
public final class Wmisc {
    public static final String MODID = "w_misc";
    public static final Logger LOG = LoggerFactory.getLogger(MODID);

    public Wmisc(FMLJavaModLoadingContext context) {
        // 先登记模块，配置构建时才知道要生成哪些 [features.<id>] 段。
        Features.init();
        WmiscConfig.register(context);

        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);

        // 构造期钩子：给需要抢在其它 mod 之前动作的模块（例如摘 Mixin）。
        FeatureRegistry.onConstructAll();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FeatureRegistry.activateAll();
            FeatureRegistry.logSummary();
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(FeatureRegistry::clientSetupAll);
    }
}

