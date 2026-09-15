package com.croety.content.motor;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "croety", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SoulMotorClient {
    private static final PartialModel SHAFT = PartialModel.of(new ResourceLocation("croety", "block/soul_motor/shaft_half"));

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
