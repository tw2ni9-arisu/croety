package com.croety.integration;

import com.Polarice3.Goety.Goety;
import com.Polarice3.Goety.common.items.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

/**
 * Compile-time and runtime proof that the Goety 2.5.57.3 API is on the classpath.
 * <p>
 * Goety is published to no Maven repository, so the release jar is staged in
 * {@code ./libs/maven} and deobfuscated by ForgeGradle through
 * {@code fg.deobf("com.polarice3:goety:2.5.57.3")}.
 */
public final class GoetyIntegration
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Goety's mod id ({@code Goety.MOD_ID}). */
    public static final String MOD_ID = Goety.MOD_ID;

    private GoetyIntegration()
    {
    }

    /** Builds a ResourceLocation in Goety's namespace. */
    public static ResourceLocation id(String path)
    {
        return Goety.location(path);
    }

    /**
     * Only call this once Goety has finished registering its content.
     */
    public static void logEnvironment()
    {
        Item totem = ModItems.TOTEM_OF_SOULS.get();
        LOGGER.info("[{}] Goety integration ready: modId={}, totemOfSouls={} ({})",
                "croety", MOD_ID, id("totem_of_souls"), totem.getDescriptionId());
    }
}
