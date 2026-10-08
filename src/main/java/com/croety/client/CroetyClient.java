package com.croety.client;

import com.croety.Croety;
import com.croety.content.fluid.SoulFluidContent;
import com.croety.content.orb.OrbContent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Croety.MODID, value = Dist.CLIENT)
public final class CroetyClient {
    private static final ResourceLocation SOUL_FLUID_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Croety.MODID, "block/fluid_soul_still");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OrbContent.SOUL_ORB.get(), SoulOrbRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return SOUL_FLUID_TEXTURE; }
            @Override public ResourceLocation getFlowingTexture() { return SOUL_FLUID_TEXTURE; }
        }, SoulFluidContent.TYPE.get());
    }
}
