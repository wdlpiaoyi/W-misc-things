package com.wdlpiaoyi.wmisc.compat.enigmatic;

import com.wdlpiaoyi.wmisc.Wmisc;
import com.wdlpiaoyi.wmisc.feature.impl.EnigmaticSoulRecoveryFeature;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class SoulCrystalRecovery {
    private static final ResourceLocation SOUL_CRYSTAL_ID = new ResourceLocation("enigmaticlegacy", "soul_crystal");
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap();
    private static boolean resolved;
    private static Item crystalItem;
    private static Method getLostCrystals;
    private static Method setLostCrystals;
    private static boolean failureLogged;

    private SoulCrystalRecovery() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == Phase.END) {
            Player player = event.player;
            if (player != null && !player.level().isClientSide()) {
                long interval = (long)EnigmaticSoulRecoveryFeature.intervalTicks();
                if (interval > 0L) {
                    long now = player.level().getGameTime();
                    UUID id = player.getUUID();
                    Long last = (Long)LAST.get(id);
                    if (last == null) {
                        LAST.put(id, now);
                    } else if (now - last >= interval) {
                        LAST.put(id, now);
                        if (resolve()) {
                            try {
                                int lost = (Integer)getLostCrystals.invoke(crystalItem, player);
                                if (lost <= 0) {
                                    return;
                                }

                                int recovered = Math.min(EnigmaticSoulRecoveryFeature.amount(), lost);
                                setLostCrystals.invoke(crystalItem, player, lost - recovered);
                                Wmisc.LOG.info("[w_misc] 灵魂水晶回复 {} 片（剩余 {}/{}）", new Object[]{recovered, lost - recovered, lost});
                            } catch (RuntimeException | ReflectiveOperationException e) {
                                logFailureOnce(e);
                            }

                        }
                    }
                }
            }
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            LAST.remove(event.getEntity().getUUID());
        }

    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            Item item = (Item)ForgeRegistries.ITEMS.getValue(SOUL_CRYSTAL_ID);
            if (item == null) {
                logFailureOnce(new IllegalStateException("注册表里找不到 " + String.valueOf(SOUL_CRYSTAL_ID)));
                return false;
            } else {
                try {
                    Method getter = item.getClass().getMethod("getLostCrystals", Player.class);
                    Method setter = item.getClass().getMethod("setLostCrystals", Player.class, Integer.TYPE);
                    crystalItem = item;
                    getLostCrystals = getter;
                    setLostCrystals = setter;
                    Wmisc.LOG.info("[w_misc] 灵魂水晶回复：已接上 {}#setLostCrystals", item.getClass().getName());
                    return true;
                } catch (RuntimeException | NoSuchMethodException e) {
                    logFailureOnce(e);
                    return false;
                }
            }
        } else {
            return crystalItem != null && getLostCrystals != null && setLostCrystals != null;
        }
    }

    private static void logFailureOnce(Throwable error) {
        if (!failureLogged) {
            failureLogged = true;
            Wmisc.LOG.warn("[w_misc] 灵魂水晶回复不可用（Enigmatic Legacy 版本对不上？），本模块这次不做任何事", error);
        }
    }
}
