package com.croety.integration;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * Compile-time and runtime proof that the Create 6.0.8 API is on the classpath.
 * <p>
 * Written against Create's own Maven artifact ({@code create-1.20.1:6.0.8-291:slim}),
 * deobfuscated by ForgeGradle through {@code fg.deobf(...)} in build.gradle.
 */
public final class CreateIntegration
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Create's mod id ({@code Create.ID}). */
    public static final String MOD_ID = Create.ID;

    private CreateIntegration()
    {
    }

    /** Builds a ResourceLocation in Create's namespace. */
    public static ResourceLocation id(String path)
    {
        return Create.asResource(path);
    }

    /**
     * Only call this once Create has finished registering its content, e.g. from
     * {@code FMLCommonSetupEvent#enqueueWork}; the Registrate entries are not
     * populated before that.
     */
    public static void logEnvironment()
    {
        Block cogwheel = AllBlocks.COGWHEEL.get();
        LOGGER.info("[{}] Create integration ready: modId={}, cogwheel={} ({})",
                "croety", MOD_ID, id("cogwheel"), cogwheel.getDescriptionId());
    }
}
