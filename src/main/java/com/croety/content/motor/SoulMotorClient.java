package com.croety.content.motor;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

@EventBusSubscriber(modid = "croety", value = Dist.CLIENT)
public class SoulMotorClient {
    private static final PartialModel SHAFT = PartialModel.of(ResourceLocation.fromNamespaceAndPath("croety", "block/soul_motor/shaft_half"));

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MotorContent.MOTOR_BE.get(), SoulMotorRenderer::new);
    }

    @SubscribeEvent
    public static void registerVisual(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(MotorContent.MOTOR_BE.get())
                .factory(OrientedRotatingVisual.of(SHAFT)).apply());
    }

    private static class SoulMotorRenderer extends KineticBlockEntityRenderer<SoulMotorBlockEntity> {
        SoulMotorRenderer(BlockEntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        protected RenderType getRenderType(SoulMotorBlockEntity be, BlockState state) {
            return RenderType.translucent();
        }

        @Override
        protected SuperByteBuffer getRotatedModel(SoulMotorBlockEntity be, BlockState state) {
            return CachedBuffers.partialFacing(SHAFT, state);
        }
    }
}
