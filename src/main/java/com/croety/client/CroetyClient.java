package com.croety.client;

import com.croety.content.orb.OrbContent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "croety", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CroetyClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OrbContent.SOUL_ORB.get(), SoulOrbRenderer::new);
    }
}
