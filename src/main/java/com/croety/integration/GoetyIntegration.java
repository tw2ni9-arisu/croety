package com.croety.integration;

import com.Polarice3.Goety.Goety;
import com.Polarice3.Goety.common.items.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

/**
 * 编译期和运行期确认 Goety 3.2.0 API 可用。
 * <p>
 * Goety 没有公共 Maven；官方发行 JAR 经 hash 校验后暂存在 {@code ./libs/maven}。
 */
public final class GoetyIntegration
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Goety 的 mod id。 */
    public static final String MOD_ID = Goety.MOD_ID;

    private GoetyIntegration()
    {
    }

    /** 创建 Goety 命名空间中的资源位置。 */
    public static ResourceLocation id(String path)
    {
        return Goety.location(path);
    }

    /**
     * Goety 完成内容注册后再调用。
     */
    public static void logEnvironment()
    {
        Item totem = ModItems.TOTEM_OF_SOULS.get();
        LOGGER.info("[{}] Goety integration ready: modId={}, totemOfSouls={} ({})",
                "croety", MOD_ID, id("totem_of_souls"), totem.getDescriptionId());
    }
}
