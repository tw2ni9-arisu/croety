package com.croety.integration;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * 编译期和运行期确认 Create 6.0.10 API 可用。
 * <p>
 * 使用 Create 官方 Maven 中的 1.21.1 slim 制品，并由 ModDevGradle 提供编译映射。
 */
public final class CreateIntegration
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Create 的 mod id。 */
    public static final String MOD_ID = Create.ID;

    private CreateIntegration()
    {
    }

    /** 创建 Create 命名空间中的资源位置。 */
    public static ResourceLocation id(String path)
    {
        return Create.asResource(path);
    }

    /**
     * Create 完成内容注册后再调用，避免在 Registrate 条目注册前读取。
     */
    public static void logEnvironment()
    {
        Block cogwheel = AllBlocks.COGWHEEL.get();
        LOGGER.info("[{}] Create integration ready: modId={}, cogwheel={} ({})",
                "croety", MOD_ID, id("cogwheel"), cogwheel.getDescriptionId());
    }
}
