package com.wdlpiaoyi.wmisc.compat.spellbooks;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 校验 {@code irons_spellbooks:upgrade_orb_type} 数据包里确实存在
 * {@code fantasy_ending:fantasy_power}。
 *
 * <p>只依赖原版 API（ResourceKey / RegistryAccess），不引用 irons_spellbooks 或 fantasy_ending 的类，
 * 所以这两个模组缺席时这段代码也不会出问题。</p>
 */
public final class UpgradeOrbEntryCheck {
    private static final Logger LOG = LoggerFactory.getLogger("w_misc/spellbooks");

    private static final ResourceKey<Registry<Object>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("irons_spellbooks:upgrade_orb_type"));

    private static final ResourceKey<Object> ENTRY_KEY =
            ResourceKey.create(REGISTRY_KEY, new ResourceLocation("fantasy_ending:fantasy_power"));

    private UpgradeOrbEntryCheck() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        try {
            boolean present = server.registryAccess().registry(REGISTRY_KEY)
                    .map(registry -> registry.containsKey(ENTRY_KEY))
                    .orElse(false);
            if (present) {
                LOG.info("[w_misc] upgrade_orb_type 校验通过：fantasy_ending:fantasy_power 已在数据包里");
            } else {
                LOG.error("[w_misc] upgrade_orb_type 校验失败：数据包里查不到 fantasy_ending:fantasy_power，"
                        + "irons_spellbooks 的奥术铁砧配方仍会报 Missing element（本模组自带的数据文件没生效？）");
            }
        } catch (Throwable t) {
            LOG.warn("[w_misc] upgrade_orb_type 校验异常，跳过：{}", t.toString());
        }
    }
}

